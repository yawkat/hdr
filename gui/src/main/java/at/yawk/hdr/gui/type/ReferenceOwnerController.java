package at.yawk.hdr.gui.type;

import at.yawk.hdr.gui.MainController;
import at.yawk.hdr.index.Indexer;
import at.yawk.hdr.index.ReferenceOwnerData;
import javafx.fxml.FXML;
import javafx.scene.text.Text;

/**
 * @author yawkat
 */
public abstract class ReferenceOwnerController<R extends ReferenceOwnerData> {
    @FXML Text name;

    public void init(Indexer indexer, MainController mainController, R data) {
        name.setText(data.getName());
    }
}
