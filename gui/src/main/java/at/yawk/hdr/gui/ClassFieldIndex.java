package at.yawk.hdr.gui;

import at.yawk.hdr.format.HprofHeapDumpClassField;
import at.yawk.hdr.format.HprofHeapDumpClassHeader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.cliffc.high_scale_lib.NonBlockingHashMapLong;

/**
 * @author yawkat
 */
public class ClassFieldIndex {
    private static final List<HprofHeapDumpClassField> NULL_VALUE = new ArrayList<>();

    private final NonBlockingHashMapLong<List<HprofHeapDumpClassField>> declared = new NonBlockingHashMapLong<>();
    private final NonBlockingHashMapLong<Long> superClasses = new NonBlockingHashMapLong<>();
    private final NonBlockingHashMapLong<List<HprofHeapDumpClassField>> allFields = new NonBlockingHashMapLong<>();

    public void startClass(HprofHeapDumpClassHeader clazz) {
        superClasses.put(clazz.id, (Long) clazz.superClassObjectId);
        declared.put(clazz.id, new ArrayList<>());
    }

    public void addField(HprofHeapDumpClassHeader clazz, HprofHeapDumpClassField field) {
        // added in startClass
        declared.get(clazz.id).add(field);
    }

    public List<HprofHeapDumpClassField> getAllFieldsForClass(long id) {
        List<HprofHeapDumpClassField> l = allFields.get(id);
        if (l == null) {
            List<HprofHeapDumpClassField> decl = declared.get(id);
            if (decl == null) {
                l = NULL_VALUE;
            } else {
                l = new ArrayList<>();
                l.addAll(decl);
                Long sup = superClasses.get(id);
                if (sup != null) {
                    List<HprofHeapDumpClassField> next = getAllFieldsForClass(sup);
                    if (next != null) {
                        l.addAll(next);
                    }
                }
            }
            allFields.put(id, l);
        }
        return l == NULL_VALUE ? null : l;
    }
}
