package at.yawk.hdr.format;

/**
 * @author yawkat
 */
public class HprofCpuSamplesEntry extends HprofItem<HprofCpuSamplesEntry> {
    public int sampleCount;
    public int stackTraceSerial;
}
