package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.GrantLedger;
import co.surumene.simplecoins.domain.GrantReceiptState;
import co.surumene.simplecoins.domain.PlayerDataReader;
import co.surumene.simplecoins.domain.WorldStorageDiscovery;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Read-only inspection of one discovered saved-playerdata source universe
 * against EVERY authenticated issuance record. The scan cannot prove that
 * Paper saved inventory + receipt in one generation; it does not freeze
 * playerdata contents or publish money or admit any player.
 */
public final class SavedGrantReceiptUniverseAudit {
    private SavedGrantReceiptUniverseAudit() {}

    public record Report(int playersRead, int acknowledgedGrants,
                         int pendingGrants, int awaitingAcknowledgement) {
        public Report {
            if (playersRead < 0 || acknowledgedGrants < 0
                    || pendingGrants < 0 || awaitingAcknowledgement < 0
                    || awaitingAcknowledgement > pendingGrants)
                throw new IllegalArgumentException("Invalid saved receipt audit counts");
        }
    }

    public static Report scan(WorldStorageDiscovery.Manifest manifest, GrantLedger ledger,
                              int maximumCompressedBytes, int maximumInflatedBytes)
            throws IOException {
        return scan(manifest, ledger, maximumCompressedBytes, maximumInflatedBytes,
                player -> { });
    }

    /** Test seam for deterministic same-path rollback after observing one player. */
    @FunctionalInterface
    interface AfterPlayerRead {
        void observed(UUID player) throws IOException;
    }

    static Report scan(WorldStorageDiscovery.Manifest manifest, GrantLedger ledger,
                       int maximumCompressedBytes, int maximumInflatedBytes,
                       AfterPlayerRead afterRead) throws IOException {
        if (manifest == null || ledger == null || manifest.origin() == null)
            throw new IOException("Saved issuance audit requires a real discovered source manifest");
        if (maximumCompressedBytes < 1 || maximumInflatedBytes < 1)
            throw new IllegalArgumentException("Saved playerdata read limits must be positive");

        if (afterRead == null)
            throw new IllegalArgumentException("Missing saved playerdata observation callback");
        // Capture exact compressed playerdata bytes BEFORE any receipt is read.
        // An already-read player must not disappear or roll back unnoticed.
        var witness = SavedPlayerdataContentWitness.capture(manifest,
                maximumCompressedBytes);
        final java.util.List<GrantLedger.Grant> history;
        try {
            history = ledger.historySnapshot();
        } catch (IllegalStateException corrupt) {
            throw new IOException("Cannot audit unverified issuance history", corrupt);
        }

        Map<UUID, GrantReceiptState> receipts = new HashMap<>();
        for (var entry : manifest.playerDataFiles().entrySet().stream()
                .sorted(Map.Entry.comparingByKey()).toList()) {
            UUID player = entry.getKey();
            Path file = entry.getValue();
            // Each read uses the same bounded GZIP/NBT parser as the saved
            // full-world scanner, including strict canonical player file names.
            Map<String, Object> nbt = PlayerDataReader.read(
                    file, maximumCompressedBytes, maximumInflatedBytes);
            GrantReceiptState receipt = SavedGrantReceiptNbt.readAndVerify(nbt, ledger, player);
            if (receipts.putIfAbsent(player, receipt) != null)
                throw new IOException("Duplicate saved player receipt identity: " + player);
            afterRead.observed(player);
        }

        int acknowledged = 0, pending = 0, awaiting = 0;
        for (GrantLedger.Grant grant : history) {
            GrantReceiptState receipt = receipts.get(grant.player());
            if (grant.delivered()) {
                if (receipt == null || receipt.completedSequence() < grant.sequence())
                    throw new IOException("Acknowledged grant has missing or rolled-back playerdata: "
                            + grant.sequence() + " for " + grant.player());
                acknowledged = Math.addExact(acknowledged, 1);
            } else {
                if (grant.confirmedPartial() > 0 && receipt == null)
                    throw new IOException("Confirmed partial grant has missing playerdata: "
                            + grant.sequence() + " for " + grant.player());
                pending = Math.addExact(pending, 1);
                if (receipt != null && receipt.completedSequence() >= grant.sequence())
                    awaiting = Math.addExact(awaiting, 1);
            }
        }
        // A late-appearing source would invalidate all observations; a
        // source-set recheck is NOT a simultaneous content-generation witness.
        witness.requireUnchanged();
        try {
            if (!history.equals(ledger.historySnapshot()))
                throw new IOException("Issuance history changed during saved audit");
        } catch (IllegalStateException corrupt) {
            throw new IOException("Issuance journal changed during saved audit", corrupt);
        }
        return new Report(receipts.size(), acknowledged, pending, awaiting);
    }
}
