package co.surumene.simplecoins.domain;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.DirectoryStream;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Durable, single-writer record of currency movements, NOT an accounting
 * snapshot or a permission to accept logins. Every prepared transaction,
 * including an event marked as applied, requires authoritative physical
 * reconciliation after restart; journal state is never a source of money.
 *
 * This is an independent domain candidate. The Paper event finality,
 * source selection and atomic snapshot integration are intentionally absent.
 */
public final class NormalMutationJournal implements AutoCloseable {
    private static final int MAGIC = 0x53434e31; // SCN1
    private static final int MAX_RECORD_BYTES = 512;

    public enum AccountKind { PLAYER, FREE, DEAD, EXTERNAL }
    public enum Phase { PREPARED, EVENT_APPLIED }

    /** The EXTERNAL account denotes supply/destroy, never a player asset. */
    public record Account(AccountKind kind, UUID player) {
        public Account {
            Objects.requireNonNull(kind, "account kind");
            if ((kind == AccountKind.PLAYER || kind == AccountKind.DEAD) != (player != null))
                throw new IllegalArgumentException("Account owner must match kind");
        }
        public static Account player(UUID id) { return new Account(AccountKind.PLAYER, id); }
        public static Account dead(UUID id) { return new Account(AccountKind.DEAD, id); }
        public static Account free() { return new Account(AccountKind.FREE, null); }
        public static Account external() { return new Account(AccountKind.EXTERNAL, null); }
    }

    public record Mutation(long sequence, UUID operationId, Account from,
                           Account to, long amount, Phase phase) {
        public Mutation {
            if (sequence <= 0 || amount <= 0 || operationId == null
                    || from == null || to == null || phase == null
                    || from.equals(to)
                    || (from.kind() == AccountKind.EXTERNAL
                        && to.kind() == AccountKind.EXTERNAL))
                throw new IllegalArgumentException("Invalid monetary movement");
        }
        public Mutation eventApplied() {
            return new Mutation(sequence, operationId, from, to, amount, Phase.EVENT_APPLIED);
        }
        /** Positive for issuance, negative for destruction, zero for reattribution. */
        public long physicalSupplyDelta() {
            return from.kind() == AccountKind.EXTERNAL ? amount
                    : to.kind() == AccountKind.EXTERNAL ? -amount : 0;
        }
    }

    /** Distinguish a new durable reservation from a repeated event ID. */
    public record Preparation(Mutation mutation, boolean newlyCreated) {
        public Preparation { Objects.requireNonNull(mutation); }
    }

    private final Path directory;
    private final FileChannel lockChannel;
    private final FileLock writerLock;
    private final Path writerLockPath;
    private final Object writerLockFileKey;
    private final TreeMap<Long, Mutation> entries = new TreeMap<>();
    private final Map<UUID, Long> operationIds = new HashMap<>();
    private final boolean unreconciledOnStartup;
    private boolean closed;
    private boolean failed;
    private long nextAuditSequence = 1;
    // One OS directory cursor is retained across bounded Paper heartbeats.
    // Weakly consistent new entries will be checked on a subsequent pass.
    private DirectoryStream<Path> auditDirectoryStream;
    private java.util.Iterator<Path> auditDirectoryIterator;

