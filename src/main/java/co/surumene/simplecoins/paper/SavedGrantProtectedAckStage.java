package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.GrantLedger;
import co.surumene.simplecoins.domain.GrantReceiptState;
import java.math.BigInteger;
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
 * Internal, unregistered G3 composition step for a completed saved grant
 * (single-batch or final batch after a prior, separately verified partial).
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
        return finishVerifiedBatch(fence, seal, manifest, ledger, player, grant,
                plan, savedBefore, maximumCompressedBytes, maximumInflatedBytes, false);
    }

    /**
     * Finish ONLY a canonical final batch after a previously persisted active
     * partial receipt. The caller must separately prove physical custody of
     * all earlier saved batches under the comprehensive installed-writer epoch.
     *
     * This method proves the *new final delta* and preservation of old saved
     * storage within a protected fresh file-readback/ACK lease; it cannot
     * independently establish the history behind the previous partial cursor.
     * Deliberately unregistered and not invoked by normal Paper startup.
     */
    static synchronized SavedGrantDeliveryReadback.Proof finishLastPartialBatch(
            WriterEpochFence fence, WriterEpochFence.Seal seal,
            WorldStorageDiscovery.Manifest manifest, GrantLedger ledger,
            UUID player, GrantLedger.Grant grant, PaperGrantInventorySlotPlan plan,
            Map<String, Object> savedBefore,
            int maximumCompressedBytes, int maximumInflatedBytes) throws IOException {
        return finishVerifiedBatch(fence, seal, manifest, ledger, player, grant,
                plan, savedBefore, maximumCompressedBytes, maximumInflatedBytes, true);
    }

    private static SavedGrantDeliveryReadback.Proof finishVerifiedBatch(
            WriterEpochFence fence, WriterEpochFence.Seal seal,
            WorldStorageDiscovery.Manifest manifest, GrantLedger ledger,
            UUID player, GrantLedger.Grant grant, PaperGrantInventorySlotPlan plan,
            Map<String, Object> savedBefore,
            int maximumCompressedBytes, int maximumInflatedBytes,
            boolean finalBatchAfterPartial) throws IOException {
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

            // A prior active cursor is allowed ONLY for the final resumed
            // batch path. It does not itself authenticate older physical coins;
            // that custody proof belongs to the caller's prior saved generation.
            GrantReceiptState prior = plan.previousReceipt();
            GrantReceiptState completed = plan.nextReceipt();
            boolean validPrior = finalBatchAfterPartial
                    ? prior.active()
                            && prior.activeSequence() == grant.sequence()
                            && grant.id().equals(prior.activeId())
                            && prior.partialValue() > 0
                            && prior.partialValue() < grant.amount()
                            && prior.completedSequence() < grant.sequence()
                    : !prior.active();
            if (!validPrior || completed.active()
                    || completed.completedSequence() != grant.sequence()
                    || plan.placements().isEmpty()
                    || !player.equals(grant.player())
                    || grant.delivered()
                    || !ledger.pending(player).contains(grant))
                throw new IllegalStateException(
                        "Grant is not an authentic completed pending delivery batch");
            long newPhysicalValue = finalBatchAfterPartial
                    ? Math.subtractExact(grant.amount(), prior.partialValue()) : grant.amount();
            // The lease is acquired BEFORE all filesystem IO and stays owned
            // until the actual durable ACK has returned. This is stronger than
            // point-in-time requireDrained checks around a slow NBT decode.
            return fence.withDrainedLease(seal, () -> {
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
            if (!fresh.receipt().equals(completed)
                    || fresh.receipt().active()
                    || fresh.receipt().completedSequence() != grant.sequence()
                    || !BigInteger.valueOf(newPhysicalValue)
                            .equals(fresh.matchedMonetaryValue()))
                throw new IOException("Fresh authoritative receipt/physical delta does not complete this grant");
            witness.requireUnchanged();
            fence.requireDrained(seal);

            // The ledger independently validates the exact persistent grant
            // handle. No caller-supplied stale receipt number is used.
            if (!ledger.pending(player).contains(grant))
                throw new IllegalStateException("Reservation changed before durable ACK");
            // Retain the SAME monitor against concurrent invalidation through
            // the ledger's actual atomic, fsynced ACK persistence. This is
            // intentionally a short protected durable commit, not a file scan.
            SavedGrantDeliveryReadback.Proof committed =
                    fence.withDrainedSeal(seal, () -> {
                        try {
                            ledger.acknowledge(grant, fresh.receipt().completedSequence());
                            return fresh;
                        } catch (IOException persistenceFailure) {
                            throw new java.io.UncheckedIOException(persistenceFailure);
                        }
                    });
            fence.requireDrained(seal);
            return committed;
            });
        } catch (java.io.UncheckedIOException wrapped) {
            fence.invalidate();
            throw wrapped.getCause();
        } catch (IOException | RuntimeException | Error failure) {
            // No stale seal may ever be reused after a partial file or journal
            // operation. A failure here may still mean durable ACK succeeded:
            // the caller MUST cold-reopen and reconcile rather than retry give.
            fence.invalidate();
            throw failure;
        }
    }
}
