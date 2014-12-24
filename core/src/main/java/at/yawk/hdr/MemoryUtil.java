package at.yawk.hdr;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.concurrent.ThreadSafe;
import lombok.extern.slf4j.Slf4j;

/**
 * Utilities for monitoring memory usage.
 *
 * @author yawkat
 */
@Slf4j
@ThreadSafe
public class MemoryUtil {
    /**
     * Weak
     */
    private static final Map<Object, Integer> SOFT_RESOURCES = Collections.synchronizedMap(new WeakHashMap<>());

    private MemoryUtil() {}

    /**
     * Register an object that will be freed by the GC when necessary (it's stored in a soft reference somewhere else)
     *
     * @param sizeBytes Estimated size of the object.
     */
    public static void registerSoftResource(Object reference, int sizeBytes) {
        log.trace("Registered soft resource {} with length {}", reference, sizeBytes);
        SOFT_RESOURCES.put(reference, sizeBytes);
    }

    /**
     * Count total size of resources that can be freed when necessary (soft refs only) and are registered in this
     * class.
     */
    public static long countSoftResourceBytes() {
        AtomicLong result = new AtomicLong(0);
        SOFT_RESOURCES.forEach((k, v) -> result.addAndGet(v));
        return result.get();
    }
}
