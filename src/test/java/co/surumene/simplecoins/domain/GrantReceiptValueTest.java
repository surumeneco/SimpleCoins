package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class GrantReceiptValueTest {
    @Test void onlyAbsentReceiptDefaultsToZero() throws Exception {
        assertEquals(0L, GrantReceiptValue.read(false, null));
        assertEquals(0L, GrantReceiptValue.read(true, 0L));
        assertEquals(42L, GrantReceiptValue.read(true, 42L));
        assertEquals(Long.MAX_VALUE, GrantReceiptValue.read(true, Long.MAX_VALUE));
    }

    @Test void presentWrongTypeOrNegativeReceiptFailsClosed() {
        assertThrows(IOException.class, () -> GrantReceiptValue.read(true, null));
        assertThrows(IOException.class, () -> GrantReceiptValue.read(true, -1L));
        assertThrows(IOException.class, () -> GrantReceiptValue.read(false, 1L));
    }
}
