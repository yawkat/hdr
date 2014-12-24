package at.yawk.hdr.gui;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
public class LongBitSet {
    private long[] data;

    public LongBitSet(long initialCapacity) {
        data = new long[(int) (((initialCapacity - 1) / 64) + 1)];
    }

    public LongBitSet() {
        this(128);
    }

    private void ensureCapacity(long key) {
        if (key / 64 > data.length) {
            int newSize = data.length;
            while (key / 64 > newSize) {
                newSize <<= 1;
            }
            log.info("Resizing LBS to {}", newSize);
            data = Arrays.copyOf(data, newSize);
            /*
            long[] oldData = data;
            data = new long[newSize];
            for (int i = 0; i < oldData.length; i++) {
                data[i] |= oldData[i];
            }
            */
        }
    }

    public synchronized void add(long k) {
        ensureCapacity(k);
        data[(int) (k / 64)] |= 1 << (k % 64);
    }

    public boolean get(long k) {
        int i = (int) (k / 64);
        return i < data.length && (data[i] >>> (k % 64)) != 0;
    }
}
