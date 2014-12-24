package at.yawk.hdr.format;

/**
 * @author yawkat
 */
public class HprofControlSettings extends HprofItem<HprofControlSettings> {
    public static final byte ID = 0x0e;

    public int flags;
    public short stackTraceDepth;
}
