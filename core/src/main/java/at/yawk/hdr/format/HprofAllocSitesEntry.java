package at.yawk.hdr.format;

/**
 * @author yawkat
 */
public class HprofAllocSitesEntry extends HprofItem<HprofAllocSitesEntry> {
    public int arrayType; // 0 = not an array
    public int classSerial;
    public int stackTraceSerial;
    public int liveBytes;
    public int liveInstances;
    public int allocatedBytes;
    public int allocatedInstances;
}