    public NormalMutationJournal(Path storage) throws IOException {
        Objects.requireNonNull(storage, "storage");
        directory = storage.toAbsolutePath().normalize();
        for (Path ancestor = directory; ancestor != null; ancestor = ancestor.getParent()) {
            if (Files.isSymbolicLink(ancestor))
                throw new IOException("Symbolic directory in journal path: " + ancestor);
        }
        Files.createDirectories(directory);
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Journal storage must be a real directory");
        writerLockPath = directory.resolve(".writer.lock");
        FileChannel channel = FileChannel.open(writerLockPath,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
        FileLock lock;
        try {
            try { lock = channel.tryLock(); }
            catch (OverlappingFileLockException duplicate) {
                throw new IOException("Journal writer already active", duplicate);
            }
            if (lock == null) throw new IOException("Journal writer lock unavailable");
        } catch (IOException | RuntimeException failure) {
            channel.close();
            throw failure;
        }
        lockChannel = channel;
        writerLock = lock;
        try {
            writerLockFileKey = requireWriterLockFileKey();
            readAllAtStartup();
            unreconciledOnStartup = !entries.isEmpty();
        } catch (IOException | RuntimeException failure) {
            close();
            throw failure;
        }
    }

    private void readAllAtStartup() throws IOException {
        try (var files = Files.list(directory)) {
            for (Path file : files.filter(f -> f.getFileName().toString().endsWith(".mutation")).toList()) {
                Mutation mutation = decode(DurableFile.read(file, MAX_RECORD_BYTES));
                if (!file.getFileName().toString().equals(mutation.sequence() + ".mutation")
                        || entries.putIfAbsent(mutation.sequence(), mutation) != null
                        || operationIds.putIfAbsent(mutation.operationId(), mutation.sequence()) != null)
                    throw new IOException("Duplicate or misnamed journal mutation: " + file);
            }
        }
        long expected = 1;
        for (long sequence : entries.keySet()) {
            if (sequence != expected++) throw new IOException("Gap in monetary journal at " + expected);
        }
        long latest = entries.isEmpty() ? 0 : entries.lastKey();
        Path head = directory.resolve("head");
        if (!Files.exists(head, LinkOption.NOFOLLOW_LINKS)) {
            if (latest != 0) throw new IOException("Missing monetary journal head");
            writeHead(0);
            return;
        }
        long saved = readHead();
        if (saved > latest) throw new IOException("Monetary journal tail missing");
        // Recover only the crash window AFTER a complete, validated record was
        // durable and BEFORE the head was advanced. Never infer event applied.
        if (saved < latest) writeHead(latest);
    }

    private long readHead() throws IOException {
        byte[] bytes = DurableFile.read(directory.resolve("head"), 8);
        if (bytes.length != 8) throw new IOException("Invalid monetary journal head size");
        long head = ByteBuffer.wrap(bytes).getLong();
        if (head < 0) throw new IOException("Invalid monetary journal head sequence");
        return head;
    }

    private void writeHead(long sequence) throws IOException {
        DurableFile.write(directory.resolve("head"), ByteBuffer.allocate(8).putLong(sequence).array());
    }

    private Path recordPath(long sequence) {
        return directory.resolve(sequence + ".mutation");
    }

    /**
     * Idempotently reserve one proposed physical movement before Paper edits
     * the corresponding inventory/Entity. This does not mint or transfer money.
     */
    public synchronized Preparation prepare(UUID operationId, Account from,
                                         Account to, long amount) throws IOException {
        checkHealthy();
        Objects.requireNonNull(operationId, "operation id");
        Long previous = operationIds.get(operationId);
        if (previous != null) {
            Mutation old = entries.get(previous);
            if (!old.from().equals(from) || !old.to().equals(to) || old.amount() != amount)
                throw new IllegalStateException("Conflicting duplicate monetary operation ID");
            return new Preparation(old, false);
        }
        long next = entries.isEmpty() ? 1 : Math.addExact(entries.lastKey(), 1);
        Mutation candidate = new Mutation(next, operationId, from, to, amount, Phase.PREPARED);
        try {
            DurableFile.writeNew(recordPath(next), encode(candidate));
            writeHead(next);
        } catch (IOException failure) {
            failed = true;
            throw failure;
        }
        entries.put(next, candidate);
        operationIds.put(operationId, next);
        return new Preparation(candidate, true);
    }

    /**
     * The event reports a physical mutation, but world/Playerdata may not yet
     * be saved. An EVENT_APPLIED record is still unresolved after restart.
     */
    public synchronized Mutation markEventApplied(UUID operationId) throws IOException {
        checkHealthy();
        Long sequence = operationIds.get(Objects.requireNonNull(operationId));
        if (sequence == null) throw new IllegalArgumentException("Unknown mutation operation ID");
        Mutation before = entries.get(sequence);
        if (before.phase() == Phase.EVENT_APPLIED) return before;
        Mutation after = before.eventApplied();
        try { DurableFile.write(recordPath(sequence), encode(after)); }
        catch (IOException failure) { failed = true; throw failure; }
        entries.put(sequence, after);
        return after;
    }

    /** Both states are unresolved until an independent, complete physical audit. */
    public synchronized List<Mutation> unresolved() {
        checkHealthy();
        return List.copyOf(entries.values());
    }

    public synchronized boolean hasUnreconciledFromStartup() {
        checkHealthy();
        return unreconciledOnStartup;
    }

    /** Cheap liveness signal; it is NOT full record verification or admission authority. */
    public synchronized void checkHealthy() {
        if (closed || failed || !writerLock.isValid())
            throw new IllegalStateException("Monetary journal not writable");
        try {
            if (!writerLockFileKey.equals(requireWriterLockFileKey()))
                throw new IOException("Monetary journal writer lock path changed");
            long actual = readHead();
            long expected = entries.isEmpty() ? 0 : entries.lastKey();
            if (actual != expected) throw new IOException("Journal head diverged from live state");
        } catch (IOException | RuntimeException failure) {
            failed = true;
            throw new IllegalStateException("Monetary journal integrity failed; stop issuing", failure);
        }
    }

    /**
     * Check precisely one historical on-disk record using the current,
     * synchronized in-memory value. This is an incremental corruption audit
     * rather than a whole-journal scan on the Paper main thread.
     *
     * A valid-looking but stale EVENT_APPLIED/PREPARED record, missing record,
     * or changed issuer/account/amount is fatal. Legitimate tail additions
     * and phase transitions are read from current state on every invocation.
     *
     * This does not scan for newly inserted untracked filenames: complete
     * source-set verification remains a separate full-audit obligation.
     */
    public synchronized boolean auditNextRecord() {
        checkHealthy();
        Map.Entry<Long, Mutation> next = entries.ceilingEntry(nextAuditSequence);
        if (next == null) next = entries.firstEntry();
        if (next == null) return false;
        try {
            Mutation persisted = decode(DurableFile.read(
                    recordPath(next.getKey()), MAX_RECORD_BYTES));
            if (!next.getValue().equals(persisted))
                throw new IOException("Durable monetary journal record changed: "
                        + next.getKey());
        } catch (IOException | RuntimeException corruption) {
            failed = true;
            throw new IllegalStateException(
                    "Monetary journal historical record integrity failed; stop issuing",
                    corruption);
        }
        nextAuditSequence = next.getKey() == Long.MAX_VALUE ? 1 : next.getKey() + 1;
        return true;
    }

    /**
     * Read at most one canonical *.mutation directory entry. The record
     * decoder audit checks known record bytes separately. This companion
     * detects untracked, noncanonical and unexpected FILE IDENTITIES without
     * doing an O(n) directory listing inside a Paper heartbeat.
     *
     * DirectoryStream may not observe additions during an existing pass:
     * this is eventual corruption detection, not a frozen filesystem view.
     */
    public synchronized boolean auditNextDirectoryEntry() {
        checkHealthy();
        try {
            if (auditDirectoryIterator == null) {
                auditDirectoryStream = Files.newDirectoryStream(directory, "*.mutation");
                auditDirectoryIterator = auditDirectoryStream.iterator();
            }
            if (!auditDirectoryIterator.hasNext()) {
                resetDirectoryAudit();
                return false;
            }
            Path entry = auditDirectoryIterator.next();
            String name = entry.getFileName().toString();
            if (!name.endsWith(".mutation"))
                throw new IOException("Unexpected source returned by journal directory filter");
            String identity = name.substring(0, name.length() - ".mutation".length());
            final long sequence;
            try {
                sequence = Long.parseLong(identity);
            } catch (NumberFormatException malformed) {
                throw new IOException("Malformed monetary journal record filename: " + name,
                        malformed);
            }
            if (sequence < 1 || !name.equals(sequence + ".mutation")
                    || !entries.containsKey(sequence))
                throw new IOException("Unexpected monetary journal file: " + name);
            return true;
        } catch (IOException | RuntimeException corruption) {
            failed = true;
            try { resetDirectoryAudit(); }
            catch (IOException cleanup) { corruption.addSuppressed(cleanup); }
            throw new IllegalStateException(
                    "Monetary journal unexpected directory record integrity failed",
                    corruption);
        }
    }

    private void resetDirectoryAudit() throws IOException {
        DirectoryStream<Path> existing = auditDirectoryStream;
        auditDirectoryStream = null;
        auditDirectoryIterator = null;
        if (existing != null) existing.close();
    }

    /** Detect missing, replaced or symbolic writer lock files before mutation. */
    private Object requireWriterLockFileKey() throws IOException {
        for (Path parent = writerLockPath.getParent(); parent != null; parent = parent.getParent()) {
            if (Files.isSymbolicLink(parent))
                throw new IOException("Symbolic directory in journal lock path: " + parent);
        }
        BasicFileAttributes attributes = Files.readAttributes(
                writerLockPath, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        Object key = attributes.fileKey();
        if (!attributes.isRegularFile() || key == null)
            throw new IOException("Monetary journal writer lock identity unavailable");
        return key;
    }

    /**
     * Expensive full-record and source-set validation. Required during audits:
     * a checksum-valid set of known records does not exclude untracked records
     * produced by a failed or competing writer.
     */
    public synchronized void verifyAll() {
        checkHealthy();
        try {
            Set<String> expectedFiles = new HashSet<>();
            for (Mutation entry : entries.values()) {
                Mutation stored = decode(DurableFile.read(recordPath(entry.sequence()), MAX_RECORD_BYTES));
                if (!entry.equals(stored))
                    throw new IOException("Journal record changed: " + entry.sequence());
                expectedFiles.add(entry.sequence() + ".mutation");
            }
            try (var files = Files.list(directory)) {
                for (Path file : files.toList()) {
                    String name = file.getFileName().toString();
                    if (name.endsWith(".mutation") && !expectedFiles.contains(name))
                        throw new IOException("Untracked monetary mutation record: " + file);
                }
            }
        } catch (IOException | RuntimeException failure) {
            failed = true;
            throw new IllegalStateException("Monetary journal record mismatch", failure);
        }
    }

    private static byte[] encode(Mutation entry) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(buffer)) {
            out.writeInt(MAGIC);
            out.writeLong(entry.sequence());
            out.writeUTF(entry.operationId().toString());
            writeAccount(out, entry.from());
            writeAccount(out, entry.to());
            out.writeLong(entry.amount());
            out.writeByte(entry.phase().ordinal());
        }
        return buffer.toByteArray();
    }

