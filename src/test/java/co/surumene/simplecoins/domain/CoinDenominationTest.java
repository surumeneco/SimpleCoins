package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class CoinDenominationTest {
    @Test void supportedDenominationsAreStableAndRoundTrip() {
        assertEquals(List.of(1, 5, 10, 50, 100, 500, 1000, 5000, 10000),
                Arrays.stream(CoinDenomination.values())
                        .map(CoinDenomination::value).toList());
        for (var denomination : CoinDenomination.values())
            assertSame(denomination, CoinDenomination.fromValue(denomination.value()));
    }

    @Test void unsupportedDenominationsCannotAcquireAnImplicitValue() {
        for (int invalid : new int[]{-1, 0, 2, 999, 10001})
            assertThrows(IllegalArgumentException.class,
                    () -> CoinDenomination.fromValue(invalid));
    }
}
