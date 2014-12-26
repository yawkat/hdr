package at.yawk.hdr.gui.type;

import at.yawk.hdr.gui.MainController;
import at.yawk.hdr.index.Indexer;
import at.yawk.hdr.index.ListenableFuture;
import at.yawk.hdr.index.ObjectData;
import at.yawk.hdr.index.TypeData;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.set.TLongSet;
import javafx.application.Platform;
import javafx.collections.ObservableListBase;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * @author yawkat
 */
public class TypeController extends ReferenceOwnerController<TypeData> {
    @FXML ListView<Long> instances;
    @FXML Text size;
    @FXML Text count;
    @FXML Text totalSize;
    @FXML BorderPane referenceWrapper;
    @FXML ProgressBar referenceCounterProgress;

    @FXML Object tree;
    @FXML ReferenceTreeController treeController;

    @Override
    public void init(Indexer indexer, MainController mainController, TypeData typeData) {
        super.init(indexer, mainController, typeData);

        int instanceSize = typeData.getClassHeader().instanceSize;
        size.setText(String.valueOf(instanceSize));
        int instanceCount = typeData.getInstanceCount();
        count.setText(String.valueOf(instanceCount));
        totalSize.setText(String.valueOf(instanceSize * instanceCount));

        instances.setCellFactory(param -> new ListCell<Long>() {
            {
                getStyleClass().add("object-id");
                setAlignment(Pos.CENTER_RIGHT);
                setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                            ObjectData data = new ObjectData();
                            data.id = getItem();
                            data.classId = typeData.getClassHeader().id;
                            mainController.openObjectData(indexer, data);
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);

                if (item == null || empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(Long.toHexString(item));
                }
            }
        });

        ListenableFuture<TLongSet> objectFuture =
                treeController.load(indexer, mainController, typeData.getClassHeader().id);
        objectFuture.addListener(() -> {
            TLongList objects;
            try {
                objects = new TLongArrayList(objectFuture.get());
            } catch (InterruptedException e) {
                throw new AssertionError(e); // should be done at this point
            }
            objects.sort();
            Platform.runLater(() -> {
                instances.setItems(new LongObservableList(objects));
                instances.setVisible(true);
            });
        });
    }

    @RequiredArgsConstructor
    private static class LongObservableList extends ObservableListBase<Long> {
        private final TLongList list;

        @NotNull
        @Override
        public Long get(int index) {
            return list.get(index);
        }

        @Override
        public int size() {
            return list.size();
        }
    }
}
