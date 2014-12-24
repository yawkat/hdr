package at.yawk.hdr.index;

/**
 * @author yawkat
 */
class PositionLengthTuple {
    public final long position;
    public final long length;

    public PositionLengthTuple(long position, long length) {
        this.position = position;
        this.length = length;
    }

    @Override
    public String toString() {
        return "Segment[" + position + " -> " + (position + length) + " (+" + length + ")]";
    }
}
