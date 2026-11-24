package co.surumene.simplecoins.domain;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** Single-writer issuance journal. A reservation is not a physical asset. */
public final class GrantLedger {
    public record Grant(long sequence, UUID id, UUID player, long amount, String issuer,
                        long createdMillis, boolean delivered,
                        CoinDenomination denomination, long coinCount, long confirmedPartial) {
        public Grant(long sequence, UUID id, UUID player, long amount, String issuer,
                     long createdMillis, boolean delivered, CoinDenomination denomination, long coinCount) {
            this(sequence, id, player, amount, issuer, createdMillis, delivered, denomination, coinCount, 0);
        }
        public Grant {
            Objects.requireNonNull(id); Objects.requireNonNull(player); Objects.requireNonNull(issuer);
            if (sequence <= 0 || amount <= 0 || issuer.isBlank() || issuer.length() > 1024
                    || confirmedPartial < 0 || confirmedPartial >= amount || (delivered && confirmedPartial != 0))
                throw new IllegalArgumentException("Invalid grant");
            if (denomination == null) {
                if (coinCount != 0) throw new IllegalArgumentException("Unspecified denomination has a count");
            } else if (coinCount <= 0 || Math.multiplyExact(coinCount, denomination.value()) != amount) {
                throw new IllegalArgumentException("Coin reservation has an inconsistent amount");
            }
        }
        Grant completed() { return new Grant(sequence, id, player, amount, issuer, createdMillis,
                true, denomination, coinCount); }
        Grant progressed(long value) { return new Grant(sequence, id, player, amount, issuer, createdMillis,
                false, denomination, coinCount, value); }
    }
    private final Path directory;
    private final TreeMap<Long, Grant> grants = new TreeMap<>();
    private volatile boolean failed;
    private volatile long mutationRevision;

    public GrantLedger(Path directory) throws IOException {
        this.directory = directory.toAbsolutePath().normalize();
        directory = this.directory;
        Files.createDirectories(directory);
        try (var listing = Files.list(directory)) {
            for (Path file : listing.filter(p -> p.getFileName().toString().endsWith(".grant")).toList()) {
                Grant grant = decode(DurableFile.read(file, 8192));
                if (!file.getFileName().toString().equals(grant.sequence() + ".grant") ||
                        grants.putIfAbsent(grant.sequence(), grant) != null)
                    throw new IOException("Duplicate/misnamed grant " + file);
            }
        }
        long expected = 1;
        Set<UUID> ids = new HashSet<>();
        for (Grant grant : grants.values()) {
            if (grant.sequence() != expected++ || !ids.add(grant.id()))
                throw new IOException("Missing or duplicate issuance journal record");
        }
        long last = grants.isEmpty() ? 0 : grants.lastKey();
        Path head = directory.resolve("head");
        if (Files.exists(head)) {
            byte[] bytes = DurableFile.read(head, 8);
            if (bytes.length != 8) throw new IOException("Invalid journal head");
            long expectedLast = java.nio.ByteBuffer.wrap(bytes).getLong();
            if (expectedLast < 0 || expectedLast > last)
                throw new IOException("Issuance journal tail is missing");
            // A reservation is written before its head. Recover a crash between them.
            if (expectedLast != last) writeHead(last);
        } else {
            if (last != 0) throw new IOException("Issuance journal head is missing");
            writeHead(0);
        }
    }
    private void writeHead(long sequence) throws IOException {
        DurableFile.write(directory.resolve("head"), java.nio.ByteBuffer.allocate(8).putLong(sequence).array());
    }
    public synchronized Grant reserve(UUID player, long amount, String issuer) throws IOException {
        return reserveTyped(player, amount, issuer, null, 0);
    }

    /** Reserve exactly the chosen physical denomination, not its minimum-coin equivalent. */
    public synchronized Grant reserveCoins(UUID player, CoinDenomination denomination,
                                          long count, String issuer) throws IOException {
        Objects.requireNonNull(denomination);
        if (count <= 0) throw new IllegalArgumentException("Coin count must be positive");
        return reserveTyped(player, Math.multiplyExact(count, denomination.value()),
                issuer, denomination, count);
    }

