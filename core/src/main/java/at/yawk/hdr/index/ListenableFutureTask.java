package at.yawk.hdr.index;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yawkat
 */
class ListenableFutureTask<V> extends FutureTask<V> implements ListenableFuture<V> {
    private static final Logger log = LoggerFactory.getLogger(ListenableFutureTask.class);

    private final Collection<Runnable> listeners = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public ListenableFutureTask(Callable<V> callable) {
        super(callable);
    }

    public ListenableFutureTask(Runnable runnable, V result) {
        super(runnable, result);
    }

    @Override
    public void run() {
        try {
            super.run();
        } finally {
            if (!isCancelled()) {
                for (Runnable listener : listeners) {
                    try {
                        listener.run();
                    } catch (Throwable e) {
                        log.error("Failed to execute listener " + listener, e);
                    }
                }
                listeners.clear();
            }
        }
    }

    @Override
    public synchronized void addListener(Runnable listener) {
        if (isDone() && !isCancelled()) {
            listener.run();
        } else {
            listeners.add(listener);
        }
    }

    @Override
    public V get() throws InterruptedException {
        try {
            return super.get();
        } catch (ExecutionException e) {
            log.error("Execution exception", e);
            return null;
        }
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        try {
            return super.get(timeout, unit);
        } catch (ExecutionException e) {
            log.error("Execution exception", e);
            return null;
        }
    }
}
