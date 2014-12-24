package at.yawk.hdr.gui;

import at.yawk.hdr.MemoryCachedFilePool;
import at.yawk.hdr.index.Indexer;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ProgressBar;
import javafx.scene.text.Text;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yawkat
 */
@Slf4j
public class LoadingController {
    @FXML ProgressBar progress;
    @FXML Text text;

    public void load(Path heapDump, Consumer<Indexer> callback) throws FileNotFoundException {
        Logger logger = LoggerFactory.getLogger(Indexer.class);
        int parallelism = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(
                parallelism,
                new ThreadFactory() {
                    int i = 0;

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r);
                        thread.setName("Pool thread #" + ++i);
                        thread.setDaemon(true);
                        return thread;
                    }
                }
        );
        Indexer indexer = new Indexer(new MemoryCachedFilePool(heapDump.toFile()), executor);
        executor.execute(() -> {
            try {
                indexer.scanRootIndex();

                Platform.runLater(() -> text.setText("Building type index…"));
                indexer.scanTypeIndex(new ProgressBarCounter(progress));

                callback.accept(indexer);
            } catch (IOException | InterruptedException e) {
                logger.error("Failed to build root index", e);
                // TODO error message
            }
        });
    }
}
