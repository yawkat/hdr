package at.yawk.hdr.index;

import java.util.Iterator;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import org.cliffc.high_scale_lib.NonBlockingHashMapLong;

/**
 * Thread safe id -> Object index for Hprof IDs (object IDs, class IDs etc)
 *
 * @author yawkat
 */
@ThreadSafe
public final class IdIndex<T> extends Index<T> {
    private final NonBlockingHashMapLong<T> items = new NonBlockingHashMapLong<>();

    void add(long id, T item) {
        items.put(id, item);
    }

    T computeIfAbsent(long id, Supplier<T> supplier) {
        T item = items.get(id);
        if (item == null) {
            item = supplier.get();
            T prev = items.putIfAbsent(id, item);
            if (prev != null) {
                item = prev;
            }
        }
        return item;
    }

    @Nullable
    public T get(long id) {
        return items.get(id);
    }

    @Override
    public Iterator<T> iterator() {
        return items.values().iterator();
    }

    @Override
    public int size() {
        return items.size();
    }
}
