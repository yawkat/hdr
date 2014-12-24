package at.yawk.hdr.gui.root;

import at.yawk.hdr.StackTraceBuilder;
import at.yawk.hdr.gui.Controls;
import at.yawk.hdr.gui.MainController;
import at.yawk.hdr.gui.SpaceSeparatedIntegerCell;
import at.yawk.hdr.index.Indexer;
import at.yawk.hdr.index.StackData;
import java.util.Comparator;
import java.util.List;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;

/**
 * @author yawkat
 */
@Controls("stack_table.fxml")
@RootController.Tab(title = "Stacks", priority = 100)
public class StackTableController extends ReferenceOwnerTableController<StackData> implements RootController {
    @FXML TableColumn<StackData, Integer> serial;
    @FXML TableColumn<StackData, Long> objectCount;

    @Override
    public void init(Indexer indexer, MainController mainController) {
        super.init(indexer, mainController);

        serial.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(
                param.getValue().getThreadObject().threadSerial));
        objectCount.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().getObjectCount()));

        objectCount.setCellFactory(column -> new SpaceSeparatedIntegerCell());

        List<StackData> entries = indexer.getThreadIndex().toList();
        entries.sort(Comparator.comparingInt(d -> d.getThreadObject().threadSerial));

        table.setItems(FXCollections.observableArrayList(entries));
    }

    @Override
    protected String getName(Indexer indexer, StackData value) {
        return new StackTraceBuilder(indexer).appendTrace(value.getThreadObject().stackTraceSerial).summarize();
    }
}
