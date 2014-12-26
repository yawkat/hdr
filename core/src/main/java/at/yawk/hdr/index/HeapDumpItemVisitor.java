package at.yawk.hdr.index;

import at.yawk.hdr.format.*;
import java.lang.reflect.Method;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;

/**
 * Note that any Hprof items passed to these methods must be cloned if they are to be stored as they are reused to
 * avoid
 * unnecessary allocations.
 *
 * @author yawkat
 */
@Slf4j
public abstract class HeapDumpItemVisitor {
    final boolean parallel;

    private static final Set<Class<?>> itemTypes =
            new Reflections(HeapDumpEntry.class.getPackage().getName()).getTypesAnnotatedWith(HeapDumpEntry.class);

    /**
     * What items may be skipped (computed from non-overridden methods)
     */
    private boolean[] skip = new boolean[0x100];

    {
        try {
            for (Method method : HeapDumpItemVisitor.class.getMethods()) {
                if (method.getParameterCount() == 1) {
                    Class<?> clazz = method.getParameterTypes()[0];
                    if (itemTypes.contains(clazz)) {
                        boolean skip = getClass().getMethod(method.getName(), clazz)
                                               .getDeclaringClass() == HeapDumpItemVisitor.class;
                        if (skip) {
                            log.trace("{} of {} will be skipped", method, this);
                            this.skip[clazz.getAnnotation(HeapDumpEntry.class).value() & 0xff] = true;
                        }
                    }
                }
            }
        } catch (ReflectiveOperationException e) {
            log.error("Failed to scan skip methods in " + this, e);
        }
    }

    /**
     * @param parallel whether parallel calls are supported
     */
    public HeapDumpItemVisitor(boolean parallel) {
        this.parallel = parallel;
    }

    /**
     * @return whether this item should be skipped.
     */
    public boolean maySkip(byte type) {
        return skip[type & 0xff];
    }

    public void visitClassHeader(HprofHeapDumpClassHeader item) {}

    /**
     * @return whether to read fields
     */
    public boolean visitInstanceHeader(HprofHeapDumpInstanceHeader item) {
        return false;
    }

    public void visitInstanceField(HprofHeapDumpInstanceHeader instance, HprofHeapDumpClassField declaring, long
            value) {
    }

    /**
     * @return whether to visit the array entries.
     */
    public boolean visitObjectArrayDumpHeader(HprofHeapDumpObjectArrayDumpHeader item) {
        return false;
    }

    /**
     * @return whether to visit the array entries.
     */
    public boolean visitPrimitiveArrayDumpHeader(HprofHeapDumpPrimitiveArrayDumpHeader item) {
        return false;
    }

    public void visitRootJavaFrame(HprofHeapDumpRootJavaFrame item) {}

    public void visitRootJniGlobal(HprofHeapDumpRootJniGlobal item) {}

    public void visitRootJniLocal(HprofHeapDumpRootJniLocal item) {}

    public void visitRootMonitorUsed(HprofHeapDumpRootMonitorUsed item) {}

    public void visitRootNativeStack(HprofHeapDumpRootNativeStack item) {}

    public void visitRootStickyClass(HprofHeapDumpRootStickyClass item) {}

    public void visitRootThreadBlock(HprofHeapDumpRootThreadBlock item) {}

    public void visitRootThreadObject(HprofHeapDumpRootThreadObject item) {}

    public void visitRootUnknown(HprofHeapDumpRootUnknown item) {}

    public void visitConstantPoolEntry(HprofHeapDumpClassHeader classHeader, HprofHeapDumpClassConstantPoolEntry
            constantPoolEntry) {
    }

    public void visitStaticField(HprofHeapDumpClassHeader classHeader, HprofHeapDumpClassStaticField staticField) {}

    public void visitField(HprofHeapDumpClassHeader classHeader, HprofHeapDumpClassField field) {}

    public void visitObjectArrayEntry(HprofHeapDumpObjectArrayDumpHeader header, long ref) {}

    public void visitPrimitiveArrayEntry(long value) {}

    public void visitInstanceEnd(HprofHeapDumpInstanceHeader header) {}

    public void visitObjectArrayEnd(HprofHeapDumpObjectArrayDumpHeader header) {}

    public void visitPrimitiveArrayEnd(HprofHeapDumpPrimitiveArrayDumpHeader header) {}
}
