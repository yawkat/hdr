package at.yawk.hdr.scanner;

import at.yawk.hdr.format.HprofHeapDumpInstanceHeader;
import at.yawk.hdr.format.HprofHeapDumpObjectArrayDumpHeader;
import at.yawk.hdr.index.HeapDumpItemVisitor;
import gnu.trove.impl.sync.TSynchronizedLongLongMap;
import gnu.trove.map.TLongLongMap;
import gnu.trove.map.hash.TLongLongHashMap;
import gnu.trove.set.TLongSet;
import lombok.Getter;

/**
 * Categorizes a given objects into classes, counting how many of the given objects are of which class.
 *
 * @author yawkat
 */
public class TypeCounter extends HeapDumpItemVisitor {
    /**
     * input object IDs
     */
    private final TLongSet objects;
    /**
     * Object counts by class
     * class ID -> object count in #objects
     */
    @Getter private final TLongLongMap counters = new TSynchronizedLongLongMap(new TLongLongHashMap());

    public TypeCounter(TLongSet objects) {
        super(true);
        this.objects = objects;
    }

    @Override
    public boolean visitInstanceHeader(HprofHeapDumpInstanceHeader item) {
        if (objects.contains(item.id)) {
            counters.adjustOrPutValue(item.classObjectId, 1, 1);
        }
        return false;
    }

    @Override
    public boolean visitObjectArrayDumpHeader(HprofHeapDumpObjectArrayDumpHeader item) {
        if (objects.contains(item.id)) {
            counters.adjustOrPutValue(item.classObjectId, 1, 1);
        }
        return false;
    }
}
