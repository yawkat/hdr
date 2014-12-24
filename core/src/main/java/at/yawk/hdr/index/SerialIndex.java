package at.yawk.hdr.index;

import gnu.trove.impl.sync.TSynchronizedIntObjectMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.util.Iterator;
import java.util.function.Supplier;

/**
 * Index for items with serial numbers (threads, stack trace elements etc)
 *
 * @author yawkat
 */
public final class SerialIndex<T> extends Index<T> {
    private final TIntObjectMap<T> items = new TSynchronizedIntObjectMap<>(new TIntObjectHashMap<>());

    void add(int serial, T item) {
        items.put(serial, item);
    }

    T computeIfAbsent(int id, Supplier<T> supplier) {
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

    public T get(int serial) {
        return items.get(serial);
    }

    @Override
    public Iterator<T> iterator() {
        return items.valueCollection().iterator();
    }

    @Override
    public int size() {
        return items.size();
    }
}
