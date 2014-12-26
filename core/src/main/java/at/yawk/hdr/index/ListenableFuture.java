package at.yawk.hdr.index;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

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

    default <T> ListenableFuture<T> map(Function<? super V, T> mapper) {
        ListenableFuture<V> parent = this;
        return new ListenableFuture<T>() {
            @Override
            public T get() throws InterruptedException {
                return mapper.apply(parent.get());
            }

            @Override
            public T get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
                return mapper.apply(parent.get(timeout, unit));
            }

            @Override
            public void addListener(Runnable listener) {
                parent.addListener(listener);
            }

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return parent.cancel(mayInterruptIfRunning);
            }

            @Override
            public boolean isCancelled() {
                return parent.isCancelled();
            }

            @Override
            public boolean isDone() {
                return parent.isDone();
            }
        };
    }
}
