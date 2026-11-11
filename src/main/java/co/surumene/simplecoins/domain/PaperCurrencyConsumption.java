package co.surumene.simplecoins.domain;

/** Pure result-slot guard for vanilla PAPER consumption outside crafting. */
public final class PaperCurrencyConsumption {
    private PaperCurrencyConsumption() {}

    public static boolean blocksResult(String station, int rawSlot,
                                       boolean coinInFirstInput, boolean coinInSecondInput) {
        return rawSlot == 2 && (coinInFirstInput || coinInSecondInput)
                && ("MERCHANT".equals(station) || "CARTOGRAPHY".equals(station));
    }
}
