package at.yawk.hdr.gui;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import javafx.application.Application;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * @author yawkat
 */
public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        prepareLogging();

        String input = getParameters().getNamed().get("input");
        Path opened;
        if (input == null) {
            opened = requestFile(primaryStage);
        } else {
            opened = expandShell(Paths.get(input));
        }
        openMainWindow(primaryStage, opened);
    }

    private void prepareLogging() {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    private static Path expandShell(Path path) {
        if (path.startsWith("~")) {
            path = Paths.get(System.getProperty("user.home")).resolve(path.subpath(1, path.getNameCount()));
        }
        return path.toAbsolutePath();
    }

    private Path requestFile(Stage primaryStage) throws IOException {
        FileChooser classFileChooser = new FileChooser();
        classFileChooser.setTitle("Heap Dump");
        return classFileChooser.showOpenDialog(primaryStage).toPath();
    }

    private void openMainWindow(Stage primaryStage, Path file) throws IOException {
        LoadingController loadingController = FX.<LoadingController>open(
                primaryStage,
                "loading.fxml",
                "HDR - " + file.toString() + " - Indexing"
        );
        loadingController.load(file, indexer -> FX.<MainController>open(
                primaryStage,
                "main.fxml",
                "HDR - " + file.toString(),
                d -> d.init(indexer)
        ));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
