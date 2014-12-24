package at.yawk.hdr.gui.type;

import at.yawk.hdr.StackTraceBuilder;
import at.yawk.hdr.gui.DisposalListeners;
import at.yawk.hdr.gui.FX;
import at.yawk.hdr.gui.MainController;
import at.yawk.hdr.gui.ProgressBarCounter;
import at.yawk.hdr.index.Indexer;
import at.yawk.hdr.index.ListenableFuture;
import at.yawk.hdr.index.StackData;
import at.yawk.hdr.scanner.StackReferenceScanner;
import at.yawk.hdr.scanner.TypeCounter;
import gnu.trove.set.hash.TLongHashSet;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;

/**
 * @author yawkat
 */
public class StackController extends ReferenceOwnerController<StackData> {
    @FXML Text objectsProgressText;
    @FXML ProgressBar objectsProgress;
    @FXML BorderPane objectsWrapper;
    @FXML Text trace;

    @Override
    public void init(Indexer indexer, MainController mainController, StackData data) {
        super.init(indexer, mainController, data);

        String trace = new StackTraceBuilder(indexer)
                .appendTrace(data.getThreadObject().stackTraceSerial)
                .compile();
        this.trace.setText(trace);

        StackReferenceScanner refScanner = new StackReferenceScanner(data.getThreadObject().threadSerial);
        ListenableFuture<?> f1 = indexer.walkHeapDump(
                refScanner,
                new ProgressBarCounter(ProgressBarCounter.DEFAULT_GRANULARITY, objectsProgress, 0, 0.5)
        );
        DisposalListeners.bind(objectsWrapper, f1, true);
        f1.addListener(() -> {
            // copy since the ref scanner uses a synchronized one and we don't need that
            TLongHashSet objects = new TLongHashSet(refScanner.getObjects());
            TypeCounter counter = new TypeCounter(objects);
            ListenableFuture<?> f2 = indexer.walkHeapDump(
                    counter,
                    new ProgressBarCounter(ProgressBarCounter.DEFAULT_GRANULARITY, objectsProgress, 0.5, 1)
            );
            Platform.runLater(() -> objectsProgressText.setText("Resolving Types…"));
            DisposalListeners.bind(objectsWrapper, f2, true);
            f2.addListener(() -> {
                FXMLLoader loader = new FXMLLoader();
                Parent parent = FX.inflate(loader, StackObjectTableController.class);
                loader.<StackObjectTableController>getController().init(indexer, mainController, counter.getCounters());
                Platform.runLater(() -> objectsWrapper.setCenter(parent));
            });
        });
    }
}
