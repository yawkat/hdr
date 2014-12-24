package at.yawk.hdr.gui.tree;

import javafx.scene.Node;
import javafx.scene.text.Text;
import javafx.util.Callback;

/**
 * @author yawkat
 */
class DefaultCellValueFactory implements Callback<TreeNode<?>, Node> {
    static final Callback instance = new DefaultCellValueFactory();

    @Override
    public Node call(TreeNode<?> param) {
        return new Text(String.valueOf(param.getValue()));
    }
}
