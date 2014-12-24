package at.yawk.hdr.format;

/**
 * @author yawkat
 */
public class HprofThreadStart extends HprofItem<HprofThreadStart> {
    public static final byte ID = 0x0a;

    public int serial;
    public long objectId;
    public int stackTraceSerial;
    public long threadNameId;
    public long threadGroupNameId;
    public long threadGroupParentNameId;
}
