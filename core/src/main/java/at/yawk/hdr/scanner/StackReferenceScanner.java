package at.yawk.hdr.scanner;

import at.yawk.hdr.format.HprofHeapDumpRootJavaFrame;
import at.yawk.hdr.index.HeapDumpItemVisitor;
import gnu.trove.impl.sync.TSynchronizedLongSet;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import lombok.Getter;

/**
 * Visitor that searches for objects on the given stack
 *
 * @author yawkat
 */
public class StackReferenceScanner extends HeapDumpItemVisitor {
    /**
     * Thread serial
     */
    private final int stack;
    /**
     * object IDs on this stack
     */
    @Getter private final TLongSet objects = new TSynchronizedLongSet(new TLongHashSet());

    public StackReferenceScanner(int stack) {
        super(true);
        this.stack = stack;
    }

    @Override
    public void visitRootJavaFrame(HprofHeapDumpRootJavaFrame item) {
        if (item.threadSerial == stack) {
            objects.add(item.id);
        }
    }
}
