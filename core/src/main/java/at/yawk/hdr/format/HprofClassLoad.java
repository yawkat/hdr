package at.yawk.hdr.format;

/**
 * @author yawkat
 */
public class HprofClassLoad extends HprofItem<HprofClassLoad> {
    public static final byte ID = 0x02;

    public int serial;
    public long objectId;
    public int stackSerial;
    public long nameId;
}
