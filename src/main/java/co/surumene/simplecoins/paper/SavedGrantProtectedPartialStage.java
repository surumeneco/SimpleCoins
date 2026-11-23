package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.GrantLedger;
import co.surumene.simplecoins.domain.GrantReceiptState;
import co.surumene.simplecoins.domain.WriterEpochFence;
import co.surumene.simplecoins.domain.WorldStorageDiscovery;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Internal saved-playerdata validation for one authentic PARTIAL grant batch.
 *
 * The only successful result is a verified active PDC partial cursor and the
 * physical coins newly saved with it. The complete grant stays pending in
 * GrantLedger and this component NEVER acknowledges an issuance.
 *
 * Current Agent/Plugin/native/async/direct-file writer coverage is not
 * exhaustive. Consequently this is deliberately package-private and
 * unregistered: callers cannot use a limited-family WriterEpochFence as
 * permission to expose offline delivery or reopen player admission.
 */
final class SavedGrantProtectedPartialStage {
    private SavedGrantProtectedPartialStage() {}

    /**
     * Validate a partial real UUID.dat transition under one uninterrupted,
     * previously drained writer seal, with durable file/dir force before
     * re-reading the physical coins and PDC receipt from the SAME disk root.
     *
     * The separate caller owns the actual Paper inventory/PDC save, blocking
     * physical interaction until verification, prior partial-history custody,
     * all-writer coverage, journal reconciliation and future continuation.
     */
    static synchronized SavedGrantDeliveryReadback.Proof verifyPartial(
            WriterEpochFence fence, WriterEpochFence.Seal seal,
            WorldStorageDiscovery.Manifest manifest, GrantLedger ledger,
            UUID player, GrantLedger.Grant grant, PaperGrantInventorySlotPlan plan,
            Map<String, Object> savedBefore,
            int maximumCompressedBytes, int maximumInflatedBytes) throws IOException {
        Objects.requireNonNull(fence, "shared writer fence");
        try {
            Objects.requireNonNull(manifest, "authentic playerdata discovery");
            Objects.requireNonNull(ledger, "durable issuance journal");
            Objects.requireNonNull(player, "player UUID");
            Objects.requireNonNull(grant, "durable reservation handle");
            Objects.requireNonNull(plan, "canonical partial inventory placement");
            Objects.requireNonNull(savedBefore, "prior saved playerdata root");
            if (maximumCompressedBytes < 1 || maximumInflatedBytes < 1)
                throw new IllegalArgumentException("Invalid saved source byte limits");

            GrantReceiptState prior = plan.previousReceipt();
            GrantReceiptState partial = plan.nextReceipt();
            long priorProgress = prior.active() ? prior.partialValue() : 0;
            if (grant.delivered()
                    || !player.equals(grant.player())
                    || !ledger.pending(player).contains(grant)
                    || plan.placements().isEmpty()
                    || !partial.active()
                    || partial.activeSequence() != grant.sequence()
                    || !grant.id().equals(partial.activeId())
                    || (prior.active() && (prior.activeSequence() != grant.sequence()
                            || !grant.id().equals(prior.activeId())))
                    || prior.completedSequence() != partial.completedSequence()
                    || partial.partialValue() <= priorProgress
                    || partial.partialValue() >= grant.amount())
                throw new IllegalStateException(
                        "Not an authentic, advancing, unacknowledged partial grant batch");

            long expectedDelta = Math.subtractExact(partial.partialValue(), priorProgress);
            return fence.withDrainedLease(seal, () -> {
                fence.requireDrained(seal);
                // The root discovery and UUID filename must be real and stable.
                SavedPlayerdataContentWitness witness =
                        SavedPlayerdataContentWitness.capturePlayer(
                                manifest, player, maximumCompressedBytes);
                Path file = manifest.playerDataFiles().get(player);
                if (file == null || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS))
                    throw new IOException("Missing original saved playerdata source");

                // Paper's void saveData return is NOT a persistence receipt.
                // Force the actual file and its directory before fresh decode.
                try (FileChannel channel = FileChannel.open(file,
                        StandardOpenOption.READ, StandardOpenOption.WRITE,
                        LinkOption.NOFOLLOW_LINKS)) {
                    channel.force(true);
                }
                Path directory = file.getParent();
                if (directory == null || !Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS))
                    throw new IOException("Missing real saved playerdata parent directory");
                try (FileChannel channel = FileChannel.open(directory,
                        StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
                    channel.force(true);
                }
                witness.requireUnchanged();
                fence.requireDrained(seal);

                SavedGrantDeliveryReadback.Proof fresh =
                        SavedGrantDeliveryFileReadback.verify(
                                manifest, ledger, player, plan, savedBefore,
                                maximumCompressedBytes, maximumInflatedBytes);
                if (!partial.equals(fresh.receipt()) || !fresh.receipt().active()
                        || !BigInteger.valueOf(expectedDelta)
                                .equals(fresh.matchedMonetaryValue()))
                    throw new IOException(
                            "Fresh saved partial receipt lacks the precise newly issued value");
                witness.requireUnchanged();
                fence.requireDrained(seal);
                // A partial cursor is NOT a successful whole-grant issuance.
                // Any other state, including a concurrent ACK, fails closed.
                if (!ledger.pending(player).contains(grant))
                    throw new IOException("Issuance journal changed during partial save proof");
                return fresh; // NO ledger.acknowledge, NO journal event, NO permit
            });
        } catch (IOException | RuntimeException | Error failure) {
            // Even with valid receipt, abort and recover from disk if this
            // partial proof failed. Do not silently give the coins again.
            fence.invalidate();
            throw failure;
        }
    }
}
