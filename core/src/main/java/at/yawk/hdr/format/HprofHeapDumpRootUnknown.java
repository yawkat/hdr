package at.yawk.hdr.format;

/**
 * @author yawkat
 */
@HeapDumpEntry(HprofHeapDumpRootUnknown.ID)
public class HprofHeapDumpRootUnknown extends HprofItem<HprofHeapDumpRootUnknown> {
    public static final byte ID = (byte) 0xff;

    public long id;
}