    private Grant reserveTyped(UUID player, long amount, String issuer,
                               CoinDenomination denomination, long coinCount) throws IOException {
        checkHealthy();
        long next = grants.isEmpty() ? 1 : Math.addExact(grants.lastKey(), 1);
        Grant grant = new Grant(next, UUID.randomUUID(), player, amount, issuer,
                Instant.now().toEpochMilli(), false, denomination, coinCount);
        advanceMutationRevision();
        persistNew(grant);
        try { writeHead(next); }
        catch (IOException failure) { failed = true; throw failure; }
        grants.put(next, grant);
        return grant;
    }
    /**
     * Immutable all-subject view of the currently verified issuance evidence.
     * A successful view alone is NOT proof that saved playerdata survived.
     */
    public synchronized List<Grant> historySnapshot() {
        checkHealthy();
        return List.copyOf(grants.values());
    }

    public synchronized List<Grant> pending(UUID player) {
        checkHealthy();
        return grants.values().stream().filter(g -> g.player().equals(player) && !g.delivered()).toList();
    }
    /**
     * One coherent, immutable, player-scoped read-only planning view. Capture
     * verifies ALL durable records, not only this player's records. Reusing it
     * within a logical planning operation avoids repeating the full disk scan
     * for each receipt predicate. It is deliberately not a current-world
     * witness or an ACK/write capability; all actual ledger mutations still
     * perform their existing independent durable checks.
     */
    public synchronized VerifiedPlayerSnapshot verifiedPlayerSnapshot(UUID player) {
        Objects.requireNonNull(player, "player UUID");
        checkHealthy();
        return new VerifiedPlayerSnapshot(this, mutationRevision, player, grants.values().stream()
                .filter(g -> g.player().equals(player)).toList());
    }

    /** One authenticated, immutable ledger revision for a read-only multi-player audit. */
    public synchronized VerifiedUniverseSnapshot verifiedUniverseSnapshot() {
        checkHealthy();
        return new VerifiedUniverseSnapshot(this, mutationRevision, List.copyOf(grants.values()));
    }

    /** Recheck ALL durable records at the end; a stale or foreign view is not evidence. */
    public synchronized void requireUnchangedVerifiedUniverse(VerifiedUniverseSnapshot view) {
        if (view == null || view.owner != this || view.revision != mutationRevision)
            throw new IllegalStateException("Issuance history changed during saved audit");
        checkHealthy();
    }

    public static final class VerifiedUniverseSnapshot {
        private final GrantLedger owner;
        private final long revision;
        private final List<Grant> history;
        private final Map<UUID, VerifiedPlayerSnapshot> players;

        private VerifiedUniverseSnapshot(GrantLedger owner, long revision, List<Grant> history) {
            this.owner = owner;
            this.revision = revision;
            this.history = List.copyOf(history);
            Map<UUID, List<Grant>> grouped = new HashMap<>();
            for (Grant grant : history)
                grouped.computeIfAbsent(grant.player(), ignored -> new ArrayList<>()).add(grant);
            Map<UUID, VerifiedPlayerSnapshot> views = new HashMap<>();
            grouped.forEach((player, records) -> views.put(player,
                    new VerifiedPlayerSnapshot(owner, revision, player, records)));
            this.players = Map.copyOf(views);
        }

        public List<Grant> history() { return history; }
        public VerifiedPlayerSnapshot forPlayer(UUID player) {
            Objects.requireNonNull(player, "player UUID");
            var view = players.get(player);
            return view != null ? view : new VerifiedPlayerSnapshot(owner, revision, player, List.of());
        }
    }

    public static final class VerifiedPlayerSnapshot {
        private final GrantLedger owner;
        private final long revision;
        private final UUID player;
        private final List<Grant> history;
        private final List<Grant> pending;

        private VerifiedPlayerSnapshot(GrantLedger owner, long revision, UUID player, List<Grant> history) {
            this.owner = owner;
            this.revision = revision;
            this.player = player;
            this.history = List.copyOf(history);
            this.pending = this.history.stream().filter(g -> !g.delivered()).toList();
        }

        public UUID player() { return player; }
        public List<Grant> pending() { return pending; }
        public void validateReceipt(long receipt) {
            validatePlayerReceipt(history, receipt);
        }
    }

