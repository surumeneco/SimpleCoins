package co.surumene.simplecoins.domain;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** Single-writer issuance journal. A reservation is not a physical asset. */
public final class GrantLedger {
    public record Grant(long sequence, UUID id, UUID player, long amount, String issuer,
                        long createdMillis, boolean delivered) {
        public Grant {
            Objects.requireNonNull(id); Objects.requireNonNull(player); Objects.requireNonNull(issuer);
            if (sequence <= 0 || amount <= 0 || issuer.isBlank() || issuer.length()>1024) throw new IllegalArgumentException("Invalid grant");
        }
        Grant completed() { return new Grant(sequence, id, player, amount, issuer, createdMillis, true); }
    }
    private final Path directory;
    private final TreeMap<Long, Grant> grants = new TreeMap<>();
    private boolean failed;

    public GrantLedger(Path directory) throws IOException {
        this.directory = directory;
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
        checkHealthy();
        long next = grants.isEmpty() ? 1 : Math.addExact(grants.lastKey(), 1);
        Grant grant = new Grant(next, UUID.randomUUID(), player, amount, issuer, Instant.now().toEpochMilli(), false);
        persist(grant);
        try { writeHead(next); }
        catch (IOException failure) { failed = true; throw failure; }
        grants.put(next, grant);
        return grant;
    }
    public synchronized List<Grant> pending(UUID player) {
        checkHealthy();
        return grants.values().stream().filter(g -> g.player().equals(player) && !g.delivered()).toList();
    }
    public synchronized boolean wasApplied(Grant grant, long savedReceipt) {
        checkHealthy();
        validateReceipt(grant.player(), savedReceipt);
        return savedReceipt >= grant.sequence();
    }
    public synchronized void validateReceipt(UUID player, long receipt) {
        checkHealthy();
        if (receipt < 0 || (receipt != 0 && (!grants.containsKey(receipt) ||
                !grants.get(receipt).player().equals(player))))
            throw new IllegalStateException("Receipt does not belong to this player's journal");
        for (Grant g : grants.values()) {
            if (g.player().equals(player) && g.delivered() && g.sequence() > receipt)
                throw new IllegalStateException("Playerdata receipt rolled back behind acknowledged grant");
        }
    }
    public synchronized void acknowledge(Grant grant, long savedReceipt) throws IOException {
        checkHealthy();
        Grant current = grants.get(grant.sequence());
        if (current == null || !current.id().equals(grant.id()) || !wasApplied(current, savedReceipt))
            throw new IllegalStateException("Cannot acknowledge an unapplied grant");
        if (current.delivered()) return;
        Grant complete = current.completed();
        persist(complete);
        grants.put(complete.sequence(), complete);
    }
    private void checkHealthy() {
        if (failed) throw new IllegalStateException("Issuance journal failed; reload from disk before continuing");
    }
    private void persist(Grant grant) throws IOException {
        try { DurableFile.write(directory.resolve(grant.sequence() + ".grant"), encode(grant)); }
        catch (IOException ex) { failed = true; throw ex; }
    }
    private static byte[] encode(Grant grant) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(0x53434731); out.writeLong(grant.sequence());
            out.writeUTF(grant.id().toString()); out.writeUTF(grant.player().toString());
            out.writeLong(grant.amount()); out.writeUTF(grant.issuer());
            out.writeLong(grant.createdMillis()); out.writeBoolean(grant.delivered());
        }
        return bytes.toByteArray();
    }
    private static Grant decode(byte[] bytes) throws IOException {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            if (in.readInt() != 0x53434731) throw new IOException("Unknown issuance journal schema");
            Grant result = new Grant(in.readLong(), UUID.fromString(in.readUTF()), UUID.fromString(in.readUTF()),
                in.readLong(), in.readUTF(), in.readLong(), in.readBoolean());
            if (in.available() != 0) throw new IOException("Trailing journal data");
            return result;
        } catch (IllegalArgumentException ex) { throw new IOException("Invalid journal", ex); }
    }
}
