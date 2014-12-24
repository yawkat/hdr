package at.yawk.hdr;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.SoftReference;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import org.cliffc.high_scale_lib.NonBlockingHashMapLong;

/**
 * StreamPool implementation that attempts to keep drive reads low by caching parts of the file.
 *
 * @author yawkat
 */
@ThreadSafe
public class MemoryCachedFilePool implements StreamPool {
    private static final int BLOCK_SIZE = 1 << 25; // 32 MiB
    private static final int MAXIMUM_CACHE_BLOCKS = 16; // ~512 MiB

    /**
     * Length of the file in bytes
     */
    private final long length;
    /**
     * Amount of blocks in the file
     */
    private final int blockCount;
    /**
     * Size of the last block (<= BLOCK_SIZE)
     */
    private final int lastBlockSize;
    /**
     * SoftReferences to the cached blocks. Null entries are not loaded, empty references should be discarded.
     */
    private final SoftReference<byte[]>[] blocks;
    /**
     * MRU "queue" on which blocks are loaded and which ones should be unloaded next.
     */
    private final MRUSelector mruSelector;

    /**
     * RAF singleton, mutex is this pool.
     */
    private final RandomAccessFile raf;

    public MemoryCachedFilePool(File file) throws FileNotFoundException {
        this.mruSelector = new MRUSelector(MAXIMUM_CACHE_BLOCKS);

        this.length = file.length();
        long blockCountLong = (length - 1) / BLOCK_SIZE + 1;
        if (blockCountLong > Integer.MAX_VALUE) {
            throw new ArrayIndexOutOfBoundsException(
                    "File too long: is " + length + ", " + (long) BLOCK_SIZE * Integer.MAX_VALUE + " is supported"
            );
        }
        this.blockCount = (int) blockCountLong;
        this.lastBlockSize = (int) (length - BLOCK_SIZE * (blockCount - 1));
        //noinspection unchecked
        this.blocks = new SoftReference[blockCount];

        this.raf = new RandomAccessFile(file, "r");
    }

    /**
     * Request a block, returning null if its not loaded
     */
    private byte[] getOrNull(int blockId) {
        SoftReference<byte[]> block = blocks[blockId];
        return block == null ? null : block.get();
    }

    /**
     * Get a block by id, loading it if necessary
     */
    private byte[] getBlock(long l) throws IOException {
        int i = (int) l;
        byte[] current = getOrNull(i);
        if (current == null) {
            current = loadBlock(i);
        }
        return current;
    }

    private synchronized byte[] loadBlock(int i) throws IOException {
        // we're synchronized now, recheck if its loaded
        byte[] block = getOrNull(i);
        if (block != null) {
            return block;
        }

        // seek to the block
        raf.seek((long) i * BLOCK_SIZE);

        // how large this block is
        int expectRead = BLOCK_SIZE;
        if (i >= blockCount - 1) {
            expectRead = lastBlockSize;
        }

        // remove old unloadable blocks
        int rem = (int) mruSelector.insert(i);
        if (rem != -1) {
            byte[] removed = getOrNull(rem);
            // try to reuse the byte array
            if (removed != null && removed.length == expectRead) { block = removed; }
            // clear that entry
            blocks[rem] = null;
        }

        if (block == null) {
            // cant reuse an old block, make a new one
            block = new byte[expectRead];
        }
        // read the block
        while (expectRead > 0) {
            expectRead -= raf.read(block);
        }

        // create the SoftReference, store it and submit it to the memory watcher
        SoftReference<byte[]> ref = new SoftReference<>(block);
        blocks[i] = ref;
        MemoryUtil.registerSoftResource(block, block.length);

        return block;
    }

    @Override
    public PooledInputStream open(long pos) throws InterruptedException, IOException {
        Stream stream = new Stream();
        stream.seek(pos);
        return stream;
    }

    @NotThreadSafe
    private class Stream implements PooledInputStream {
        private static final int POOLED_ARRAY_SIZE_LIMIT = 8;

