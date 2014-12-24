package at.yawk.hdr.format;

/**
 * @author yawkat
 */
@HeapDumpEntry(HprofHeapDumpRootJavaFrame.ID)
public class HprofHeapDumpRootJavaFrame extends HprofItem<HprofHeapDumpRootJavaFrame> {
    public static final byte ID = (byte) 0x03;

    public long id;
    public int threadSerial;
    public int stackTraceFrameIndex;
}
