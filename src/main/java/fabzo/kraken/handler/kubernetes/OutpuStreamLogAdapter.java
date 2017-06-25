package fabzo.kraken.handler.kubernetes;

import org.slf4j.Logger;

import java.io.IOException;
import java.io.OutputStream;

/**
 * OutputStream that logs the input to the given logger.
 *
 * Credits: Stefan, https://stackoverflow.com/a/28579006/2235502
 */
public class OutpuStreamLogAdapter extends OutputStream {
    /**
     * Default number of bytes in the buffer.
     */
    private static final int DEFAULT_BUFFER_LENGTH = 2048;

    /**
     * Indicates stream state.
     */
    private boolean hasBeenClosed = false;

    /**
     * Internal buffer where data is stored.
     */
    private byte[] buf;

    /**
     * The number of valid bytes in the buffer.
     */
    private int count;

    /**
     * Remembers the size of the buffer.
     */
    private int curBufLength;

    /**
     * The logger to write to.
     */
    private final Logger log;
    private final String logPrefix;
    private final boolean flushOnLineBreak;

    /**
     * Creates the Logging instance to flush to the given logger.
     */
    public OutpuStreamLogAdapter(final Logger log, final String logPrefix, final boolean flushOnLineBreak) throws IllegalArgumentException {
        this.logPrefix = logPrefix;
        this.flushOnLineBreak = flushOnLineBreak;
        if (log == null) {
            throw new IllegalArgumentException("Logger or log level must be not null");
        }
        this.log = log;
        curBufLength = DEFAULT_BUFFER_LENGTH;
        buf = new byte[curBufLength];
        count = 0;
    }

    /**
     * Writes the specified byte to this output stream.
     *
     * @param b the byte to write
     * @throws IOException if an I/O error occurs.
     */
    public void write(final int b) throws IOException {
        if (hasBeenClosed) {
            throw new IOException("The stream has been closed.");
        }
        // don't log nulls
        if (b == 0) {
            return;
        }

        if (flushOnLineBreak && b == '\n') {
            flush();
        } else {
            // would this be writing past the buffer?
            if (count == curBufLength) {
                // grow the buffer
                final int newBufLength = curBufLength + DEFAULT_BUFFER_LENGTH;
                final byte[] newBuf = new byte[newBufLength];
                System.arraycopy(buf, 0, newBuf, 0, curBufLength);
                buf = newBuf;
                curBufLength = newBufLength;
            }

            buf[count] = (byte) b;
            count++;
        }
    }

    /**
     * Flushes this output stream and forces any buffered output
     * bytes to be written out.
     */
    public void flush() {
        if (count == 0) {
            return;
        }
        final byte[] bytes = new byte[count];
        System.arraycopy(buf, 0, bytes, 0, count);
        final String str = new String(bytes);
        log.info("[{}], {}", logPrefix, str);
        count = 0;
    }

    /**
     * Closes this output stream and releases any system resources
     * associated with this stream.
     */
    public void close() {
        flush();
        hasBeenClosed = true;
    }

}
