package at.yawk.hdr.gui.object;

import at.yawk.hdr.format.BaseType;
import at.yawk.hdr.gui.Controller;
import at.yawk.hdr.gui.Controls;
import at.yawk.hdr.gui.MainController;
import at.yawk.hdr.index.Indexer;
import at.yawk.hdr.index.ObjectData;
import at.yawk.hdr.index.TypeData;
import com.google.common.base.Strings;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;

/**
 * @author yawkat
 */
@Controls("object_view.fxml")
public class ObjectViewController extends Controller {
    Indexer indexer;
    MainController mainController;

    @FXML Text name;
    @FXML Text size;
    @FXML TreeView<CellValue> tree;

    public void init(Indexer indexer, MainController mainController, ObjectData data) {
        this.indexer = indexer;
        this.mainController = mainController;

        updateBase(data);

        ObjectNode wrapper = new ObjectNode(indexer, this);
        wrapper.data = data;
        wrapper.root = true;
        wrapper.treeItem.setExpanded(true);
        tree.setRoot(wrapper.treeItem);

        tree.setCellFactory(param -> new TreeCell<CellValue>() {
            {
                setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                            CellValue item = getItem();
                            if (item != null) {
                                ObjectData data = item.getNode().data;
                                if (data != null) {
                                    mainController.openObjectData(indexer, data);
                                }
                            }
                        }
                    }
                });
            }

            @Override
            protected void updateItem(CellValue item, boolean empty) {
                super.updateItem(item, empty);

                if (item == null || empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getValue());
                }
            }
        });

        wrapper.check(true);
    }

    void updateBase(ObjectData data) {
        String idString = getObjectId(data.id);
        TypeData typeData = indexer.getTypeIndex().get(data.classId);

        long len = -1;
        String title;
        if (typeData == null) {
            title = "??? / " + idString;
        } else {
            title = typeData.getName() + " / " + idString;
            if (data.primitiveArrayType == -1) {
                len = typeData.getFieldsWithInherited().stream()
                        .mapToLong(field -> BaseType.LENGTH[field.type])
                        .sum();
            } else {
                if (data.fields != null) {
                    len = data.fields.size() * BaseType.LENGTH[data.primitiveArrayType];
                }
            }
        }
        name.setText(title);
        setTitle(title);
        size.setText(len == -1 ? "" : String.valueOf(len));
    }

    static String getObjectId(long id) {
        return Long.toHexString(id);
    }
}
