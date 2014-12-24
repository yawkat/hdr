package at.yawk.hdr.format;

/**
 * @author yawkat
 */
@HeapDumpEntry(HprofHeapDumpRootThreadObject.ID)
public class HprofHeapDumpRootThreadObject extends HprofItem<HprofHeapDumpRootThreadObject> {
    public static final byte ID = (byte) 0x08;

    public long objectId;
    public int threadSerial;
    public int stackTraceSerial;
}
