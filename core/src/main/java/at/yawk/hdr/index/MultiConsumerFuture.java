package at.yawk.hdr.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@RequiredArgsConstructor
class MultiConsumerFuture<V> {
    private final ListenableFuture<V> future;

    private final List<Future> children = Collections.synchronizedList(new ArrayList<>());

    public ListenableFuture<V> openFuture() {
        Future f = new Future();
        children.add(f);
        return f;
    }

    private class Future implements ListenableFuture<V> {
        volatile boolean cancelled;
        volatile boolean mayInterruptIfRunning;

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (!cancelled || mayInterruptIfRunning != this.mayInterruptIfRunning) {
                this.cancelled = true;
                this.mayInterruptIfRunning = mayInterruptIfRunning;
                synchronized (children) {
                    if (children.stream().allMatch(c -> c.cancelled)) {
                        boolean iir = children.stream().allMatch(c -> c.mayInterruptIfRunning);
                        future.cancel(iir);
                    }
                }
            }
            return !isDone();
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public boolean isDone() {
            return future.isDone();
        }

        @Override
        public V get() throws InterruptedException {
            return future.get();
        }

        @Override
        public V get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
            return future.get(timeout, unit);
        }

        @Override
        public void addListener(Runnable listener) {
            future.addListener(() -> {
                if (!isCancelled()) {
                    listener.run();
                }
            });
        }
    }
}
