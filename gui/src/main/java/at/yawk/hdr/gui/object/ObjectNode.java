package at.yawk.hdr.gui.object;

import at.yawk.hdr.HeapDumpUtil;
import at.yawk.hdr.format.BaseType;
import at.yawk.hdr.format.HprofHeapDumpClassField;
import at.yawk.hdr.gui.DisposalListeners;
import at.yawk.hdr.index.Indexer;
import at.yawk.hdr.index.ListenableFuture;
import at.yawk.hdr.index.ObjectData;
import at.yawk.hdr.index.TypeData;
import at.yawk.hdr.scanner.ObjectFinder;
import com.google.common.base.Strings;
import gnu.trove.list.TLongList;
import gnu.trove.procedure.TLongProcedure;
import java.util.Arrays;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringEscapeUtils;

/**
 * @author yawkat
 */
@RequiredArgsConstructor
class ObjectNode {
    private final Indexer indexer;
    private final ObjectViewController controller;

    boolean root = false;

    TreeItem<CellValue> treeItem = new TreeItem<>();

    @Nullable ObjectNode parent;
    HprofHeapDumpClassField field;
    int arrayIndex = -1;
    @Nullable String fieldTypeName = null;

    byte primitiveType = BaseType.OBJECT;
    long primitiveValue;
    @Nullable ObjectData data;
    ObjectNode[] values;

    private ListenableFuture<?> loadFuture = null;

    private boolean prevShown = false;

    static String getPrimitiveValue(byte type, long value) {
        switch (type) {
        case BaseType.BOOLEAN:
            return value == 0 ? "false" : "true";
        case BaseType.BYTE:
        case BaseType.SHORT:
        case BaseType.INT:
        case BaseType.LONG:
            return Strings.padStart(Long.toHexString(value), BaseType.LENGTH[type] * 2, '0') + " " + value;
        case BaseType.CHAR:
            return Strings.padStart(Long.toHexString(value), BaseType.LENGTH[type] * 2, '0') + " '" +
                   StringEscapeUtils.escapeJava(Character.toString((char) value)) + "'";
        case BaseType.DOUBLE:
            return String.valueOf(Double.longBitsToDouble(value));
        case BaseType.FLOAT:
            return String.valueOf(Float.intBitsToFloat((int) value));
        default:
            throw new UnsupportedOperationException("Cannot print primitive type " + type);
        }
    }

    boolean shown() {
        if (root) { return true; }
        TreeItem<?> parent = treeItem.getParent();
        if (parent == null) { return false; }
        if (!parent.isExpanded()) { return false; }
        return this.parent != null && this.parent.shown();
    }

    {
        treeItem.expandedProperty().addListener(o -> {
            if (values != null) {
                for (ObjectNode value : values) {
                    value.check(false);
                }
            }
        });
        treeItem.parentProperty().addListener(o -> check(false));
    }

    synchronized void check(boolean force) {
        boolean shown = shown();
        if (force || prevShown != shown) {
            prevShown = shown;
            if (values == null) {
                if (shown) {
                    request(indexer);
                } else {
                    stopRequest();
                }
            } else {
                for (ObjectNode value : values) {
                    value.check(force);
                }
            }
            updateCellValue();
        }
    }

    @SuppressWarnings("unchecked")
    private void request(Indexer indexer) {
        if (data != null && loadFuture == null) {
            if (data.fields == null) {
                loadFuture = indexer.walkHeapDump(new ObjectFinder(indexer, data, () -> {
                    if (loadFuture != null) {
                        loadFuture.cancel(true);
                        buildValues(indexer);
                        if (root) {
                            Platform.runLater(() -> controller.updateBase(data));
                        }
                    }
                }));
                DisposalListeners.bind(controller.tree, loadFuture, true);
            } else {
                buildValues(indexer);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void buildValues(final Indexer indexer) {
        assert data != null;
        assert data.fields != null;
        values = new ObjectNode[data.fields.size()];
        TypeData type = indexer.getTypeIndex().get(data.classId);
        assert type != null;
        data.fields.forEach(new TLongProcedure() {
            int i = 0;

            @Override
            public boolean execute(long value) {
                ObjectNode wrapper = new ObjectNode(indexer, controller);
                wrapper.parent = ObjectNode.this;
                if (data.primitiveArrayType == -1) {
                    wrapper.field = type.getFieldsWithInherited().get(i);
                    if (wrapper.field.type == BaseType.OBJECT) {
                        if (value == 0) {
                            wrapper.fieldTypeName = "null";
                        } else {
                            wrapper.data = new ObjectData();
                            wrapper.data.id = value;
                        }
                    } else {
                        wrapper.fieldTypeName = BaseType.NAME[wrapper.field.type];
                        wrapper.primitiveType = wrapper.field.type;
                        wrapper.primitiveValue = value;
                    }
                } else {
                    wrapper.primitiveType = data.primitiveArrayType;
                    wrapper.arrayIndex = i;
                    wrapper.fieldTypeName = HeapDumpUtil.pathToClassName(
                            indexer.getStringIndex().get(type.getClassLoad().nameId), 1);

                    if (wrapper.primitiveType == BaseType.OBJECT) {
                        wrapper.data = new ObjectData();
                        wrapper.data.id = value;
                    } else {
                        wrapper.primitiveValue = value;
                    }
                }
                values[i++] = wrapper;
                return true;
            }
        });

        Platform.runLater(() -> {
            treeItem.getChildren().addAll(
                    Arrays.stream(values).map(wrapper -> wrapper.treeItem).toArray(TreeItem[]::new));
            check(true);
            for (ObjectNode value : values) {
                if (value.data != null) {
                }
            }
        });
    }

    private void updateCellValue() {
        if (!shown()) {
            return;
        }

        StringBuilder value = new StringBuilder();
        if (field != null) {
            value.append(indexer.getStringIndex().get(field.nameId)).append(": \t");
        }
        String className = fieldTypeName;
        if (data != null) {
            TypeData typeData = indexer.getTypeIndex().get(data.classId);
            if (typeData != null) {
                className = typeData.getName();
            }
        }
        value.append(className == null ? "???" : className);

        if (data != null) {
            value.append(" / ").append(ObjectViewController.getObjectId(data.id));

            // display short char arrays as strings for convenience
            TLongList fields = data.fields;
            if (data.primitiveArrayType == BaseType.CHAR && fields != null && fields.size() < 0xff) {
                StringBuilder builder = new StringBuilder(fields.size());
                fields.forEach(value1 -> {
                    builder.append((char) value1);
                    return true;
                });
                value.append(" = \"").append(StringEscapeUtils.escapeJava(builder.toString())).append('"');
            }

        } else if (primitiveType != BaseType.OBJECT) {
            value.append(" = ").append(getPrimitiveValue(primitiveType, primitiveValue));
        }

        treeItem.setValue(new CellValue(this, value.toString()));
    }

    private void stopRequest() {
        if (loadFuture != null && !loadFuture.isDone()) {
            loadFuture.cancel(true);
            loadFuture = null;
        }
    }
}
