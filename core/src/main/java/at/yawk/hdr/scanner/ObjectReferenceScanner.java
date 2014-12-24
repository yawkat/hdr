package at.yawk.hdr.scanner;

import at.yawk.hdr.format.*;
import at.yawk.hdr.index.HeapDumpItemVisitor;
import gnu.trove.impl.sync.TSynchronizedIntIntMap;
import gnu.trove.impl.sync.TSynchronizedLongObjectMap;
import gnu.trove.impl.sync.TSynchronizedLongSet;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import lombok.Getter;

/**
 * Scan a heap dump for reference holders to a given set of objects.
 *
 * @author yawkat
 */
public class ObjectReferenceScanner {
    private TLongSet objects;
    /**
     * Other objects that reference the objects in #objects:
     * class ID -> [object IDs]
     */
    @Getter private final TLongObjectMap<TLongSet> ownerObjects =
            new TSynchronizedLongObjectMap<>(new TLongObjectHashMap<>());
    /**
     * Stacks that reference the objects in #objects:
     * thread serial -> ref count
     */
    @Getter private final TIntIntMap ownerStacks = new TSynchronizedIntIntMap(new TIntIntHashMap());

    public ObjectReferenceScanner(TLongSet objects) {
        this.objects = objects;
    }

    private TLongSet getObjectSet(long classId) {
        TLongSet set = ownerObjects.get(classId);
        if (set == null) {
            set = new TSynchronizedLongSet(new TLongHashSet());
            TLongSet previous = ownerObjects.putIfAbsent(classId, set);
            if (previous != null) { set = previous; }
        }
        return set;
    }

    /**
     * Create a visitor that runs the given scanners on all objects except the ones in the given id set.
     */
    public static HeapDumpItemVisitor toVisitor(TLongSet exclude, ObjectReferenceScanner... scanners) {
        return new HeapDumpItemVisitor(true) {
            @Override
            public void visitRootJavaFrame(HprofHeapDumpRootJavaFrame item) {
                for (ObjectReferenceScanner scanner : scanners) {
                    if (scanner.objects.contains(item.id)) {
                        scanner.ownerStacks.adjustOrPutValue(item.threadSerial, 1, 1);
                    }
                }
            }

            @Override
            public boolean visitInstanceHeader(HprofHeapDumpInstanceHeader item) {
                return exclude == null || !exclude.contains(item.id);
            }

            @Override
            public void visitInstanceField(HprofHeapDumpInstanceHeader instance, HprofHeapDumpClassField declaring,
                                           long value) {
                if (declaring.type == BaseType.OBJECT) {
                    for (ObjectReferenceScanner scanner : scanners) {
                        if (scanner.objects.contains(value)) {
                            scanner.getObjectSet(instance.classObjectId).add(instance.id);
                        }
                    }
                }
            }

            @Override
            public boolean visitObjectArrayDumpHeader(HprofHeapDumpObjectArrayDumpHeader item) {
                return exclude == null || !exclude.contains(item.id);
            }

            @Override
            public void visitObjectArrayEntry(HprofHeapDumpObjectArrayDumpHeader header, long ref) {
                for (ObjectReferenceScanner scanner : scanners) {
                    if (scanner.objects.contains(ref)) {
                        scanner.getObjectSet(header.classObjectId).add(header.id);
                    }
                }
            }
        };
    }

    /**
     * Close this scanner, making it unavailable for future runs but cleaning up resources only required for running
     * it.
     */
    public void close() {
        objects = null; // clear for GC
    }
}
