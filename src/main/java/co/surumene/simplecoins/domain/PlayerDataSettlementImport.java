package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Stages one authoritative playerdata snapshot in a settlement transaction.
 * The caller is responsible for resolving the current BAN state and supplying
 * a stable, unique source. This does not itself freeze files or publish totals.
 */
public final class PlayerDataSettlementImport {
    private PlayerDataSettlementImport() {}

    public static void stage(SettlementAccumulator settlement, String source,
                             UUID player, boolean currentlyBanned,
                             Map<String, Object> playerdata) throws IOException {
        Objects.requireNonNull(settlement);
        Objects.requireNonNull(player);
        Map<String, BigInteger> slots;
        try {
            slots = SavedPlayerAssets.extract(playerdata);
        } catch (IOException failure) {
            settlement.failSource(source, "Playerdata could not be accounted: " + failure.getMessage());
            throw failure;
        }
        for (var entry : slots.entrySet()) {
            String location = "playerdata/" + player + "/" + entry.getKey();
            settlement.accept(new SettlementAccumulator.Asset(
                    source, location, null, player, false, currentlyBanned,
                    true, entry.getValue(), location));
        }
        settlement.completeSource(source);
    }
}
