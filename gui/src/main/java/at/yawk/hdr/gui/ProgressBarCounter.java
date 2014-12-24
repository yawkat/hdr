package at.yawk.hdr.gui;

import at.yawk.hdr.index.ProgressCounter;
import javafx.application.Platform;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;

/**
 * @author yawkat
 */
public class ProgressBarCounter extends ProgressCounter {
    public static final int DEFAULT_GRANULARITY = 1 << 20;

    private final ProgressBar progressBar;
    private final double startProgress;
    private final double progressMultiplier;

    public ProgressBarCounter(ProgressBar progressBar) {
        this(DEFAULT_GRANULARITY, progressBar, 0, 1);
    }

    public ProgressBarCounter(int granularity, ProgressBar progressBar,
                              double startProgress, double endProgress) {
        super(granularity);
        this.progressBar = progressBar;
        this.startProgress = startProgress;
        this.progressMultiplier = endProgress - startProgress;
    }

    @Override
    protected void updateValue(long value, long max) {
        Platform.runLater(() -> {
            if (max <= 0) {
                progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
            } else {
                progressBar.setProgress(startProgress + progressMultiplier * value / max);
            }
        });
    }
}
