package at.yawk.hdr.gui.root;

import at.yawk.hdr.gui.MainController;
import at.yawk.hdr.index.Indexer;
import at.yawk.hdr.index.ReferenceOwnerData;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

/**
 * @author yawkat
 */
abstract class ReferenceOwnerTableController<R extends ReferenceOwnerData> {
    @FXML TableView<R> table;
    @FXML TableColumn<R, String> name;

    public void init(Indexer indexer, MainController mainController) {
        table.setRowFactory(param -> {
            TableRow<R> row = new TableRow<>();
            row.setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                        mainController.openReferenceOwnerData(indexer, row.getItem());
                    }
                }
            });
            return row;
        });

        name.setCellValueFactory(param -> new ReadOnlyStringWrapper(getName(indexer, param.getValue())));
    }

    protected String getName(Indexer indexer, R value) {
        return value.getName();
    }
}
