package at.yawk.hdr;

import java.io.IOException;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * InputStream-like interface for streams given out by a StreamPool.
 *
 * @author yawkat
 */
@NotThreadSafe
public interface PooledInputStream extends AutoCloseable {
    int read() throws IOException;

    int read(byte b[]) throws IOException;

    int read(byte b[], int off, int len) throws IOException;

    /**
     * Read a fixed-length byte array. Note that returned arrays may be reused so clone them if you want to store them
     * before calling this method again.
     */
    byte[] read(int count) throws IOException;

    void seekBy(long count) throws IOException;

    void seek(long pos) throws IOException;

    /**
     * Free this stream.
     */
    @Override
    void close();

    long getPosition();

    /**
     * @return whether our position is behind the size of the underlying file
     */
    boolean hitEnd() throws IOException;
}
