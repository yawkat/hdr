package at.yawk.hdr.index;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Future, that
 *
 * - does not throw ExecutionException
 * - allows adding completion listeners
 *
 * @author yawkat
 */
public interface ListenableFuture<V> extends Future<V> {
    V get() throws InterruptedException;

    V get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException;

    /**
     * Add a listener that will be called upon completion, or, if this task is complete, immediately.
     */
    void addListener(Runnable listener);
}
