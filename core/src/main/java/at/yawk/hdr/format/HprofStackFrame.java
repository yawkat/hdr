package at.yawk.hdr.format;

/**
 * @author yawkat
 */
public class HprofStackFrame extends HprofItem<HprofStackFrame> {
    public static final byte ID = 0x04;

    public long id; // -> this
    public long methodNameId; // -> string
    public long methodSignatureId; // -> string
    public long sourceFileNameId; // -> string
    public int classSerial;
    public int lineNumber; // 0 not available, -1 unknown, -2 compiled, -3 native
}
