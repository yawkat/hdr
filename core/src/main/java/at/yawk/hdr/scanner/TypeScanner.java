package at.yawk.hdr.scanner;

import at.yawk.hdr.format.HprofHeapDumpInstanceHeader;
import at.yawk.hdr.format.HprofHeapDumpObjectArrayDumpHeader;
import at.yawk.hdr.index.HeapDumpItemVisitor;
import gnu.trove.impl.sync.TSynchronizedLongSet;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import lombok.Getter;

/**
 * Scans for all objects of a given type.
 *
 * @author yawkat
 */
public class TypeScanner extends HeapDumpItemVisitor {
    private final long type;
    /**
     * Object IDs of this type
     */
    @Getter private final TLongSet objects = new TSynchronizedLongSet(new TLongHashSet());

    public TypeScanner(long type) {
        super(true);
        this.type = type;
    }

    @Override
    public boolean visitInstanceHeader(HprofHeapDumpInstanceHeader item) {
        if (item.classObjectId == type) {
            objects.add(item.id);
        }
        return false;
    }

    @Override
    public boolean visitObjectArrayDumpHeader(HprofHeapDumpObjectArrayDumpHeader item) {
        if (item.classObjectId == type) {
            objects.add(item.id);
        }
        return false;
    }
}
