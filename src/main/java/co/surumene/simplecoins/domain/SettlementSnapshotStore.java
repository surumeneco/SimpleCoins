package co.surumene.simplecoins.domain;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Single-host, restart-readable settlement generations. Only fully committed
 * accumulator results may be passed to this store. The file lock serializes
 * writers that use this class; it does not freeze Minecraft world sources.
 */
public final class SettlementSnapshotStore {
    private static final int MAGIC = 0x53435331; // SCS1
    private static final int VERSION = 1;
    private static final int MAX_BYTES = 4 * 1024 * 1024;
    private static final int MAX_RECORDS = 100_000;
    private static final int MAX_INTEGER_BYTES = 512;
    private final Path root;
    private final Path snapshot;
    private final Path lock;
    private final Path initialized;

    public SettlementSnapshotStore(Path directory) throws IOException {
        if (directory == null) throw new IOException("Missing settlement snapshot directory");
        root = directory.toAbsolutePath().normalize();
        requireRealAncestors();
        Files.createDirectories(root);
        requireRealAncestors();
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Settlement snapshot root is not a real directory");
        snapshot = root.resolve("settlement.snapshot");
        lock = root.resolve("settlement.lock");
        initialized = root.resolve("settlement.initialized");
    }

    /** Missing means no generation was committed; malformed means fail-closed. */
    public Optional<SettlementGenerationPublisher.Published> load() throws IOException {
        requireRealAncestors();
        if (!Files.exists(snapshot, LinkOption.NOFOLLOW_LINKS)) {
            if (Files.exists(initialized, LinkOption.NOFOLLOW_LINKS))
                throw new IOException("Previously initialized settlement snapshot is missing");
            return Optional.empty();
        }
        if (Files.exists(initialized, LinkOption.NOFOLLOW_LINKS) &&
                !Arrays.equals(DurableFile.read(initialized, 4), new byte[] {0x53, 0x43, 0x53, 0x31}))
            throw new IOException("Invalid settlement initialization marker");
        return Optional.of(decode(DurableFile.read(snapshot, MAX_BYTES)));
    }

    /**
     * Persists one successive generation and verifies the bytes before return.
     * A failure has an ambiguous disk outcome: callers must reload, never retry
     * blindly with the same expected generation.
     */
    public void persist(SettlementGenerationPublisher.Published candidate) throws IOException {
        Objects.requireNonNull(candidate, "candidate");
        byte[] payload = encode(candidate);
        requireRealAncestors();
        try (FileChannel channel = FileChannel.open(lock,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
             FileLock guard = channel.tryLock()) {
            if (guard == null) throw new IOException("Settlement snapshot writer is busy");
            if (!Files.isRegularFile(lock, LinkOption.NOFOLLOW_LINKS))
                throw new IOException("Invalid settlement writer lock");
            long current = load().map(SettlementGenerationPublisher.Published::generation).orElse(0L);
            if (current == Long.MAX_VALUE || candidate.generation() != current + 1)
                throw new IOException("Stale or non-sequential settlement generation");
            DurableFile.write(snapshot, payload);
            if (!candidate.equals(load().orElseThrow(
                    () -> new IOException("Committed settlement snapshot disappeared"))))
                throw new IOException("Settlement snapshot readback differs from committed candidate");
            if (!Files.exists(initialized, LinkOption.NOFOLLOW_LINKS))
                DurableFile.write(initialized, new byte[] {0x53, 0x43, 0x53, 0x31});
        } catch (OverlappingFileLockException concurrent) {
            throw new IOException("Settlement snapshot writer already active", concurrent);
        }
    }

    private void requireRealAncestors() throws IOException {
        for (Path part = root; part != null; part = part.getParent()) {
            if (Files.isSymbolicLink(part))
                throw new IOException("Settlement snapshot path contains a symbolic link: " + part);
        }
    }

    private static byte[] encode(SettlementGenerationPublisher.Published candidate) throws IOException {
        SettlementAccumulator.Result result = candidate.result();
        validate(result);
        var bytes = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(bytes)) {
            out.writeInt(MAGIC);
            out.writeInt(VERSION);
            out.writeLong(candidate.generation());
            out.writeInt(result.personalBalances().size());
            for (var entry : result.personalBalances().entrySet().stream()
                    .sorted(Comparator.comparing(e -> e.getKey().toString())).toList()) {
                writeUuid(out, entry.getKey());
                writeBig(out, entry.getValue());
            }
            writeBig(out, result.free());
            writeBig(out, result.dead());
            out.writeInt(result.deadLocations().size());
            for (var dead : result.deadLocations()) {
                writeUuid(out, dead.owner());
                out.writeUTF(dead.reason().name());
                writeBig(out, dead.value());
                out.writeUTF(dead.location());
            }
        }
        if (bytes.size() > MAX_BYTES) throw new IOException("Settlement snapshot exceeds byte budget");
        return bytes.toByteArray();
    }