    /**
     * A playerdata receipt refers to a durable reservation, not an arbitrary
     * caller-supplied Grant with a plausible owner and sequence. Its immutable
     * attributes must match our journal before it can be treated as applied.
     *
     * The delivered flag is deliberately excluded: the same reservation may
     * have been acknowledged since the caller obtained its immutable handle.
     */
    private Grant requireAuthenticReservation(Grant handle) {
        Objects.requireNonNull(handle, "grant handle");
        Grant stored = grants.get(handle.sequence());
        if (stored == null
                || !stored.id().equals(handle.id())
                || !stored.player().equals(handle.player())
                || stored.amount() != handle.amount()
                || !stored.issuer().equals(handle.issuer())
                || stored.createdMillis() != handle.createdMillis()
                || stored.denomination() != handle.denomination()
                || stored.coinCount() != handle.coinCount())
            throw new IllegalStateException("Grant handle disagrees with durable reservation");
        return stored;
    }

    public synchronized boolean wasApplied(Grant grant, long savedReceipt) {
        checkHealthy();
        Grant stored = requireAuthenticReservation(grant);
        validateReceipt(stored.player(), savedReceipt);
        return savedReceipt >= stored.sequence();
    }
    public synchronized void validateReceipt(UUID player, long receipt) {
        checkHealthy();
        validatePlayerReceipt(grants.values().stream()
                .filter(g -> g.player().equals(player)).toList(), receipt);
    }

    /** Both fresh durable validation and immutable read-only views use the same predicates. */
    private static void validatePlayerReceipt(List<Grant> history, long receipt) {
        if (receipt < 0 || (receipt != 0 && history.stream().noneMatch(g -> g.sequence() == receipt)))
            throw new IllegalStateException("Receipt does not belong to this player's journal");
        for (Grant g : history) {
            if (g.delivered() && g.sequence() > receipt)
                throw new IllegalStateException("Playerdata receipt rolled back behind acknowledged grant");
            // An undelivered earlier grant cannot be skipped by a later receipt.
            // The grant at 'receipt' may be awaiting acknowledgement after a crash.
            if (!g.delivered() && g.sequence() < receipt)
                throw new IllegalStateException("Playerdata receipt skipped an undelivered grant");
        }
    }
    public synchronized void acknowledge(Grant grant, long savedReceipt) throws IOException {
        checkHealthy();
        Grant current = requireAuthenticReservation(grant);
        if (!wasApplied(current, savedReceipt))
            throw new IllegalStateException("Cannot acknowledge an unapplied grant");
        if (current.delivered()) return;
        Grant complete = current.completed();
        advanceMutationRevision();
        persist(complete);
        grants.put(complete.sequence(), complete);
    }
    /**
     * Reconcile the cumulative receipt read from the target's actual playerdata.
     * Caller must have observed the matching saved inventory/receipt generation
     * (or a valid persisted receipt on recovery). No whole-inventory intent or
     * server-wide writer seal is required for this normal per-player path.
     */
    public synchronized void confirmSavedProgress(Grant grant, GrantReceiptState receipt) throws IOException {
        Objects.requireNonNull(receipt);
        checkHealthy();
        Grant current = requireAuthenticReservation(grant);
        receipt.verify(verifiedPlayerSnapshot(current.player()));
        if (receipt.completedSequence() == current.sequence()) {
            acknowledge(current, receipt.completedSequence());
            return;
        }
        if (current.delivered() || receipt.activeSequence() != current.sequence()
                || !current.id().equals(receipt.activeId())
                || receipt.partialValue() < current.confirmedPartial()
                || receipt.partialValue() >= current.amount())
            throw new IllegalStateException("Saved receipt does not confirm this reservation");
        if (receipt.partialValue() == current.confirmedPartial()) return;
        var progressed = current.progressed(receipt.partialValue());
        advanceMutationRevision();
        persist(progressed);
        grants.put(progressed.sequence(), progressed);
    }

    /** Persist only a physically verified, strictly advancing partial batch.
     * Caller owns the real saved-file proof and uninterrupted writer seal.
     */
    public synchronized void confirmPartial(Grant grant, GrantReceiptState prior,
            GrantReceiptState next) throws IOException {
        Objects.requireNonNull(prior); Objects.requireNonNull(next);
        var view = verifiedPlayerSnapshot(grant.player());
        requirePendingIntentView(view, grant);
        prior.verify(view); next.verify(view);
        long floor = grant.confirmedPartial();
        if (prior.partialValue() != floor || (floor > 0 && !prior.active())
                || (prior.active() && (prior.activeSequence() != grant.sequence() || !grant.id().equals(prior.activeId())))
                || !next.active() || next.activeSequence() != grant.sequence() || !grant.id().equals(next.activeId())
                || prior.completedSequence() != next.completedSequence() || next.partialValue() <= floor
                || next.partialValue() >= grant.amount())
            throw new IllegalStateException("Invalid verified partial progress transition");
        // The cursor alone is not a physical proof. Require the prepared batch
        // boundary and the caller's independently verified actual save.
        var intent = decodeIntent(intentPath(grant), grant, null, MAX_INTENT_COMPRESSED);
        if (intent.priorValue() != floor)
            throw new IOException("Prepared intent differs from confirmed partial floor");
        Grant progressed = grant.progressed(next.partialValue());
        advanceMutationRevision();
        persist(progressed);
        grants.put(progressed.sequence(), progressed);
    }

