package at.yawk.hdr.index;

import java.util.Arrays;

/**
 * @author yawkat
 */
class MultiProgressCounter extends ProgressCounter {
    private ProgressCounter[] counters;

    public MultiProgressCounter(ProgressCounter... counters) {
        super(1);
        this.counters = counters;
    }

    synchronized void add(ProgressCounter counter) {
        counters = Arrays.copyOf(counters, counters.length + 1);
        counters[counters.length - 1] = counter;
    }

    @Override
    void increment(long count) {
        for (ProgressCounter counter : counters) {
            counter.increment(count);
        }
    }

    @Override
    void setMax(long max) {
        for (ProgressCounter counter : counters) {
            counter.setMax(max);
        }
    }

    @Override
    protected void updateValue(long value, long max) {
        throw new UnsupportedOperationException();
    }
}
