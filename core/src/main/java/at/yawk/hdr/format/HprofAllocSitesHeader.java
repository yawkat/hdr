package at.yawk.hdr.format;

/**
 * @author yawkat
 */
public class HprofAllocSitesHeader extends HprofItem<HprofAllocSitesHeader> {
    public static final byte ID = 0x06;

    public short flags;
    public float cutoffRatio;
    public int liveBytes;
    public int liveInstances;
    public long allocatedBytes;
    public int entryCount;
}
