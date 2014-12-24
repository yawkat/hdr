package at.yawk.hdr.index;

import at.yawk.hdr.format.HprofHeapDumpRootThreadObject;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Data on a call stack / thread stack
 *
 * @author yawkat
 */
public class StackData extends ReferenceOwnerData {
    HprofHeapDumpRootThreadObject threadObject;
    AtomicLong objectCount = new AtomicLong();

    public HprofHeapDumpRootThreadObject getThreadObject() {
        return threadObject;
    }

    public long getObjectCount() {
        return objectCount.get();
    }
}
