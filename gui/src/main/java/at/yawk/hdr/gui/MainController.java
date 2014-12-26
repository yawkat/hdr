package at.yawk.hdr.gui;

import at.yawk.hdr.format.HprofHeapDumpInstanceHeader;
import at.yawk.hdr.gui.object.ObjectViewController;
import at.yawk.hdr.gui.root.RootController;
import at.yawk.hdr.gui.type.ReferenceOwnerController;
import at.yawk.hdr.gui.type.TypeController;
import at.yawk.hdr.index.*;
import java.util.Comparator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;

/**
 * @author yawkat
 */
@Slf4j
public class MainController extends Controller {
    @FXML TabPane tabs;
    @FXML Object commonObjects;

    public void init(Indexer indexer) {
        Reflections reflections = new Reflections("at.yawk.hdr.gui");

        reflections.getTypesAnnotatedWith(RootController.Tab.class)
                .stream()
                .filter(c -> c.getAnnotation(RootController.Tab.class) != null) // no subclasses
                .filter(c -> {
                    if (!RootController.class.isAssignableFrom(c)) {
                        log.warn(c + " is annotated with RootController.Tab but does not extend RootController!");
                        return false;
                    }
                    return true;
                })
                .sorted(Comparator.comparingInt(c -> c.getAnnotation(RootController.Tab.class).priority()))
                .forEach(c -> {
                    FXMLLoader loader = new FXMLLoader();
                    Parent parent = FX.inflate(loader, c);

                    RootController controller = loader.<RootController>getController();
                    controller.init(indexer, this);

                    openTab(parent, (Controller) controller, false);
                });
    }

    public <R extends ReferenceOwnerData> void openReferenceOwnerData(Indexer indexer, R data) {
        String layout;
        if (data instanceof TypeData) {
            layout = "type.fxml";
        } else if (data instanceof StackData) {
            layout = "stack.fxml";
        } else {
            throw new UnsupportedOperationException("Cannot open reference data of type " + data.getClass());
        }

        FXMLLoader loader = new FXMLLoader();
        Parent parent = FX.inflate(TypeController.class, loader, layout);
        ReferenceOwnerController<R> controller = loader.<ReferenceOwnerController<R>>getController();
        controller.init(indexer, this, data);

        openTab(parent, controller, true);
    }

    public void openObjectData(Indexer indexer, ObjectData objectData) {
        FXMLLoader loader = new FXMLLoader();
        Parent parent = FX.inflate(loader, ObjectViewController.class);
        ObjectViewController controller = loader.<ObjectViewController>getController();
        controller.init(indexer, this, objectData);
        openTab(parent, controller, true);
    }

    private void openTab(Parent parent, Controller controller, boolean closeable) {
        Tab tab = new Tab();
        tab.textProperty().bind(controller.titleProperty());
        tab.setClosable(closeable);
        tab.setContent(parent);
        tabs.getTabs().add(tab);
        tabs.getSelectionModel().select(tab);
    }
}