    private static SettlementGenerationPublisher.Published decode(byte[] bytes) throws IOException {
        try (var in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            if (in.readInt() != MAGIC || in.readInt() != VERSION)
                throw new IOException("Unknown settlement snapshot format");
            long generation = in.readLong();
            if (generation <= 0) throw new IOException("Invalid settlement generation");
            int players = readCount(in);
            Map<UUID, BigInteger> balances = new HashMap<>();
            for (int i = 0; i < players; i++) {
                UUID id = readUuid(in);
                BigInteger value = readBig(in);
                if (value.signum() < 0 || balances.putIfAbsent(id, value) != null)
                    throw new IOException("Invalid or duplicate settlement player balance");
            }
            BigInteger free = readBig(in);
            BigInteger dead = readBig(in);
            int locations = readCount(in);
            List<SettlementAccumulator.DeadLocation> deadLocations = new ArrayList<>(locations);
            for (int i = 0; i < locations; i++) {
                UUID owner = readUuid(in);
                AssetOwner.DeadReason reason = AssetOwner.DeadReason.valueOf(in.readUTF());
                BigInteger value = readBig(in);
                String location = in.readUTF();
                deadLocations.add(new SettlementAccumulator.DeadLocation(owner, reason, value, location));
            }
            if (in.available() != 0) throw new IOException("Trailing settlement snapshot bytes");
            var result = new SettlementAccumulator.Result(
                    AssetStatistics.calculate(balances, free, dead),
                    Map.copyOf(balances), free, dead, List.copyOf(deadLocations));
            validate(result);
            return new SettlementGenerationPublisher.Published(generation, result);
        } catch (RuntimeException invalid) {
            throw new IOException("Invalid settlement snapshot contents", invalid);
        }
    }

    private static void validate(SettlementAccumulator.Result result) throws IOException {
        if (result == null || result.personalBalances() == null || result.deadLocations() == null)
            throw new IOException("Missing settlement result fields");
        if (result.personalBalances().size() > MAX_RECORDS || result.deadLocations().size() > MAX_RECORDS)
            throw new IOException("Too many settlement records");
        try {
            var expected = AssetStatistics.calculate(result.personalBalances(), result.free(), result.dead());
            if (!expected.equals(result.statistics()))
                throw new IOException("Settlement statistics differ from source balances");
            BigInteger deadTotal = BigInteger.ZERO;
            for (var item : result.deadLocations()) {
                if (item == null || item.owner() == null || item.reason() == null ||
                        item.value() == null || item.value().signum() <= 0 ||
                        item.location() == null || item.location().isBlank() ||
                        item.location().length() > 1024)
                    throw new IOException("Invalid dead-asset location");
                deadTotal = deadTotal.add(item.value());
            }
            if (!deadTotal.equals(result.dead()))
                throw new IOException("Dead-asset locations do not reconcile to dead total");
        } catch (IllegalArgumentException | NullPointerException invalid) {
            throw new IOException("Invalid settlement result", invalid);
        }
    }

    private static int readCount(DataInputStream in) throws IOException {
        int count = in.readInt();
        if (count < 0 || count > MAX_RECORDS) throw new IOException("Invalid settlement record count");
        return count;
    }
    private static void writeUuid(DataOutputStream out, UUID id) throws IOException {
        if (id == null) throw new IOException("Missing settlement UUID");
        out.writeLong(id.getMostSignificantBits());
        out.writeLong(id.getLeastSignificantBits());
    }
    private static UUID readUuid(DataInputStream in) throws IOException {
        return new UUID(in.readLong(), in.readLong());
    }
    private static void writeBig(DataOutputStream out, BigInteger value) throws IOException {
        if (value == null || value.signum() < 0) throw new IOException("Invalid settlement amount");
        byte[] bytes = value.toByteArray();
        if (bytes.length > MAX_INTEGER_BYTES) throw new IOException("Settlement amount exceeds byte budget");
        out.writeInt(bytes.length);
        out.write(bytes);
    }
    private static BigInteger readBig(DataInputStream in) throws IOException {
        int size = in.readInt();
        if (size < 1 || size > MAX_INTEGER_BYTES) throw new IOException("Invalid settlement amount size");
        byte[] bytes = new byte[size];
        in.readFully(bytes);
        BigInteger result = new BigInteger(bytes);
        if (result.signum() < 0) throw new IOException("Negative settlement amount");
        return result;
    }
}