        /**
         * Pool of short byte arrays used for read(int)
         * #0 has length 1, #1 has len 2 etc, up to POOLED_ARRAY_SIZE_LIMIT elements
         */
        private final byte[][] shortArrayPool;
        /**
         * Position in the file
         */
        private long pos = 0;

        {
            shortArrayPool = new byte[POOLED_ARRAY_SIZE_LIMIT][];
            for (int i = 0; i < POOLED_ARRAY_SIZE_LIMIT; i++) {
                shortArrayPool[i] = new byte[i + 1];
            }
        }

        @Override
        public int read() throws IOException {
            byte b = getBlock(pos / BLOCK_SIZE)[(int) (pos % BLOCK_SIZE)];
            pos++;
            return b;
        }

        @Override
        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int done = 0;
            while (len > done) {
                long blockIndex = pos / BLOCK_SIZE;
                byte[] block = getBlock(blockIndex);
                int blockPos = (int) (pos % BLOCK_SIZE);
                int copy = Math.min(len - done, block.length - blockPos);
                System.arraycopy(block, blockPos, b, off + done, copy);
                done += copy;
                pos += copy;
                if (blockIndex >= blockCount - 1) {
                    return done;
                }
            }
            return done;
        }

        @Override
        public byte[] read(int count) throws IOException {
            byte[] buf;
            if (count > POOLED_ARRAY_SIZE_LIMIT) {
                // too large for short array pool
                buf = new byte[count];
            } else {
                buf = shortArrayPool[count - 1];
            }
            read(buf);
            return buf;
        }

        @Override
        public void seekBy(long count) throws IOException {
            pos += count;
        }

        @Override
        public void seek(long pos) throws IOException {
            this.pos = pos;
        }

        @Override
        public void close() {
            // do nothing as Stream objects aren't reused
        }

        @Override
        public long getPosition() {
            return pos;
        }

        @Override
        public boolean hitEnd() throws IOException {
            return pos >= length;
        }
    }

    /**
     * Fast most-recently-used selector with long values. Basically an item queue with a fixed maximum entry count.
     */
    @ThreadSafe
    private static class MRUSelector {
        /**
         * Continuously increasing "time" value (higher = younger)
         */
        private final AtomicLong modificationTime = new AtomicLong();

        private final Slot[] slots;
        private final NonBlockingHashMapLong<Slot> slotsByValue = new NonBlockingHashMapLong<>();

        public MRUSelector(int elementCount) {
            this.slots = new Slot[elementCount];
            for (int i = 0; i < slots.length; i++) {
                this.slots[i] = new Slot();
            }
        }

        /**
         * Find a slot for the given value.
         *
         * Pushes or moves the given value to the front of the queue and returns a removed back element if the queue
         * overflowed.
         *
         * @return The slot that was replaced or -1 if there was a free slot or value was already present
         */
        public long insert(long value) {
            while (true) {
                Slot slot = slotsByValue.get(value);
                if (slot == null) {
                    return claimSlot(value); // use a free slot or replace an old one
                } else {
                    slot.modTime = modificationTime.getAndIncrement();
                    if (slot.value == value) {
                        return -1;
                    }
                }
            }
        }

        private synchronized long claimSlot(long value) {
            // we're synchronized, recheck if we have a slot
            Slot best = slotsByValue.get(value);
            if (best != null) {
                return -1;
            }

            // select the slot with the lowest modTime (least recently used)
            for (Slot slot : slots) {
                if (best == null || best.modTime > slot.modTime) {
                    best = slot;
                }
            }

            // must be the case if we have > 1 slot
            assert best != null;

            // replace slot value with our value
            long prevValue = best.value;
            slotsByValue.remove(prevValue);
            best.value = value;
            // make young
            best.modTime = modificationTime.getAndIncrement();
            slotsByValue.put(best.value, best);
            return prevValue;
        }

        @Override
        public String toString() {
            return Arrays.toString(slots);
        }

        private class Slot {
            volatile long value = -1;
            volatile long modTime = -1;

            @Override
            public String toString() {
                return "{ v=" + value + " t=" + modTime + " }";
            }
        }
    }
}
