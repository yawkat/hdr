package at.yawk.hdr.gui;

import at.yawk.hdr.index.ListenableFuture;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import javafx.scene.Node;
import javafx.scene.Parent;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
public class DisposalListeners {
    private static final Map<Node, Set<Runnable>> listeners = Collections.synchronizedMap(new WeakHashMap<>());

    private DisposalListeners() {}

    public static void addDisposalListener(Node node, Runnable listener) {
        listeners.computeIfAbsent(node, n -> {
            Set<Runnable> listeners = Collections.newSetFromMap(new ConcurrentHashMap<>());
            Runnable invokeListeners = () -> listeners.forEach(l -> {
                try {
                    l.run();
                } catch (Throwable e) {
                    log.error("Failed to execute listener " + l, e);
                }
            });
            node.parentProperty().addListener((observable, oldValue, newValue) -> {
                if (oldValue != null) {
                    removeDisposalListener(oldValue, invokeListeners);
                }
                if (newValue == null) {
                    invokeListeners.run();
                } else {
                    addDisposalListener(newValue, invokeListeners);
                }
            });
            Parent currentParent = node.getParent();
            if (currentParent != null) {
                addDisposalListener(currentParent, invokeListeners);
            }

            return listeners;
        }).add(listener);
    }

    public static void removeDisposalListener(Node node, Runnable listener) {
        Set<Runnable> listeners = DisposalListeners.listeners.get(node);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    /**
     * Cancel a future when the given node is disposed.
     */
    public static void bind(Node node, Future<?> future, boolean mayInterruptIfRunning) {
        if (future.isDone()) { return; }
        Runnable listener = () -> {
            if (!future.isDone()) {
                future.cancel(mayInterruptIfRunning);
            }
        };
        addDisposalListener(node, listener);
        if (future instanceof ListenableFuture) {
            ((ListenableFuture) future).addListener(() -> removeDisposalListener(node, listener));
        }
    }
}
