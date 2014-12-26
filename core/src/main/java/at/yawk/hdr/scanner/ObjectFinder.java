package at.yawk.hdr.scanner;

import at.yawk.hdr.format.*;
import at.yawk.hdr.index.HeapDumpItemVisitor;
import at.yawk.hdr.index.Indexer;
import at.yawk.hdr.index.ObjectData;
import gnu.trove.list.array.TLongArrayList;
import lombok.Getter;

/**
 * @author yawkat
 */
public class ObjectFinder extends HeapDumpItemVisitor {
    private final Indexer indexer;
    @Getter private final ObjectData data;
    private final Runnable onFind;

    public ObjectFinder(Indexer indexer, ObjectData data, Runnable onFind) {
        super(true);
        this.indexer = indexer;
        this.data = data;
        this.onFind = onFind;
    }

    public ObjectFinder(Indexer indexer, long objectId, Runnable onFind) {
        this(indexer, new ObjectData(), onFind);
        data.id = objectId;
    }

    // normal object

    @Override
    public boolean visitInstanceHeader(HprofHeapDumpInstanceHeader item) {
        if (item.id == data.id) {
            data.classId = item.classObjectId;
            data.primitiveArrayType = -1;
            data.fields = new TLongArrayList();
            return true;
        }
        return false;
    }

    @Override
    public void visitInstanceField(HprofHeapDumpInstanceHeader instance, HprofHeapDumpClassField declaring,
                                   long value) {
        data.fields.add(value);
    }

    @Override
    public void visitInstanceEnd(HprofHeapDumpInstanceHeader header) {
        onFind.run();
    }

    // primitive array

    @Override
    public boolean visitPrimitiveArrayDumpHeader(HprofHeapDumpPrimitiveArrayDumpHeader item) {
        if (item.id == data.id) {
            data.classId = indexer.getPrimitiveArrayType(item.elementType).getClassHeader().id;
            data.primitiveArrayType = item.elementType;
            data.fields = new TLongArrayList();
            return true;
        }
        return false;
    }

    @Override
    public void visitPrimitiveArrayEntry(long value) {
        data.fields.add(value);
    }

    @Override
    public void visitPrimitiveArrayEnd(HprofHeapDumpPrimitiveArrayDumpHeader header) {
        onFind.run();
    }

    // object array

    @Override
    public boolean visitObjectArrayDumpHeader(HprofHeapDumpObjectArrayDumpHeader item) {
        if (item.id == data.id) {
            data.classId = item.classObjectId;
            data.primitiveArrayType = BaseType.OBJECT;
            data.fields = new TLongArrayList();
            return true;
        }
        return false;
    }

    @Override
    public void visitObjectArrayEntry(HprofHeapDumpObjectArrayDumpHeader header, long ref) {
        data.fields.add(ref);
    }

    @Override
    public void visitObjectArrayEnd(HprofHeapDumpObjectArrayDumpHeader header) {
        onFind.run();
    }
}