    private static void writeAccount(DataOutputStream out, Account account) throws IOException {
        out.writeByte(account.kind().ordinal());
        out.writeUTF(account.player() == null ? "" : account.player().toString());
    }

    private static Account readAccount(DataInputStream in) throws IOException {
        int kind = in.readUnsignedByte();
        if (kind >= AccountKind.values().length) throw new IOException("Unknown account kind");
        String identity = in.readUTF();
        UUID player = identity.isEmpty() ? null : parseUuid(identity);
        try { return new Account(AccountKind.values()[kind], player); }
        catch (IllegalArgumentException malformed) { throw new IOException("Invalid account", malformed); }
    }

    private static UUID parseUuid(String text) throws IOException {
        try {
            UUID uuid = UUID.fromString(text);
            if (!uuid.toString().equals(text)) throw new IOException("Noncanonical UUID");
            return uuid;
        } catch (IllegalArgumentException badId) { throw new IOException("Invalid UUID", badId); }
    }

    private static Mutation decode(byte[] data) throws IOException {
        try (var in = new DataInputStream(new ByteArrayInputStream(data))) {
            if (in.readInt() != MAGIC) throw new IOException("Unsupported monetary journal schema");
            long sequence = in.readLong();
            UUID id = parseUuid(in.readUTF());
            Account from = readAccount(in), to = readAccount(in);
            long amount = in.readLong();
            int phase = in.readUnsignedByte();
            if (phase >= Phase.values().length) throw new IOException("Unknown mutation phase");
            if (in.available() != 0) throw new IOException("Trailing monetary journal bytes");
            return new Mutation(sequence, id, from, to, amount, Phase.values()[phase]);
        } catch (IllegalArgumentException malformed) {
            throw new IOException("Malformed monetary movement", malformed);
        }
    }

    @Override public synchronized void close() throws IOException {
        if (closed) return;
        closed = true;
        try { resetDirectoryAudit(); }
        finally {
            try { writerLock.release(); }
            finally { lockChannel.close(); }
        }
    }
}
