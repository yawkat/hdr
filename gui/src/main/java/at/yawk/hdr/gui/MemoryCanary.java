package at.yawk.hdr.gui;

import at.yawk.hdr.MemoryUtil;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ProgressBar;
import javafx.scene.text.Text;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
@Controls("memory_canary.fxml")
public class MemoryCanary extends Controller {
    private static final char[] SUFFIXES = { 'B', 'K', 'M', 'G', 'T' };

    @FXML ProgressBar softUsage;
    @FXML ProgressBar usage;
    @FXML Text text;

    @FXML
    private void initialize() {
        RunnableFuture<?> updater = new FutureTask<Void>(() -> {
            try {
                long lastMem = 0;
                while (!Thread.currentThread().isInterrupted()) {
                    long maxMem = Runtime.getRuntime().maxMemory();
                    if (maxMem == Long.MAX_VALUE) { maxMem = Runtime.getRuntime().totalMemory(); }
                    long currentMem = maxMem - Runtime.getRuntime().freeMemory();

                    if (lastMem == currentMem) { continue; }
                    lastMem = currentMem;

                    long softMem = MemoryUtil.countSoftResourceBytes();

                    double usage = (double) currentMem / maxMem;
                    double softUsage = (double) softMem / maxMem;
                    String text = formatBytes(currentMem) + "/" + formatBytes(maxMem) + " | " +
                                  Math.round(usage * 100) + "% | Cached: " + formatBytes(softMem);

                    Platform.runLater(() -> {
                        this.text.setText(text);
                        this.usage.setProgress(usage);
                        this.softUsage.setProgress(softUsage);
                    });

                    Thread.sleep(50);
                }
            } catch (InterruptedException ignored) {
            } catch (Throwable e) {
                log.error("Error in canary thread", e);
            }
        }, null);
        DisposalListeners.bind(usage, updater, true);
        Thread thread = new Thread(updater, "Memory canary thread");
        thread.setDaemon(true);
        thread.start();
    }

    private static String formatBytes(long bytes) {
        int suffixIndex = 0;
        double bytesD = bytes;
        while (bytesD >= 1024 && suffixIndex < SUFFIXES.length - 1) {
            bytesD /= 1024;
            suffixIndex++;
        }
        StringBuilder builder = new StringBuilder();
        if (bytesD < 100 && suffixIndex > 0) {
            builder.append(Math.round(bytesD * 10) / 10D);
        } else {
            builder.append(Math.round(bytesD));
        }
        return builder.append(SUFFIXES[suffixIndex]).toString();
    }
}
