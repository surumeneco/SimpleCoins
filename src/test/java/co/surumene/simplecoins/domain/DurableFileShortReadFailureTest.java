package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * TASK-20261101-005: bounded-stream short-read and late-failure Draft vectors.
 * This is not a crash/restart or journal replay integration test.
 */
class DurableFileShortReadFailureTest {
    private static final class ChunkedStream extends InputStream {
        private final byte[] bytes;
        private final int maxChunk;
        private final int failAfter;
        private int position;

        ChunkedStream(byte[] bytes, int maxChunk, int failAfter) {
            this.bytes = bytes;
            this.maxChunk = maxChunk;
            this.failAfter = failAfter;
        }

        @Override public int read() throws IOException {
            if (failAfter >= 0 && position >= failAfter)
                throw new IOException("Injected late stream failure");
            return position == bytes.length ? -1 : bytes[position++] & 0xff;
        }

        @Override public int read(byte[] target, int offset, int length) throws IOException {
            if (length == 0) return 0;
            if (failAfter >= 0 && position >= failAfter)
                throw new IOException("Injected late stream failure");
            if (position == bytes.length) return -1;
            int count = Math.min(Math.min(maxChunk, length), bytes.length - position);
            if (failAfter >= 0) count = Math.min(count, failAfter - position);
            System.arraycopy(bytes, position, target, offset, count);
            position += count;
            return count;
        }
    }

    private static byte[] payload(int size) {
        byte[] bytes = new byte[size];
        for (int i = 0; i < size; i++) bytes[i] = (byte) (i * 13 + 3);
        return bytes;
    }

    @Test void repeatedShortReadsReturnCompletePayload() throws Exception {
        byte[] expected = payload(8193);
        assertArrayEquals(expected,
                DurableFile.readBounded(new ChunkedStream(expected, 3, -1), 8193));
    }

    @Test void lateIOExceptionCannotReturnSuccessfullyReadPrefix() {
        byte[] bytes = payload(100);
        assertThrows(IOException.class, () ->
                DurableFile.readBounded(new ChunkedStream(bytes, 2, 7), 100));
    }

    @Test void shortReadsCannotBypassCumulativeByteBudget() {
        byte[] bytes = payload(13);
        assertThrows(IOException.class, () ->
                DurableFile.readBounded(new ChunkedStream(bytes, 3, -1), 12));
    }

    @Test void zeroBudgetDistinguishesEmptyAndNonemptyStreams() throws Exception {
        assertArrayEquals(new byte[0],
                DurableFile.readBounded(new ByteArrayInputStream(new byte[0]), 0));
        assertThrows(IOException.class, () ->
                DurableFile.readBounded(new ChunkedStream(payload(1), 1, -1), 0));
    }
}
