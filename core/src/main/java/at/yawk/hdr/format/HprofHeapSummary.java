package at.yawk.hdr.format;

/**
 * @author yawkat
 */
public class HprofHeapSummary extends HprofItem<HprofHeapSummary> {
    public static final byte ID = 0x07;

    public int liveBytes;
    public int liveInstances;
    public int allocatedBytes;
    public int allocatedInstances;
}
