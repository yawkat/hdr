package at.yawk.hdr.gui;

import at.yawk.hdr.gui.root.RootController;
import at.yawk.hdr.gui.type.ReferenceOwnerController;
import at.yawk.hdr.gui.type.TypeController;
import at.yawk.hdr.index.Indexer;
import at.yawk.hdr.index.ReferenceOwnerData;
import at.yawk.hdr.index.StackData;
import at.yawk.hdr.index.TypeData;
import java.util.Comparator;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.FlowPane;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;

/**
 * @author yawkat
 */
@Slf4j
public class MainController {
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
                    RootController.Tab annotation = c.getAnnotation(RootController.Tab.class);
                    FXMLLoader loader = new FXMLLoader();
                    Parent parent = FX.inflate(loader, c);

                    loader.<RootController>getController().init(indexer, this);

                    Tab tab = new Tab(annotation.title());
                    tab.setClosable(false);
                    tab.setContent(parent);
                    tabs.getTabs().add(tab);
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
        loader.<ReferenceOwnerController<R>>getController().init(indexer, this, data);

        Tab tab = new Tab();
        tab.setText(data.getName());
        tab.setClosable(true);
        tab.setContent(parent);
        tabs.getTabs().add(tab);
        tabs.getSelectionModel().select(tab);
    }
}
