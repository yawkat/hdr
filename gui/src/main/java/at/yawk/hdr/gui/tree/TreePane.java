package at.yawk.hdr.gui.tree;

import javafx.beans.DefaultProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Rectangle;
import javafx.util.Callback;

/**
 * @author yawkat
 */
@DefaultProperty("rootNode")
public class TreePane<T> extends Region {

    public final ObjectProperty<TreeNode<T>> rootNodeProperty() {
        if (rootNode == null) {
            rootNode = new SingletonListObjectProperty<TreeNode<T>>(rootWrapper.getChildren()) {
                @Override
                public Object getBean() {
                    return TreePane.this;
                }

                @Override
                public String getName() {
                    return "rootNode";
                }
            };
        }
        return rootNode;
    }

    private final TreeNode<T> rootWrapper = new TreeNode<>(null);
    private ObjectProperty<TreeNode<T>> rootNode;

    public final void setRootNode(TreeNode<T> rootNode) { rootNodeProperty().set(rootNode); }

    public final TreeNode<T> getRootNode() { return rootNodeProperty().get(); }

    //

    public ObjectProperty<Callback<? super TreeNode<T>, Node>> cellValueFactoryProperty() {
        if (cellValueFactory == null) {
            cellValueFactory = new ObjectPropertyBase<Callback<? super TreeNode<T>, Node>>() {
                @Override
                public Object getBean() {
                    return TreePane.this;
                }

                @Override
                public String getName() {
                    return "cellValueFactory";
                }
            };
        }
        return cellValueFactory;
    }

    private ObjectProperty<Callback<? super TreeNode<T>, Node>> cellValueFactory;

    public void setCellValueFactory(Callback<? super TreeNode<T>, Node> cellValueFactory) {
        cellValueFactoryProperty().set(cellValueFactory);
    }

    public Callback<? super TreeNode<T>, Node> getCellValueFactory() {
        return cellValueFactory == null ? null : cellValueFactory.get();
    }

    private Callback<? super TreeNode<T>, Node> getCellValueFactory0() {
        Callback<? super TreeNode<T>, Node> factory = getCellValueFactory();
        return factory == null ? DefaultCellValueFactory.instance : factory;
    }

    // borders

    private Canvas canvas;

    // drag

    private double dragX;
    private double dragY;
    private double transX = 0;
    private double transY = 0;
    private double transXActual;
    private double transYActual;

    private double scale = 1; // TODO

