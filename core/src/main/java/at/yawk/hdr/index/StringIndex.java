package at.yawk.hdr.index;

import at.yawk.hdr.format.HprofString;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TLongIntHashMap;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import org.jetbrains.annotations.NotNull;

/**
 * Fast, low-memory id -> string index
 *
 * @author yawkat
 */
public class StringIndex extends Index<String> {
    private static final int INITIAL_SIZE = 128;
    private static final Charset CHARSET = StandardCharsets.UTF_8;

    /*
     * The data array contains all UTF-8-encoded strings of this index. Each is prefixed with a variable-length
     * header which contains the encoded string length. This string length is encoded big-endian with 7 bits per entry,
     * the remaining first bit being 0 for all bytes of the string length but the first. After that follows the actual
     * encoded string. Example:
     *
     * Let the string be a 200-byte string starting with abc... and ending with ...xyz
     * 0x00: ((200 >>> 7 * 1) & 0x7f) | 0x80 = 0b10000001 [upper 7 bits of string length | 0x80 for first byte]
     * 0x01:  (200 >>> 7 * 0) & 0x7f         = 0b01001000 [lower 7 bits of string length]
     * 0x02: a                               = 0b01100001 [string[  0]]
     * 0x03: b                               = 0b01100010 [string[  1]]
     * 0x04: c                               = 0b01100011 [string[  2]]
     * ...
     * 0xc7: x                               = 0b01110100 [string[197]]
     * 0xc8: y                               = 0b01110101 [string[198]]
     * 0xc9: z                               = 0b01110110 [string[199]]
     *
     * The "position" field marks the end of the last string and where the next length header may begin.
     *
     * The "positions" map maps the string ID to the position of the byte array start of that string (0x02 in the
     * example).
     */

    private byte[] data = new byte[INITIAL_SIZE];
    private int position = 0;
    private TLongIntMap positions = new TLongIntHashMap();

    private void ensureCapacity(int capacity) {
        int newSize = data.length;
        if (capacity > newSize) {
            do {
                newSize <<= 1;
            } while (capacity > newSize);
            data = Arrays.copyOf(data, newSize);
        }
    }

    void add(HprofString string) {
        byte[] bytes = string.value.getBytes(CHARSET);

        writeVarUInt(bytes.length);

        ensureCapacity(position + bytes.length);
        System.arraycopy(bytes, 0, data, position, bytes.length);
        positions.put(string.id, position);
        position += bytes.length;
    }

    void shrinkToContent() {
        data = Arrays.copyOf(data, position);
    }

    public String get(long id) {
        int pos = positions.get(id);
        if (pos == 0) {
            return null;
        }
        return get0(pos);
    }

    private String get0(int pos) {
        int len = readVarUInt(pos);
        return new String(data, pos, len, CHARSET).intern();
    }

    private void writeVarUInt(int uint) {
        boolean first = true;
        do {
            ensureCapacity(position + 1);
            data[position] |= (byte) (uint & 0x7f);
            if (first) {
                data[position] |= 0x80;
                first = false;
            }
            position++;
            uint >>>= 7;
        } while (uint != 0);
    }

    private int readVarUInt(int pos) {
        int val = 0;
        int shift = 0;
        byte b;
        do {
            pos--;
            b = data[pos];
            val |= (b & 0x7f) << shift;
            shift += 7;
        } while ((b & 0x80) == 0);
        return val;
    }

    @Override
    public int size() {
        return positions.size();
    }

    @NotNull
    @Override
    public Iterator<String> iterator() {
        return new Iterator<String>() {
            TIntIterator positionIterator = positions.valueCollection().iterator();

            @Override
            public boolean hasNext() {
                return positionIterator.hasNext();
            }

            @Override
            public String next() {
                return get(positionIterator.next());
            }
        };
    }
}

