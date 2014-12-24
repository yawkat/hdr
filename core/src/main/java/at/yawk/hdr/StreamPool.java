package at.yawk.hdr;

import java.io.IOException;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Pool of PooledInputStreams for multi-threaded use.
 *
 * @author yawkat
 */
@ThreadSafe
public interface StreamPool {
    /**
     * Open a stream at the given position.
     */
    PooledInputStream open(long pos) throws InterruptedException, IOException;
}
