package at.yawk.hdr.format;

import java.lang.reflect.Field;

/**
 * @author yawkat
 */
public abstract class HprofItem<T extends HprofItem<T>> implements Cloneable {
    @SuppressWarnings({ "unchecked", "CloneDoesntDeclareCloneNotSupportedException" })
    @Override
    public T clone() {
        try {
            return (T) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName()).append('{');
        try {
            toString(builder, getClass(), this);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        builder.setLength(builder.length() - 2); // clip last ', ', we'll assume we have at least one field
        builder.append('}');
        return builder.toString();
    }

    private static void toString(StringBuilder into, Class<?> on, Object obj) throws IllegalAccessException {
        if (on == null) { return; }
        toString(into, on.getSuperclass(), obj);

        for (Field field : on.getDeclaredFields()) {
            field.setAccessible(true);
            into.append(field.getName()).append('=');
            into.append(field.get(obj));
            into.append(", ");
        }
    }
}
