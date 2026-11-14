package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.GrantLedger;
import co.surumene.simplecoins.domain.GrantReceiptState;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Read-only candidate bridge from one bounded, already-decoded Paper
 * playerdata NBT root to the persistent issuance journal.
 *
 * The caller must independently prove file authority, stable saved/live
 * generation, and correct playerdata UUID. This does not save playerdata,
 * mutate inventory or authorize a live offline grant replay.
 */
public final class SavedGrantReceiptNbt {
    private static final String BUKKIT_VALUES = "BukkitValues";
    private static final String KEY = "simplecoins:grant_receipt_v1";

    private SavedGrantReceiptNbt() { }

    public static GrantReceiptState readAndVerify(Map<String, Object> savedRoot,
                                                  GrantLedger issuance,
                                                  UUID expectedPlayer) throws IOException {
        if (savedRoot == null || issuance == null || expectedPlayer == null)
            throw new IOException("Missing authoritative playerdata or issuance journal");
        GrantReceiptState receipt = GrantReceiptState.empty();
        if (savedRoot.containsKey(BUKKIT_VALUES)) {
            Object stored = savedRoot.get(BUKKIT_VALUES);
            if (!(stored instanceof Map<?, ?> values))
                throw new IOException("Playerdata BukkitValues is not an NBT compound");
            if (values.containsKey(KEY)) {
                Object raw = values.get(KEY);
                if (!(raw instanceof byte[] bytes))
                    throw new IOException("Saved grant receipt has an unexpected NBT tag type");
                try {
                    receipt = PaperGrantReceiptPdc.fromSavedBytes(bytes);
                } catch (IllegalArgumentException | IllegalStateException invalid) {
                    throw new IOException("Corrupt saved playerdata grant receipt", invalid);
                }
            }
        }
        try {
            receipt.verify(issuance, expectedPlayer);
        } catch (IllegalArgumentException | IllegalStateException invalid) {
            throw new IOException("Saved grant receipt disagrees with durable issuance evidence", invalid);
        }
        return receipt;
    }
}
