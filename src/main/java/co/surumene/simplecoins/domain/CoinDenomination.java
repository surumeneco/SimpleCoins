package co.surumene.simplecoins.domain;

/** Stable denominations; display names and asset models must never redefine their value. */
public enum CoinDenomination {
    ONE(1), FIVE(5), TEN(10), FIFTY(50), HUNDRED(100), FIVE_HUNDRED(500),
    THOUSAND(1000), FIVE_THOUSAND(5000), TEN_THOUSAND(10000);

    private final int value;

    CoinDenomination(int value) { this.value = value; }
    public int value() { return value; }

    public static CoinDenomination fromValue(int value) {
        return switch (value) {
            case 1 -> ONE;
            case 5 -> FIVE;
            case 10 -> TEN;
            case 50 -> FIFTY;
            case 100 -> HUNDRED;
            case 500 -> FIVE_HUNDRED;
            case 1000 -> THOUSAND;
            case 5000 -> FIVE_THOUSAND;
            case 10000 -> TEN_THOUSAND;
            default -> throw new IllegalArgumentException("Unsupported denomination: " + value);
        };
    }
}
