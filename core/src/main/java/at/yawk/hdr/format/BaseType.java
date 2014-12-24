package at.yawk.hdr.format;

/**
 * @author yawkat
 */
public class BaseType {
    private BaseType() {}

    public static final byte OBJECT = 2;
    public static final byte BOOLEAN = 4;
    public static final byte CHAR = 5;
    public static final byte FLOAT = 6;
    public static final byte DOUBLE = 7;
    public static final byte BYTE = 8;
    public static final byte SHORT = 9;
    public static final byte INT = 10;
    public static final byte LONG = 11;

    /**
     * Highest type ID
     */
    public static final byte MAX = LONG;

    /**
     * Length (in bytes) of primitive types (type id -> length)
     */
    public static byte[] LENGTH = new byte[MAX + 1];

    static {
        LENGTH[BOOLEAN] = 1;
        LENGTH[BYTE] = 1;
        LENGTH[SHORT] = 2;
        LENGTH[CHAR] = 2;
        LENGTH[INT] = 4;
        LENGTH[FLOAT] = 4;
        LENGTH[LONG] = 8;
        LENGTH[DOUBLE] = 8;
    }
}
