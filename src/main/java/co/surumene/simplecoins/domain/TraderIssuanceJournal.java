package co.surumene.simplecoins.domain;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Durable single-writer death-event decisions. Never claim that a Paper
 * event's mutated drop list and the saved world are one atomic transaction.
 * Each positive receipt remains UNRECONCILED until authoritative world/ledger
 * scanning has separately established the actual physical outcome.
 */
public final class TraderIssuanceJournal implements AutoCloseable {
    private static final int MAGIC = 0x53435431; // SCT1
    private static final int RECORD_MAX_BYTES = 128;
    public enum Status { NO_DROP, PREPARED, EVENT_RECORDED }

    public record Receipt(UUID traderId, CoinDenomination denomination, int count, Status status) {
        public Receipt {
            Objects.requireNonNull(traderId, "traderId");
            Objects.requireNonNull(status, "status");
            if (status == Status.NO_DROP) {
                if (denomination != null || count != 0)
                    throw new IllegalArgumentException("No-drop record cannot contain money");
            } else if (denomination == null || count < 1)
                throw new IllegalArgumentException("Issued record needs positive coins");
        }
        public long value() { return denomination == null ? 0 : (long) denomination.value() * count; }
        public boolean needsReconciliation() { return status != Status.NO_DROP; }
    }

    private final Path directory;
    private final FileChannel lockChannel;
    private final FileLock singleWriterLock;
    private final Map<UUID, Receipt> records = new HashMap<>();
    private boolean unhealthy;

    public TraderIssuanceJournal(Path directory) throws IOException {
        this.directory = Objects.requireNonNull(directory, "directory").toAbsolutePath().normalize();
        Files.createDirectories(this.directory);
        FileChannel channel = FileChannel.open(this.directory.resolve(".single-writer"),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        FileLock lock;
        try {
            try { lock = channel.tryLock(); }
            catch (OverlappingFileLockException busy) { throw new IOException("Trader journal already open", busy); }
            if (lock == null) throw new IOException("Trader journal single-writer lock unavailable");
        } catch (IOException | RuntimeException error) {
            channel.close();
            throw error;
        }
        this.lockChannel = channel;
        this.singleWriterLock = lock;
        try {
            try (var files = Files.list(this.directory)) {
                for (Path path : files.filter(p -> p.getFileName().toString().endsWith(".trader")).toList()) {
                    Receipt entry = decode(DurableFile.read(path, RECORD_MAX_BYTES));
                    if (!path.getFileName().toString().equals(entry.traderId() + ".trader")
                            || records.putIfAbsent(entry.traderId(), entry) != null)
                        throw new IOException("Misnamed or duplicate trader receipt: " + path);
                }
            }
        } catch (IOException | RuntimeException error) {
            close();
            throw error;
        }
    }

    public synchronized boolean has(UUID traderId) {
        healthy();
        return records.containsKey(Objects.requireNonNull(traderId, "traderId"));
    }

    public synchronized Optional<Receipt> receipt(UUID traderId) {
        healthy();
        return Optional.ofNullable(records.get(Objects.requireNonNull(traderId)));
    }

    /**
     * Record *every* death decision (including no drop) before publishing money.
     * A duplicate UUID returns false without rerolling or rewriting its receipt.
     */
    public synchronized boolean begin(UUID traderId, Optional<WanderingTraderDropPolicy.Drop> decision)
            throws IOException {
        healthy();
        Objects.requireNonNull(traderId, "traderId");
        Objects.requireNonNull(decision, "decision");
        if (records.containsKey(traderId)) return false;
        Receipt entry = decision.map(d -> new Receipt(traderId, d.denomination(), d.count(),
                        Status.PREPARED))
                .orElseGet(() -> new Receipt(traderId, null, 0, Status.NO_DROP));
        try { DurableFile.writeNew(path(traderId), encode(entry)); }
        catch (IOException failure) { unhealthy = true; throw failure; }
        records.put(traderId, entry);
        return true;
    }

    /**
     * Records addition to the in-memory death-event list, NOT persistence of
     * spawned item entities to world storage. Reconciliation is still required.
     */
    public synchronized void markEventRecorded(UUID traderId) throws IOException {
        healthy();
        Receipt original = records.get(Objects.requireNonNull(traderId));
        if (original == null || original.status() == Status.NO_DROP)
            throw new IllegalStateException("No coin issuance reservation exists");
        if (original.status() == Status.EVENT_RECORDED) return;
        Receipt updated = new Receipt(traderId, original.denomination(), original.count(),
                Status.EVENT_RECORDED);
        try { DurableFile.write(path(traderId), encode(updated)); }
        catch (IOException failure) { unhealthy = true; throw failure; }
        records.put(traderId, updated);
    }

    /** All potentially published coins require startup physical/ledger review. */
    public synchronized List<Receipt> unresolved() {
        healthy();
        return records.values().stream().filter(Receipt::needsReconciliation)
                .sorted(java.util.Comparator.comparing(r -> r.traderId().toString())).toList();
    }

    private Path path(UUID id) { return directory.resolve(id + ".trader"); }
    private void healthy() {
        if (unhealthy || !singleWriterLock.isValid())
            throw new IllegalStateException("Trader issuance journal is unavailable; stop issuing");
    }

    private static byte[] encode(Receipt entry) {
        try {
            var bytes = new ByteArrayOutputStream();
            try (var out = new DataOutputStream(bytes)) {
                out.writeInt(MAGIC);
                out.writeUTF(entry.traderId().toString());
                out.writeByte(entry.status().ordinal());
                out.writeInt(entry.denomination() == null ? 0 : entry.denomination().value());
                out.writeInt(entry.count());
            }
            return bytes.toByteArray();
        } catch (IOException impossible) { throw new AssertionError(impossible); }
    }

    private static Receipt decode(byte[] bytes) throws IOException {
        try (var in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            if (in.readInt() != MAGIC) throw new IOException("Invalid trader receipt schema");
            String text = in.readUTF();
            UUID id = UUID.fromString(text);
            if (!id.toString().equals(text)) throw new IOException("Noncanonical trader UUID");
            int statusNumber = in.readUnsignedByte();
            if (statusNumber >= Status.values().length) throw new IOException("Unknown trader receipt status");
            int faceValue = in.readInt();
            int count = in.readInt();
            if (in.available() != 0) throw new IOException("Trailing trader receipt bytes");
            CoinDenomination denomination = faceValue == 0 ? null : CoinDenomination.fromValue(faceValue);
            return new Receipt(id, denomination, count, Status.values()[statusNumber]);
        } catch (IllegalArgumentException | IllegalStateException malformed) {
            throw new IOException("Invalid trader receipt data", malformed);
        }
    }

    @Override public void close() throws IOException {
        unhealthy = true;
        try { singleWriterLock.release(); } finally { lockChannel.close(); }
    }
}
