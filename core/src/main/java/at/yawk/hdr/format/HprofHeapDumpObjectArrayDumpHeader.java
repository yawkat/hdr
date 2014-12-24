package at.yawk.hdr.format;

/**
 * @author yawkat
 */
@HeapDumpEntry(HprofHeapDumpObjectArrayDumpHeader.ID)
public class HprofHeapDumpObjectArrayDumpHeader extends HprofItem<HprofHeapDumpObjectArrayDumpHeader> {
    public static final byte ID = (byte) 0x22;

    public long id;
    public int stackTraceSerial;
    public int length;
    public long classObjectId;
}
