package at.yawk.hdr.format;

/**
 * @author yawkat
 */
public class HprofHeapDumpClassConstantPoolEntry extends HprofItem<HprofHeapDumpClassConstantPoolEntry> {
    public short index;
    public byte type;
    public long value;
}
