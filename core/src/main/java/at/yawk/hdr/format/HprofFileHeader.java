package at.yawk.hdr.format;

/**
 * @author yawkat
 */
public class HprofFileHeader extends HprofItem<HprofFileHeader> {
    public String version;
    public int identifierSize;
    public long time;
}
