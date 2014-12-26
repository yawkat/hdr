package at.yawk.hdr.index;

import java.util.concurrent.Future;

/**
 * @author yawkat
 */
class HeapDumpSegment {
    public final long position;
    public final long length;

    HeapDumpItemVisitor currentEnqueuedVisitor = null;
    MultiConsumerFuture<?> consumerFuture = null;
    ProgressCounter counter = null;

    public HeapDumpSegment(long position, long length) {
        this.position = position;
        this.length = length;
    }

    @Override
    public String toString() {
        return "Segment[" + position + " -> " + (position + length) + " (+" + length + ")]";
    }
}
