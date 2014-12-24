package at.yawk.hdr.gui;

import java.io.IOException;
import java.net.URL;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

/**
 * @author yawkat
 */
public class FX {
    private FX() {}

    static <C> C open(Stage stage, String layout, String title) {
        return open(stage, layout, title, c -> {});
    }

    static <C> C open(Stage stage, String layout, String title, Consumer<C> controllerHandler) {
        FXMLLoader loader = new FXMLLoader();
        Parent parent = inflate(FX.class, loader, layout);
        controllerHandler.accept(loader.getController());
        FX.updateStage(stage, title, parent);
        return loader.getController();
    }

    public static Parent inflate(FXMLLoader loader, Class<?> controller) {
        return inflate(controller, loader, controller.getAnnotation(Controls.class).value());
    }

    public static Parent inflate(Class<?> context, FXMLLoader loader, String layout) {
        URL resource = context.getResource(layout);
        if (resource == null) {
            throw new NoSuchElementException("Missing resource " + layout + " in context " + context);
        }
        loader.setLocation(resource);
        loader.setBuilderFactory(new JavaFXBuilderFactory());
        Parent parent;
        try {
            parent = loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        parent.getStylesheets().add(FX.class.getResource("style.css").toExternalForm());
        return parent;
    }

    static void dump(Node n) { dump(n, 0); }

    private static void dump(Node n, int depth) {
        for (int i = 0; i < depth; i++) { System.out.print(" "); }
        System.out.println(n);
        if (n instanceof Parent) {
            for (Node c : ((Parent) n).getChildrenUnmodifiable()) {
                dump(c, depth + 1);
            }
        }
    }

    private static void updateStage(Stage stage, String title, Parent parent) {
        if (!Platform.isFxApplicationThread()) {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(() -> {
                updateStage(stage, title, parent);
                latch.countDown();
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return;
        }

        // resize to current window size
        if (parent instanceof Region) {
            ((Region) parent).setPrefSize(stage.getWidth(), stage.getHeight());
        }
        stage.setTitle(title);
        Scene scene = new Scene(parent);
        stage.setScene(scene);
        stage.show();
    }

    public static void bind(Future<?> future, Node node) {
    }
}
