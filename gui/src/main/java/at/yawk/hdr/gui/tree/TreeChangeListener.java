package at.yawk.hdr.gui.tree;

import javafx.collections.ListChangeListener;

/**
 * @author yawkat
 */
@FunctionalInterface
interface TreeChangeListener<T> {
    void onChange(TreeNode<T> node, ListChangeListener.Change<? extends TreeNode<T>> change);
}
