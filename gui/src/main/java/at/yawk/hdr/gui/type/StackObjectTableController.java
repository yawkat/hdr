package at.yawk.hdr.gui.type;

import at.yawk.hdr.gui.Controls;
import at.yawk.hdr.gui.MainController;
import at.yawk.hdr.gui.root.CommonObjectsTableController;
import at.yawk.hdr.index.Indexer;
import at.yawk.hdr.index.TypeData;
import gnu.trove.map.TLongLongMap;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yawkat
 */
@Controls("stack_object_table.fxml")
public class StackObjectTableController extends CommonObjectsTableController {
    private TLongLongMap counters;

    public void init(Indexer indexer, MainController mainController, TLongLongMap counters) {
        this.counters = counters;

        init(indexer, mainController);
    }

    @Override
    protected List<TypeData> getEntries(Indexer indexer) {
        List<TypeData> entries = new ArrayList<>(counters.size());
        counters.forEachKey(type -> {
            entries.add(indexer.getTypeIndex().get(type));
            return true;
        });
        return entries;
    }

    @Override
    protected long getInstanceCount(TypeData typeData) {
        return counters.get(typeData.getClassHeader().id);
    }

    @Override
    protected long getMemoryUsage(TypeData typeData) {
        return getInstanceCount(typeData) * typeData.getClassHeader().instanceSize; // TODO
    }
}
