package at.yawk.hdr.format;

/**
 * @author yawkat
 */
@HeapDumpEntry(HprofHeapDumpRootMonitorUsed.ID)
public class HprofHeapDumpRootMonitorUsed extends HprofItem<HprofHeapDumpRootMonitorUsed> {
    public static final byte ID = (byte) 0x07;

    public long objectId;
}
