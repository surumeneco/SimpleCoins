package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.GrantLedger;
import co.surumene.simplecoins.domain.GrantReceiptState;
import co.surumene.simplecoins.domain.PlayerDataReader;
import co.surumene.simplecoins.domain.WorldStorageDiscovery;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

/**
 * After-save read-only, fail-closed proof based on the *actual* compressed
 * UUID playerdata file. Paper Player.saveData() is void and the underlying
 * PlayerDataStorage may silently return or log and swallow an IOException;
 * returning from that call can never be considered a persisted grant.
 *
 * This method does not call saveData, alter a physical inventory/PDC, write
 * a journal, fsync storage or issue money. Even a stable file readback is not
 * an atomic world snapshot, crash-durability guarantee or admission permit.
 */
public final class SavedGrantDeliveryFileReadback {
    private SavedGrantDeliveryFileReadback() {}

    @FunctionalInterface
    interface AfterRead {
        void run() throws IOException;
    }

    public static SavedGrantDeliveryReadback.Proof verify(
            WorldStorageDiscovery.Manifest manifest, GrantLedger ledger,
            UUID player, PaperGrantInventorySlotPlan staged,
            Map<String, Object> previouslySavedRoot,
            int maximumCompressedBytes, int maximumInflatedBytes) throws IOException {
        return verify(manifest, ledger, player, staged, previouslySavedRoot,
                maximumCompressedBytes, maximumInflatedBytes, () -> { });
    }

    /** Package-private deterministic failure-injection seam; never mutates. */
    static SavedGrantDeliveryReadback.Proof verify(
            WorldStorageDiscovery.Manifest manifest, GrantLedger ledger,
            UUID player, PaperGrantInventorySlotPlan staged,
            Map<String, Object> previouslySavedRoot,
            int maximumCompressedBytes, int maximumInflatedBytes,
            AfterRead afterRead) throws IOException {
        if (manifest == null || ledger == null || player == null
                || staged == null || previouslySavedRoot == null || afterRead == null)
            throw new IOException("Incomplete after-save playerdata verification");
        if (maximumCompressedBytes < 1 || maximumInflatedBytes < 1)
            throw new IllegalArgumentException("Playerdata readback budgets must be positive");

        // This checks exact UUID membership in a real source discovery. No
        // fabricated manifest may confer authority on a caller-selected file.
        var witness = SavedPlayerdataContentWitness.capturePlayer(
                manifest, player, maximumCompressedBytes);
        Path file = manifest.playerDataFiles().get(player);
        Map<String, Object> diskRoot = PlayerDataReader.read(
                file, maximumCompressedBytes, maximumInflatedBytes);
        var proof = SavedGrantDeliveryReadback.verifyTransition(
                previouslySavedRoot, diskRoot, ledger, player, staged);
        afterRead.run();
        witness.requireUnchanged();
        // Do not acknowledge a journal entry here. A successful readback is
        // only one prerequisite for a future physically durable commit.
        return proof;
    }
}
