package co.surumene.simplecoins.domain;

import java.io.IOException;

/** Fail-closed decoding of a saved player grant receipt PDC long. */
public final class GrantReceiptValue {
    private GrantReceiptValue() {}

    /**
     * Only a genuinely absent key represents sequence zero.
     * A present key of another PDC type must not silently become zero.
     */
    public static long read(boolean present, Long typedValue) throws IOException {
        if (!present) {
            if (typedValue != null)
                throw new IOException("Inconsistent grant receipt key presence");
            return 0L;
        }
        if (typedValue == null || typedValue < 0)
            throw new IOException("Corrupt grant receipt value or PDC type");
        return typedValue;
    }
}
