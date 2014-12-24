package at.yawk.hdr.format;

/**
 * @author yawkat
 */
public class HprofTagHeader extends HprofItem<HprofTagHeader> {
    public byte type;
    public int timeOffset;
    public int length;
}