    {
        setOnMousePressed(event -> {
            dragX = transX - event.getX();
            dragY = transY - event.getY();
        });
        setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                transX = dragX + event.getX();
                transY = dragY + event.getY();
                requestLayout();
            }
        });
    }

    // hover

    TreeNode<T> previousHover = null;

    private static final String TREE_NON_HOVER_CLASS = "tree-entry-normal";
    private static final String TREE_HOVER_CLASS = "tree-entry-hover";

    {
        setOnMouseMoved(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                TreeNode<T> node = findNodeAtScreenPosition(event.getX(), event.getY());
                if (node != previousHover) {
                    if (previousHover != null) {
                        previousHover.node.getStyleClass().remove(TREE_HOVER_CLASS);
                        previousHover.node.getStyleClass().add(TREE_NON_HOVER_CLASS);
                    }
                    if (node != null && node.show) {
                        node.node.toFront();
                        node.node.getStyleClass().remove(TREE_NON_HOVER_CLASS);
                        node.node.getStyleClass().add(TREE_HOVER_CLASS);
                        setCursor(Cursor.HAND);
                    } else {
                        setCursor(null);
                    }
                    previousHover = node;
                }
            }
        });
        setOnMouseClicked(event -> {
            TreeNode<T> node = findNodeAtScreenPosition(event.getX(), event.getY());
            if (node != null && node.show) {
                EventHandler<? super MouseEvent> listener = node.node.getOnMouseClicked();
                if (listener != null) {
                    listener.handle(event);
                }
            }
        });
    }

    // impl

    {
        rootWrapper.setListener(this::onChange);
        rootWrapper.radius = 0;
        rootWrapper.startAngle = Math.PI;
        rootWrapper.endAngle = Math.PI * 3;
    }

    private void onChange(TreeNode<T> node, ListChangeListener.Change<? extends TreeNode<T>> change) {
        while (change.next()) {
            for (TreeNode<T> removed : change.getRemoved()) {
                deepOp(removed, true);
            }
            for (TreeNode<T> added : change.getAddedSubList()) {
                deepOp(added, true);
            }
        }
        change.reset();
        rebuild();
    }

    private void deepOp(TreeNode<T> node, boolean add) {
        if (add) {
            node.node = getCellValueFactory0().call(node);
            node.node.getStyleClass().add(TREE_NON_HOVER_CLASS);
            getChildren().add(node.node);
        } else {
            getChildren().remove(node.node);
            node.node = null;
        }
        for (TreeNode<T> child : node.getChildren()) {
            deepOp(child, add);
        }
    }

    @Override
    protected void layoutChildren() {
        rebuild();
    }

    private void rebuild() {
        transXActual = transX + getWidth() / 2;
        transYActual = transY + getHeight() / 2;

        if (canvas != null) { getChildren().remove(canvas); }
        canvas = new Canvas();
        getChildren().add(0, canvas);
        canvas.relocate(0, 0);
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());

        canvas.getGraphicsContext2D().setStroke(Color.GRAY);
        canvas.getGraphicsContext2D().setFill(Color.GRAY);
        canvas.getGraphicsContext2D().setLineWidth(1);

        TreeNode<T> root = getRootNode();
        if (root != null) {
            root.startAngle = rootWrapper.startAngle;
            root.endAngle = rootWrapper.endAngle;
            root.radius = rootWrapper.radius;

            position(root);
        }
    }

    private void position(TreeNode<T> node) {
        node.node.setVisible(node.show);

        double innerRadius = node.radius + 50;
        double outerRadius = node.radius + 150;
        if (node.show) {
            double prefW;
            double prefH;
            if (node.node.getContentBias() == Orientation.VERTICAL) {
                prefH = node.node.prefHeight(-1);
                prefW = node.node.prefWidth(prefH);
            } else {
                prefW = node.node.prefWidth(-1);
                prefH = node.node.prefHeight(prefW);
            }
            double angle = (node.startAngle + node.endAngle) / 2;
            double cx = Math.sin(angle) * node.radius + transXActual;
            double cy = Math.cos(angle) * node.radius + transYActual;
            double x = cx - prefW / 2;
            double y = cy - prefH / 2;
            node.node.relocate(x, y);
            node.node.setClip(new Rectangle(-x, -cy + prefH / 4, getWidth(), getHeight()));

            canvas.getGraphicsContext2D().strokeArc(
                    transXActual - innerRadius, transYActual - innerRadius,
                    innerRadius * 2, innerRadius * 2,
                    Math.toDegrees(node.startAngle) - 90,
                    Math.toDegrees(node.endAngle - node.startAngle),
                    ArcType.OPEN
            );
        }

        double start = node.startAngle;
        double scale = (node.endAngle - node.startAngle) /
                       node.getChildren().stream().mapToDouble(TreeNode::getSize).sum();

        for (int i = 0; i < node.getChildren().size(); i++) {
            TreeNode<T> child = node.getChildren().get(i);

            child.startAngle = start;
            child.endAngle = start + scale * child.getSize();
            start = child.endAngle;

            child.radius = node.radius + 100; // TODO proper sizing
            child.show = node.show && Math.abs(child.endAngle - child.startAngle) > Math.PI / 80;

            position(child);

            if (child.show) {
                drawRayComponent(innerRadius, outerRadius, child.startAngle);
                drawRayComponent(innerRadius, outerRadius, child.endAngle);
            }
        }
    }

    private void drawRayComponent(double innerRadius, double outerRadius, double angle) {
        double sx = Math.sin(angle) * innerRadius + transXActual;
        double sy = Math.cos(angle) * innerRadius + transYActual;
        double tx = Math.sin(angle) * outerRadius + transXActual;
        double ty = Math.cos(angle) * outerRadius + transYActual;
        canvas.getGraphicsContext2D().strokeLine(sx, sy, tx, ty);
    }

    private TreeNode<T> findNodeAtScreenPosition(double x, double y) {
        return findNodeAtCanvasPosition(x - transXActual, y - transYActual);
    }

    private TreeNode<T> findNodeAtCanvasPosition(double x, double y) {
        double angle = mmod(Math.atan2(x, y), Math.PI * 2);
        double radius = Math.sqrt(x * x + y * y);

        TreeNode<T> node = getRootNode();

        depth:
        while (node != null) {
            if (radius < node.radius + 50) {
                return node;
            }
            for (TreeNode<T> child : node.getChildren()) {
                if (child.contains(angle)) {
                    node = child;
                    continue depth;
                }
            }
            break;
        }
        return null;
    }

    private static double mmod(double b, double d) {
        return ((b % d) + d) % d;
    }
}