    /**
     * Nonblocking in-process freshness check for a sealed main-thread handoff.
     * Not a disk proof: the caller MUST exclude all durable writers for the
     * entire protected interval. Actual ACK still verifies the durable journal.
     */
    public void requireCurrentPlanningView(VerifiedPlayerSnapshot view) {
        if (view == null || view.owner != this || failed || view.revision != mutationRevision)
            throw new IllegalStateException("Issuance planning view is stale or foreign");
    }

    private static final int FIRST_BATCH_MAGIC = 0x53434931; // SCI1
    private static final int BATCH_MAGIC = 0x53434932; // SCI2
    private static final int MAX_INTENT_COMPRESSED = 16 * 1024 * 1024;
    private static final int INTENT_OVERHEAD = 16 * 1024;

    /** Immutable prior image bound to the previously confirmed cumulative value. */
    public record BatchIntent(long priorValue, byte[] compressedBefore) {
        public BatchIntent {
            if (priorValue < 0) throw new IllegalArgumentException("Negative prior progress");
            compressedBefore = Objects.requireNonNull(compressedBefore).clone();
        }
        @Override public byte[] compressedBefore() { return compressedBefore.clone(); }
    }

    public synchronized Optional<byte[]> readFirstBatchIntent(VerifiedPlayerSnapshot view,
            Grant grant, Path source, int maximumBytes) throws IOException {
        if (grant.confirmedPartial() != 0) throw new IllegalStateException("Not first batch");
        return readBatchIntent(view, grant, source, maximumBytes).map(BatchIntent::compressedBefore);
    }

    public synchronized Optional<BatchIntent> readBatchIntent(VerifiedPlayerSnapshot view,
            Grant grant, Path source, int maximumBytes) throws IOException {
        requirePendingIntentView(view, grant);
        String identity = intentSource(grant, source);
        requireIntentBudget(maximumBytes);
        Path target = intentPath(grant);
        if (Files.notExists(target, LinkOption.NOFOLLOW_LINKS)) return Optional.empty();
        var intent = decodeIntent(target, grant, identity, maximumBytes);
        if (intent.priorValue() > grant.confirmedPartial())
            throw new IOException("Batch intent ahead of durable confirmed progress");
        return Optional.of(intent);
    }

    public synchronized void writeFirstBatchIntent(VerifiedPlayerSnapshot view,
            Grant grant, Path source, byte[] compressedBefore, int maximumBytes) throws IOException {
        if (grant.confirmedPartial() != 0) throw new IllegalStateException("Not first batch");
        writeBatchIntent(view, grant, source, compressedBefore, maximumBytes);
    }

