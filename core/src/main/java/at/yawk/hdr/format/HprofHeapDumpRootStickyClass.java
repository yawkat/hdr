package at.yawk.hdr.format;

/**
 * @author yawkat
 */
@HeapDumpEntry(HprofHeapDumpRootStickyClass.ID)
public class HprofHeapDumpRootStickyClass extends HprofItem<HprofHeapDumpRootStickyClass> {
    public static final byte ID = (byte) 0x05;

    public long objectId;
}
