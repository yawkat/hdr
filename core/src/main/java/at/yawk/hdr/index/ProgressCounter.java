package at.yawk.hdr.index;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author yawkat
 */
public abstract class ProgressCounter {
    /**
     * ProgressCounter that does nothing
     */
    static final ProgressCounter EMPTY = new ProgressCounter(Integer.MAX_VALUE) {
        @Override
        protected void updateValue(long value, long max) {}

        @Override
        void increment(long count) {}
    };

    /**
     * Update speed (updateValue will be called every x steps)
     */
    private final int granularity;
    private final AtomicLong value = new AtomicLong();
    private volatile long max = 0;

    public ProgressCounter(int granularity) {
        this.granularity = granularity;
    }

    void increment(long count) {
        while (true) {
            long prev = value.get();
            if (value.compareAndSet(prev, prev + count)) {
                if (prev / granularity != (prev + count) / granularity) {
                    updateValue(prev + count, max);
                }
                break;
            }
        }
    }

    void setMax(long max) {
        this.max = max;
        updateValue(value.get(), max);
    }

    protected abstract void updateValue(long value, long max);
}
