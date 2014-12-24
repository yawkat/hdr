package at.yawk.hdr.gui.tree;

import java.util.HashMap;
import java.util.Map;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

/**
 * @author yawkat
 */
abstract class SingletonListObjectProperty<T> extends ObjectProperty<T> {
    private final ObservableList<T> list;

    private final Map<Object, Object> listeners = new HashMap<>();

    private ObservableValue<? extends T> boundTo;
    @SuppressWarnings("unchecked")
    private final InvalidationListener bindListener = observable ->
            set(((ObservableValue<? extends T>) observable).getValue());

    @Override
    public boolean isBound() {
        return boundTo != null;
    }

    @Override
    public void unbind() {
        if (isBound()) {
            boundTo.removeListener(bindListener);
            boundTo = null;
        }
    }

    @Override
    public void bind(ObservableValue<? extends T> observable) {
        unbind();
        observable.addListener(bindListener);
        boundTo = observable;
    }

    public SingletonListObjectProperty(ObservableList<T> list) {
        this.list = list;
    }

    @Override
    public T get() {
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public void set(T value) {
        if (value == null) {
            list.clear();
        } else {
            if (list.isEmpty()) {
                list.add(value);
            } else {
                list.set(0, value);
            }
        }
    }

    @Override
    public void addListener(ChangeListener<? super T> listener) {
        ListChangeListener<T> del = c -> {
            T old;
            if (c.getRemovedSize() > 0) {
                old = c.getRemoved().get(0);
            } else {
                old = null;
            }
            listener.changed(this, old, getValue());
        };
        listeners.put(listener, del);
        list.addListener(del);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void removeListener(ChangeListener<? super T> listener) {
        ListChangeListener<T> del = (ListChangeListener<T>) listeners.remove(listener);
        if (del != null) { list.removeListener(del); }
    }

    @Override
    public void addListener(InvalidationListener listener) {
        InvalidationListener del = observable -> listener.invalidated(this);
        listeners.put(listener, del);
        list.addListener(del);
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        InvalidationListener del = (InvalidationListener) listeners.remove(listener);
        if (del != null) { list.removeListener(del); }
    }
}
