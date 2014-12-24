package at.yawk.hdr.index;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yawkat
 */
class MultiFuture<Void> implements ListenableFuture<Void> {
    private static final Logger log = LoggerFactory.getLogger(MultiFuture.class);

    private final Collection<ListenableFuture<?>> children;
    private final Collection<Runnable> listeners = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public MultiFuture(Collection<ListenableFuture<?>> children) {
        this.children = children;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean success = false;
        for (Future<?> child : children) {
            success |= child.cancel(mayInterruptIfRunning);
        }
        return success;
    }

    @Override
    public boolean isCancelled() {
        return children.stream().allMatch(Future::isCancelled);
    }

    @Override
    public boolean isDone() {
        return children.stream().allMatch(Future::isDone);
    }

    @Override
    public Void get() throws InterruptedException {
        for (ListenableFuture<?> child : children) {
            child.get();
        }
        return null;
    }

    @Override
    public Void get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        for (ListenableFuture<?> child : children) {
            child.get(timeout, unit);
        }
        return null;
    }

    @Override
    public void addListener(Runnable listener) {
        if (isDone()) {
            if (!isCancelled()) {
                listener.run();
            }
            return;
        }

        if (listeners.isEmpty()) {
            synchronized (listeners) {
                if (listeners.isEmpty()) {
                    AtomicInteger todo = new AtomicInteger(children.size());
                    for (ListenableFuture<?> child : children) {
                        child.addListener(() -> {
                            if (todo.decrementAndGet() == 0) {
                                for (Runnable l : listeners) {
                                    try {
                                        l.run();
                                    } catch (Throwable e) {
                                        log.error("Failed to execute listener " + l, e);
                                    }
                                }
                            }
                        });
                    }
                }
            }
        }
        listeners.add(listener);
    }
}
