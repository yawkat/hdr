package at.yawk.hdr.format;

/**
 * @author yawkat
 */
@HeapDumpEntry(HprofHeapDumpRootJniGlobal.ID)
public class HprofHeapDumpRootJniGlobal extends HprofItem<HprofHeapDumpRootJniGlobal> {
    public static final byte ID = (byte) 0x01;

    public long id;
    public long refId;
}
