package co.surumene.simplecoins.domain;

/**
 * Coins share PAPER with vanilla recipes. Result-taking from a merchant or a
 * cartography table must not irreversibly consume a PDC-identified coin.
 */
public final class PaperCurrencyConsumption {
    private PaperCurrencyConsumption() {}

    public static boolean blocksResult(String station, int rawSlot,
                                       boolean coinInFirstInput, boolean coinInSecondInput) {
        return rawSlot == 2 && (coinInFirstInput || coinInSecondInput)
                && ("MERCHANT".equals(station) || "CARTOGRAPHY".equals(station));
    }
}
