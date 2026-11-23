package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.GrantLedger;
import co.surumene.simplecoins.domain.WriterEpochFence;
import co.surumene.simplecoins.domain.WorldStorageDiscovery;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Internal, unregistered G3 composition step for a FULL single-batch grant.
 *
 * All applicable world/native/async/file writers must be independently
 * proven to participate in the supplied same WriterEpochFence before this
 * class may be connected to the plugin. Current Agent coverage is incomplete:
 * this helper is deliberately package-private and is NOT invoked by startup,
 * player joining, a command, or a public issuance service.
 *
 * A seal of the existing limited-family fence alone cannot authorize G3.
 * This component merely closes the proof/ACK gap for a caller that HAS
 * established an exhaustive, continuously enforced writer interval.
 */
final class SavedGrantProtectedAckStage {
    private SavedGrantProtectedAckStage() {}

    /**
     * Finalize only a complete, authentic, single-batch, disk-persisted grant
     * while the same already-closed writer generation remains sealed.
     *
     * Caller owns the actual save and protected writer lifecycle. No
     * playerdata mutation/issuance or permit occurs here.
     *
     * If an error happens after ledger persistence began, the ACK outcome
     * must be recovered from disk; this method does NOT promise a pending
     * reservation or attempt a second physical issuance.
     */
    static synchronized SavedGrantDeliveryReadback.Proof finish(
            WriterEpochFence fence, WriterEpochFence.Seal seal,
            WorldStorageDiscovery.Manifest manifest, GrantLedger ledger,
            UUID player, GrantLedger.Grant grant, PaperGrantInventorySlotPlan plan,
            Map<String, Object> savedBefore,
            int maximumCompressedBytes, int maximumInflatedBytes) throws IOException {
        Objects.requireNonNull(fence, "shared installed writer fence");
        try {
            Objects.requireNonNull(manifest, "verified playerdata universe");
            Objects.requireNonNull(ledger, "durable grant journal");
            Objects.requireNonNull(player, "target player");
            Objects.requireNonNull(grant, "authentic reserved grant");
            Objects.requireNonNull(plan, "saved physical coin placement");
            Objects.requireNonNull(savedBefore, "saved prior playerdata");
            if (maximumCompressedBytes < 1 || maximumInflatedBytes < 1)
                throw new IllegalArgumentException("Invalid source readback byte budgets");

            // Do not claim previous partially saved stacks are present merely
            // because the final receipt says complete. Multi-batch durable
            // reconciliation requires a separate whole-grant proof.
            if (plan.previousReceipt().active()
                    || plan.nextReceipt().active()
                    || plan.nextReceipt().completedSequence() != grant.sequence()
                    || plan.placements().isEmpty()
                    || !player.equals(grant.player())
                    || grant.delivered()
                    || !ledger.pending(player).contains(grant))
                throw new IllegalStateException(
                        "Grant is not an authentic complete one-batch pending delivery");
            fence.requireDrained(seal);

            // The witness requires real origin-backed discovery and rejects
            // source/ancestor symlinks and missing, renamed or duplicate UUID
            // playerdata. A manually invented Manifest cannot authorize ACK.
            SavedPlayerdataContentWitness witness =
                    SavedPlayerdataContentWitness.capturePlayer(
                            manifest, player, maximumCompressedBytes);
            Path file = manifest.playerDataFiles().get(player);
            if (file == null || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS))
                throw new IOException("Missing authoritative saved playerdata source");
            // Both file contents and parent directory entry must be durable
            // before a fresh readback. No fake callback or stale proof accepted.
            try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ,
                    StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
                channel.force(true);
            }
            Path directory = file.getParent();
            if (directory == null || !Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS))
                throw new IOException("Missing authoritative playerdata directory");
            try (FileChannel channel = FileChannel.open(directory,
                    StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
                channel.force(true);
            }
            witness.requireUnchanged();
            fence.requireDrained(seal);

            SavedGrantDeliveryReadback.Proof fresh =
                    SavedGrantDeliveryFileReadback.verify(manifest, ledger, player, plan,
                            savedBefore, maximumCompressedBytes, maximumInflatedBytes);
            if (!fresh.receipt().equals(plan.nextReceipt())
                    || fresh.receipt().active()
                    || fresh.receipt().completedSequence() != grant.sequence())
                throw new IOException("Fresh authoritative receipt does not complete this grant");
            witness.requireUnchanged();
            fence.requireDrained(seal);

            // The ledger independently validates the exact persistent grant
            // handle. No caller-supplied stale receipt number is used.
            if (!ledger.pending(player).contains(grant))
                throw new IllegalStateException("Reservation changed before durable ACK");
            ledger.acknowledge(grant, fresh.receipt().completedSequence());
            fence.requireDrained(seal);
            return fresh;
        } catch (IOException | RuntimeException | Error failure) {
            // No stale seal may ever be reused after a partial file or journal
            // operation. A failure here may still mean durable ACK succeeded:
            // the caller MUST cold-reopen and reconcile rather than retry give.
            fence.invalidate();
            throw failure;
        }
    }
}
