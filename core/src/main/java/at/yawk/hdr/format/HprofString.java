package at.yawk.hdr.format;

/**
 * @author yawkat
 */
public class HprofString extends HprofItem<HprofString> {
    public static final byte ID = 0x01;

    public long id;
    public String value;
}
