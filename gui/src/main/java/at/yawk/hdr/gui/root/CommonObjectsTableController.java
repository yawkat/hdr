package at.yawk.hdr.gui.root;

import at.yawk.hdr.format.HprofHeapDumpClassHeader;
import at.yawk.hdr.gui.Controls;
import at.yawk.hdr.gui.MainController;
import at.yawk.hdr.gui.SpaceSeparatedIntegerCell;
import at.yawk.hdr.index.Indexer;
import at.yawk.hdr.index.TypeData;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
@Controls("common_objects_table.fxml")
@RootController.Tab(title = "Types", priority = 0)
public class CommonObjectsTableController extends ReferenceOwnerTableController<TypeData> implements RootController {
    @FXML private TableColumn<TypeData, Long> instanceCount;
    @FXML private TableColumn<TypeData, Long> bytes;

    public void init(Indexer indexer, MainController mainController) {
        super.init(indexer, mainController);

        List<TypeData> entries = getEntries(indexer);
        Collections.sort(entries, Comparator.comparingLong(e -> -getInstanceCount(e)));

        instanceCount.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(getInstanceCount(param.getValue())));
        bytes.setCellValueFactory(param -> {
            TypeData typeData = param.getValue();
            HprofHeapDumpClassHeader header = typeData.getClassHeader();
            if (header == null) { return new ReadOnlyObjectWrapper<>(null); }
            return new ReadOnlyObjectWrapper<>(getMemoryUsage(typeData));
        });

        instanceCount.setCellFactory(column -> new SpaceSeparatedIntegerCell());
        bytes.setCellFactory(column -> new SpaceSeparatedIntegerCell());

        table.setItems(FXCollections.observableArrayList(entries));
    }

    protected List<TypeData> getEntries(Indexer indexer) {
        return indexer.getTypeIndex().toList();
    }

    protected long getInstanceCount(TypeData typeData) {
        return typeData.getInstanceCount();
    }

    protected long getMemoryUsage(TypeData typeData) {
        return typeData.getMemoryUsage();
    }
}
