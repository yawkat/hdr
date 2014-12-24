package at.yawk.hdr.format;

/**
 * @author yawkat
 */
@HeapDumpEntry(HprofHeapDumpPrimitiveArrayDumpHeader.ID)
public class HprofHeapDumpPrimitiveArrayDumpHeader extends HprofItem<HprofHeapDumpPrimitiveArrayDumpHeader> {
    public static final byte ID = (byte) 0x23;

    public long id;
    public int stackTraceSerial;
    public int length;
    public byte elementType;
}
