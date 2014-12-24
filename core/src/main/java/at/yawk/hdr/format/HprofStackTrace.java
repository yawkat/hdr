package at.yawk.hdr.format;

/**
 * @author yawkat
 */
public class HprofStackTrace extends HprofItem<HprofStackTrace> {
    public static final byte ID = 0x05;

    public int serial;
    public int threadSerial;
    public long[] frameIds;
}
