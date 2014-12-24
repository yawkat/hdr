package at.yawk.hdr.format;

/**
 * @author yawkat
 */
@HeapDumpEntry(HprofHeapDumpRootJniLocal.ID)
public class HprofHeapDumpRootJniLocal extends HprofItem<HprofHeapDumpRootJniLocal> {
    public static final byte ID = (byte) 0x02;

    public long id;
    public int threadSerial;
    public int stackTraceFrame;
}