    public synchronized void writeBatchIntent(VerifiedPlayerSnapshot view,
            Grant grant, Path source, byte[] compressedBefore, int maximumBytes) throws IOException {
        requirePendingIntentView(view, grant);
        String identity = intentSource(grant, source);
        requireIntentBudget(maximumBytes);
        if (compressedBefore == null || compressedBefore.length == 0 || compressedBefore.length > maximumBytes)
            throw new IOException("Prior playerdata exceeds batch intent budget");
        byte[] reservation = encode(grant.progressed(0));
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(buffer)) {
            out.writeInt(BATCH_MAGIC);
            out.writeInt(reservation.length); out.write(reservation);
            out.writeUTF(identity); out.writeLong(grant.confirmedPartial());
            out.writeInt(compressedBefore.length); out.write(compressedBefore);
        }
        DurableFile.writeNew(intentPath(grant), buffer.toByteArray());
        requireCurrentPlanningView(view);
    }

    /** The durable partial floor, not a saved cursor alone, authorizes removal. */
    public synchronized void removeConfirmedBatchIntent(Grant grant, Path source, long priorValue) throws IOException {
        checkHealthy();
        Grant current = requireAuthenticReservation(grant);
        if (current.delivered() || priorValue < 0 || current.confirmedPartial() <= priorValue)
            throw new IllegalStateException("Cannot remove an unconfirmed batch intent");
        Path target = intentPath(grant);
        if (Files.notExists(target, LinkOption.NOFOLLOW_LINKS)) return;
        var intent = decodeIntent(target, grant, intentSource(grant, source), MAX_INTENT_COMPRESSED);
        if (intent.priorValue() != priorValue) throw new IOException("Different prepared batch intent");
        removeIntent(target);
    }

    /** Cleanup is legal only after genuine durable ACK; a leftover is harmless. */
    public synchronized void removeAcknowledgedFirstBatchIntent(Grant grant) throws IOException {
        checkHealthy();
        Grant current = requireAuthenticReservation(grant);
        if (!current.delivered() || grant.delivered())
            throw new IllegalStateException("Cannot remove pending or foreign grant intent");
        Path target = intentPath(grant);
        if (Files.notExists(target, LinkOption.NOFOLLOW_LINKS)) return;
        decodeIntent(target, grant, null, MAX_INTENT_COMPRESSED);
        removeIntent(target);
    }

    private static void removeIntent(Path target) throws IOException {
        Files.delete(target);
        try (var parent = java.nio.channels.FileChannel.open(target.getParent(), StandardOpenOption.READ)) {
            parent.force(true);
        }
    }

    private void requirePendingIntentView(VerifiedPlayerSnapshot view, Grant grant) {
        requireCurrentPlanningView(view);
        if (!view.pending().contains(grant) || !requireAuthenticReservation(grant).equals(grant))
            throw new IllegalStateException("Intent does not identify an authentic pending grant");
    }

    private Path intentPath(Grant grant) {
        return directory.resolve(grant.sequence() + ".intent");
    }

    private static String intentSource(Grant grant, Path source) {
        Path canonical = Objects.requireNonNull(source, "saved playerdata path").toAbsolutePath().normalize();
        String identity = canonical.toString();
        if (!canonical.getFileName().toString().equals(grant.player() + ".dat") || identity.length() > 4096)
            throw new IllegalArgumentException("Invalid canonical intent source identity");
        return identity;
    }

    private static void requireIntentBudget(int maximumBytes) {
        if (maximumBytes < 1 || maximumBytes > MAX_INTENT_COMPRESSED)
            throw new IllegalArgumentException("Invalid first-batch intent byte budget");
    }

    private static BatchIntent decodeIntent(Path target, Grant grant, String expectedSource,
            int maximumBytes) throws IOException {
        byte[] payload = DurableFile.read(target, Math.addExact(maximumBytes, INTENT_OVERHEAD));
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload))) {
            int schema = in.readInt();
            if (schema != FIRST_BATCH_MAGIC && schema != BATCH_MAGIC) throw new IOException("Unknown batch intent schema");
            int reservationSize = in.readInt();
            if (reservationSize < 1 || reservationSize > 8192
                    || !Arrays.equals(in.readNBytes(reservationSize), encode(grant.progressed(0))))
                throw new IOException("First-batch intent reservation identity mismatch");
            String source = in.readUTF();
            if (!source.equals(intentSource(grant, Path.of(source)))
                    || (expectedSource != null && !source.equals(expectedSource)))
                throw new IOException("First-batch intent source identity mismatch");
            long prior = schema == BATCH_MAGIC ? in.readLong() : 0;
            if (prior < 0 || prior >= grant.amount()) throw new IOException("Invalid batch intent progress");
            GrantDeliveryBatchPlanner.next(grant.progressed(0), prior, 0);
            int size = in.readInt();
            if (size < 1 || size > maximumBytes || size != in.available())
                throw new IOException("Invalid first-batch prior-state size");
            return new BatchIntent(prior, in.readNBytes(size));
        } catch (IllegalArgumentException invalid) {
            throw new IOException("Malformed first-batch intent", invalid);
        }
    }

    private void advanceMutationRevision() {
        try { mutationRevision = Math.incrementExact(mutationRevision); }
        catch (ArithmeticException exhausted) { failed = true; throw exhausted; }
    }

    private void checkHealthy() {
        if (failed)
            throw new IllegalStateException("Issuance journal failed; reload from disk before continuing");
        try {
            // Do not trust a valid tail if an older persisted reservation has
            // vanished or changed. This is a correctness-first full scan,
            // intentionally O(n) durable reads per operation until a safe
            // compact/indexed journal protocol is designed and verified.
            byte[] head = DurableFile.read(directory.resolve("head"), 8);
            long last = grants.isEmpty() ? 0 : grants.lastKey();
            if (head.length != 8 || java.nio.ByteBuffer.wrap(head).getLong() != last)
                throw new IOException("Durable issuance head differs from in-memory journal");
            Set<String> knownRecords = new HashSet<>();
            for (Grant expected : grants.values()) {
                Grant persisted = decode(DurableFile.read(
                        directory.resolve(expected.sequence() + ".grant"), 8192));
                if (!expected.equals(persisted))
                    throw new IOException("Durable issuance record disagrees at sequence "
                            + expected.sequence());
                knownRecords.add(expected.sequence() + ".grant");
            }
            // A valid head and matching known records do not prove that there
            // are no additional durable reservations. Detect unexpected files
            // before permitting another read, acknowledgement or issuance.
            try (var listing = Files.list(directory)) {
                for (Path entry : listing.toList()) {
                    String name = entry.getFileName().toString();
                    if (name.endsWith(".grant") && !knownRecords.contains(name))
                        throw new IOException("Untracked issuance journal record: " + entry);
                }
            }
        } catch (IOException | RuntimeException corruption) {
            failed = true;
            throw new IllegalStateException(
                    "Issuance journal failed integrity verification; deny further operations",
                    corruption);
        }
    }
    private void persist(Grant grant) throws IOException {
        try { DurableFile.write(directory.resolve(grant.sequence() + ".grant"), encode(grant)); }
        catch (IOException ex) { failed = true; throw ex; }
    }

    private void persistNew(Grant grant) throws IOException {
        try {
            DurableFile.writeNew(directory.resolve(grant.sequence() + ".grant"), encode(grant));
        } catch (IOException ex) {
            failed = true;
            throw ex;
        }
    }
    private static byte[] encode(Grant grant) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(grant.confirmedPartial() == 0 ? 0x53434732 : 0x53434733); out.writeLong(grant.sequence());
            out.writeUTF(grant.id().toString()); out.writeUTF(grant.player().toString());
            out.writeLong(grant.amount()); out.writeUTF(grant.issuer());
            out.writeLong(grant.createdMillis()); out.writeBoolean(grant.delivered());
            out.writeInt(grant.denomination() == null ? 0 : grant.denomination().value());
            out.writeLong(grant.coinCount());
            if (grant.confirmedPartial() != 0) out.writeLong(grant.confirmedPartial());
        }
        return bytes.toByteArray();
    }
    private static Grant decode(byte[] bytes) throws IOException {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            int schema = in.readInt();
            if (schema != 0x53434731 && schema != 0x53434732 && schema != 0x53434733)
                throw new IOException("Unknown issuance journal schema");
            long sequence = in.readLong();
            String rawId = in.readUTF(), rawPlayer = in.readUTF();
            UUID id = UUID.fromString(rawId), player = UUID.fromString(rawPlayer);
            // UUID.fromString accepts abbreviated and case-aliased spellings.
            // Monetary journal identities must replay exactly as encoded.
            if (!id.toString().equals(rawId) || !player.toString().equals(rawPlayer))
                throw new IOException("Noncanonical issuance journal UUID");
            long amount = in.readLong();
            String issuer = in.readUTF();
            long createdMillis = in.readLong();
            boolean delivered = in.readBoolean();
            int denominationValue = schema != 0x53434731 ? in.readInt() : 0;
            long coinCount = schema != 0x53434731 ? in.readLong() : 0;
            long progress = schema == 0x53434733 ? in.readLong() : 0;
            CoinDenomination denomination = denominationValue == 0 ? null :
                    CoinDenomination.fromValue(denominationValue);
            Grant result = new Grant(sequence, id, player, amount, issuer,
                    createdMillis, delivered, denomination, coinCount, progress);
            if (progress > 0) GrantDeliveryBatchPlanner.next(result, progress, 0);
            if (in.available() != 0) throw new IOException("Trailing journal data");
            return result;
        } catch (IllegalArgumentException ex) { throw new IOException("Invalid journal", ex); }
    }
}
