package at.yawk.hdr.index;

import at.yawk.hdr.format.*;
import java.util.Arrays;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Delegates visitor calls to its children. NOT thread safe, so use one per thread!
 *
 * @author yawkat
 */
@NotThreadSafe
class MultiItemVisitor extends HeapDumpItemVisitor {
    private HeapDumpItemVisitor[] visitors;

    private boolean[] skipCache = new boolean[0x100];

    public MultiItemVisitor(HeapDumpItemVisitor... visitors) {
        super(false);
        this.visitors = visitors;
        recomputeCaches();
    }

    private void recomputeCaches() {
        for (int i = 0; i < skipCache.length; i++) {
            skipCache[i] = true;
            for (HeapDumpItemVisitor visitor : visitors) {
                skipCache[i] &= visitor.maySkip((byte) i);
                if (!skipCache[i]) { break; }
            }
        }

        enter = new boolean[visitors.length];
    }

    synchronized void add(HeapDumpItemVisitor visitor) {
        visitors = Arrays.copyOf(visitors, visitors.length + 1);
        visitors[visitors.length - 1] = visitor;
        recomputeCaches();
    }

    @Override
    public boolean maySkip(byte type) {
        return skipCache[type];
    }

    @Override
    public void visitClassHeader(HprofHeapDumpClassHeader item) {
        for (HeapDumpItemVisitor visitor : visitors) {
            visitor.visitClassHeader(item);
        }
    }

    private boolean[] enter;

    @Override
    public boolean visitInstanceHeader(HprofHeapDumpInstanceHeader item) {
        boolean enter = false;
        for (int i = 0; i < visitors.length; i++) {
            boolean e = visitors[i].visitInstanceHeader(item);
            enter |= e;
            this.enter[i] = e;
        }
        return enter;
    }

    @Override
    public void visitInstanceField(HprofHeapDumpInstanceHeader instance, HprofHeapDumpClassField declaring,
                                   long value) {
        for (int i = 0; i < visitors.length; i++) {
            if (this.enter[i]) {
                visitors[i].visitInstanceField(instance, declaring, value);
            }
        }
    }

    @Override
    public boolean visitObjectArrayDumpHeader(HprofHeapDumpObjectArrayDumpHeader item) {
        boolean enter = false;
        for (int i = 0; i < visitors.length; i++) {
            boolean e = visitors[i].visitObjectArrayDumpHeader(item);
            enter |= e;
            this.enter[i] = e;
        }
        return enter;
    }

    @Override
    public boolean visitPrimitiveArrayDumpHeader(HprofHeapDumpPrimitiveArrayDumpHeader item) {
        boolean enter = false;
        for (int i = 0; i < visitors.length; i++) {
            boolean e = visitors[i].visitPrimitiveArrayDumpHeader(item);
            enter |= e;
            this.enter[i] = e;
        }
        return enter;
    }

    @Override
    public void visitRootJavaFrame(HprofHeapDumpRootJavaFrame item) {
        for (HeapDumpItemVisitor visitor : visitors) {
            visitor.visitRootJavaFrame(item);
        }
    }

    @Override
    public void visitRootJniGlobal(HprofHeapDumpRootJniGlobal item) {
        for (HeapDumpItemVisitor visitor : visitors) {
            visitor.visitRootJniGlobal(item);
        }
    }

    @Override
    public void visitRootJniLocal(HprofHeapDumpRootJniLocal item) {
        for (HeapDumpItemVisitor visitor : visitors) {
            visitor.visitRootJniLocal(item);
        }
    }

    @Override
    public void visitRootMonitorUsed(HprofHeapDumpRootMonitorUsed item) {
        for (HeapDumpItemVisitor visitor : visitors) {
            visitor.visitRootMonitorUsed(item);
        }
    }

    @Override
    public void visitRootNativeStack(HprofHeapDumpRootNativeStack item) {
        for (HeapDumpItemVisitor visitor : visitors) {
            visitor.visitRootNativeStack(item);
        }
    }

    @Override
    public void visitRootStickyClass(HprofHeapDumpRootStickyClass item) {
        for (HeapDumpItemVisitor visitor : visitors) {
            visitor.visitRootStickyClass(item);
        }
    }

    @Override
    public void visitRootThreadBlock(HprofHeapDumpRootThreadBlock item) {
        for (HeapDumpItemVisitor visitor : visitors) {
            visitor.visitRootThreadBlock(item);
        }
    }

    @Override
    public void visitRootThreadObject(HprofHeapDumpRootThreadObject item) {
        for (HeapDumpItemVisitor visitor : visitors) {
            visitor.visitRootThreadObject(item);
        }
    }

    @Override
    public void visitRootUnknown(HprofHeapDumpRootUnknown item) {
        for (HeapDumpItemVisitor visitor : visitors) {
            visitor.visitRootUnknown(item);
        }
    }

    @Override
    public void visitConstantPoolEntry(HprofHeapDumpClassHeader classHeader, HprofHeapDumpClassConstantPoolEntry
            constantPoolEntry) {
        for (HeapDumpItemVisitor visitor : visitors) {
            visitor.visitConstantPoolEntry(classHeader, constantPoolEntry);
        }
    }

    @Override
    public void visitStaticField(HprofHeapDumpClassHeader classHeader, HprofHeapDumpClassStaticField staticField) {
        for (HeapDumpItemVisitor visitor : visitors) {
            visitor.visitStaticField(classHeader, staticField);
        }
    }

    @Override
    public void visitField(HprofHeapDumpClassHeader classHeader, HprofHeapDumpClassField field) {
        for (int i = 0; i < visitors.length; i++) {
            if (enter[i]) {
                visitors[i].visitField(classHeader, field);
            }
        }
    }

    @Override
    public void visitObjectArrayEntry(HprofHeapDumpObjectArrayDumpHeader header, long ref) {
        for (int i = 0; i < visitors.length; i++) {
            if (enter[i]) {
                visitors[i].visitObjectArrayEntry(header, ref);
            }
        }
    }

    @Override
    public void visitPrimitiveArrayEntry(long value) {
        for (int i = 0; i < visitors.length; i++) {
            if (enter[i]) {
                visitors[i].visitPrimitiveArrayEntry(value);
            }
        }
    }

    @Override
    public void visitInstanceEnd(HprofHeapDumpInstanceHeader header) {
        for (int i = 0; i < visitors.length; i++) {
            if (enter[i]) {
                visitors[i].visitInstanceEnd(header);
            }
        }
    }

    @Override
    public void visitObjectArrayEnd(HprofHeapDumpObjectArrayDumpHeader header) {
        for (int i = 0; i < visitors.length; i++) {
            if (enter[i]) {
                visitors[i].visitObjectArrayEnd(header);
            }
        }
    }

    @Override
    public void visitPrimitiveArrayEnd(HprofHeapDumpPrimitiveArrayDumpHeader header) {
        for (int i = 0; i < visitors.length; i++) {
            if (enter[i]) {
                visitors[i].visitPrimitiveArrayEnd(header);
            }
        }
    }
}
