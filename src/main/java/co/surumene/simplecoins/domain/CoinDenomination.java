package co.surumene.simplecoins.domain;

import java.util.Arrays;

/** Stable denominations; display names and asset models must never redefine their value. */
public enum CoinDenomination {
    ONE(1), FIVE(5), TEN(10), FIFTY(50), HUNDRED(100), FIVE_HUNDRED(500),
    THOUSAND(1000), FIVE_THOUSAND(5000), TEN_THOUSAND(10000);

    private final int value;

    CoinDenomination(int value) { this.value = value; }
    public int value() { return value; }

    public static CoinDenomination fromValue(int value) {
        return Arrays.stream(values()).filter(d -> d.value == value).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported denomination: " + value));
    }
}
