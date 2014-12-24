package at.yawk.hdr.format;

/**
 * @author yawkat
 */
public class HprofCpuSamplesHeader extends HprofItem<HprofCpuSamplesHeader> {
    public static final byte ID = 0x0d;

    public int sampleCount;
}
