package at.yawk.hdr.index;

import at.yawk.hdr.format.HprofClassLoad;
import at.yawk.hdr.format.HprofHeapDumpClassField;
import at.yawk.hdr.format.HprofHeapDumpClassHeader;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Getter;

/**
 * Data on types (arrays, classes, interfaces etc)
 *
 * @author yawkat
 */
public class TypeData extends ReferenceOwnerData {
    AtomicInteger instanceCount = new AtomicInteger();
    AtomicLong memoryUsage = new AtomicLong();
    @Getter HprofClassLoad classLoad = null;
    @Getter HprofHeapDumpClassHeader classHeader = null;
    List<HprofHeapDumpClassField> fields = Collections.emptyList();
    List<HprofHeapDumpClassField> fieldsWithInherited = Collections.emptyList();
    byte arrayBaseType = -1;

    public int getInstanceCount() {
        return instanceCount.get();
    }

    public long getMemoryUsage() {
        return memoryUsage.get();
    }

    public List<HprofHeapDumpClassField> getFields() {
        return Collections.unmodifiableList(fields);
    }

    public List<HprofHeapDumpClassField> getFieldsWithInherited() {
        return Collections.unmodifiableList(fieldsWithInherited);
    }
}
