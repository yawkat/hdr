package at.yawk.hdr.format;

/**
 * @author yawkat
 */
@HeapDumpEntry(HprofHeapDumpClassHeader.ID)
public class HprofHeapDumpClassHeader extends HprofItem<HprofHeapDumpClassHeader> {
    public static final byte ID = (byte) 0x20;

    public long id;
    public int stackTraceSerial;
    public long superClassObjectId;
    public long classLoaderObjectId;
    public long signersObjectId;
    public long protectionDomainObjectId;
    public long reserved1;
    public long reserved2;
    public int instanceSize;
}
