package at.yawk.hdr.gui;

import javafx.geometry.Pos;
import javafx.scene.control.TableCell;

/**
 * @author yawkat
 */
public class SpaceSeparatedIntegerCell<T, N extends Number> extends TableCell<T, N> {
    @Override
    protected void updateItem(N item, boolean empty) {
        super.updateItem(item, empty);

        if (item == null || empty) {
            setText(null);
            setStyle("");
        } else {
            StringBuilder str = new StringBuilder();
            str.append(item);
            for (int i = str.length() - 3; i > 0; i -= 3) {
                str.insert(i, ' ');
            }

            setText(str.toString());
            setAlignment(Pos.CENTER_RIGHT);
        }
    }
}
