package at.yawk.hdr.format;

/**
 * @author yawkat
 */
@HeapDumpEntry(HprofHeapDumpInstanceHeader.ID)
public class HprofHeapDumpInstanceHeader extends HprofItem<HprofHeapDumpInstanceHeader> {
    public static final byte ID = (byte) 0x21;

    public long id;
    public int stackTraceSerial;
    public long classObjectId;
    public int bodyLength;
}
