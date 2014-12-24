package at.yawk.hdr.gui.tree;

import javafx.beans.DefaultProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;

/**
 * @author yawkat
 */
@DefaultProperty("children")
public class TreeNode<T> {
    public TreeNode() {}

    public TreeNode(T value) {
        this();
        setValue(value);
    }

    //

    public final ObjectProperty<T> valueProperty() {
        if (value == null) {
            value = new ObjectPropertyBase<T>() {
                @Override
                public Object getBean() {
                    return TreeNode.this;
                }

                @Override
                public String getName() {
                    return "value";
                }
            };
        }
        return value;
    }

    private ObjectProperty<T> value = null;

    public final void setValue(T value) { valueProperty().set(value); }

    public final T getValue() { return value == null ? null : value.get(); }

    //

    private ObservableList<TreeNode<T>> children = FXCollections.observableArrayList();

    public final ObservableList<TreeNode<T>> getChildren() { return children; }

    //

    public final DoubleProperty sizeProperty() {
        if (size == null) {
            size = new SimpleDoubleProperty(this, "size", 1);
        }
        return size;
    }

    private DoubleProperty size = null;

    public final void setSize(double size) { sizeProperty().set(size); }

    public final double getSize() { return size == null ? 1 : size.get(); }

    /// implementation ///

    double radius;
    double startAngle;
    double endAngle;
    boolean show = true;
    Node node;

    private TreeChangeListener<T> listener = null;

    {
        children.addListener((ListChangeListener<TreeNode<T>>) c -> {
            if (listener != null) {
                listener.onChange(this, c);
            }
            while (c.next()) {
                for (TreeNode<T> removed : c.getRemoved()) {
                    removed.setListener(null);
                }
                for (TreeNode<T> added : c.getAddedSubList()) {
                    added.setListener(listener);
                }
            }
            c.reset();
        });
    }

    void setListener(TreeChangeListener<T> listener) {
        this.listener = listener;
        for (TreeNode<T> child : children) {
            child.setListener(listener);
        }
    }

    boolean contains(double angle) {
        if (angle >= startAngle && angle < endAngle) { return true; }
        if (endAngle > Math.PI * 2 && angle < endAngle % (Math.PI * 2)) { return true; }
        return false;
    }
}
