package at.yawk.hdr.format;

/**
 * @author yawkat
 */
@HeapDumpEntry(HprofHeapDumpRootNativeStack.ID)
public class HprofHeapDumpRootNativeStack extends HprofItem<HprofHeapDumpRootNativeStack> {
    public static final byte ID = (byte) 0x04;

    public long id;
    public int threadSerial;
}
