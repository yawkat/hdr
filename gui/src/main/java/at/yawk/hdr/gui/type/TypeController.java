package at.yawk.hdr.gui.type;

import at.yawk.hdr.gui.MainController;
import at.yawk.hdr.index.Indexer;
import at.yawk.hdr.index.TypeData;
import javafx.fxml.FXML;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;

/**
 * @author yawkat
 */
public class TypeController extends ReferenceOwnerController<TypeData> {
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

        treeController.load(indexer, mainController, typeData.getClassHeader().id);
    }
}
