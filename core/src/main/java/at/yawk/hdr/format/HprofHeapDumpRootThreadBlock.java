package at.yawk.hdr.format;

/**
 * @author yawkat
 */
@HeapDumpEntry(HprofHeapDumpRootThreadBlock.ID)
public class HprofHeapDumpRootThreadBlock extends HprofItem<HprofHeapDumpRootThreadBlock> {
    public static final byte ID = (byte) 0x06;

    public long objectId;
    public int threadSerial;
}
