package at.yawk.hdr.index;

import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author yawkat
 */
public abstract class Index<T> implements Iterable<T> {
    public abstract int size();

    @Override
    public Spliterator<T> spliterator() {
        return Spliterators.spliterator(iterator(), size(), Spliterator.DISTINCT | Spliterator.SIZED);
    }

    public Stream<T> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * Copy all values into a mutable list
     */
    public List<T> toList() {
        return stream().collect(Collectors.toList());
    }
}
