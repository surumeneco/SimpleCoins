package co.surumene.simplecoins.domain;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** Single-writer issuance journal. A reservation is not a physical asset. */
public final class GrantLedger {
    public record Grant(long sequence, UUID id, UUID player, long amount, String issuer,
                        long createdMillis, boolean delivered,
                        CoinDenomination denomination, long coinCount) {
        public Grant {
            Objects.requireNonNull(id); Objects.requireNonNull(player); Objects.requireNonNull(issuer);
            if (sequence <= 0 || amount <= 0 || issuer.isBlank() || issuer.length() > 1024)
                throw new IllegalArgumentException("Invalid grant");
            if (denomination == null) {
                if (coinCount != 0) throw new IllegalArgumentException("Unspecified denomination has a count");
            } else if (coinCount <= 0 || Math.multiplyExact(coinCount, denomination.value()) != amount) {
                throw new IllegalArgumentException("Coin reservation has an inconsistent amount");
            }
        }
        Grant completed() { return new Grant(sequence, id, player, amount, issuer, createdMillis,
                true, denomination, coinCount); }
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
            if (!g.player().equals(player)) continue;
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
            out.writeInt(0x53434732); out.writeLong(grant.sequence());
            out.writeUTF(grant.id().toString()); out.writeUTF(grant.player().toString());
            out.writeLong(grant.amount()); out.writeUTF(grant.issuer());
            out.writeLong(grant.createdMillis()); out.writeBoolean(grant.delivered());
            out.writeInt(grant.denomination() == null ? 0 : grant.denomination().value());
            out.writeLong(grant.coinCount());
        }
        return bytes.toByteArray();
    }
    private static Grant decode(byte[] bytes) throws IOException {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            int schema = in.readInt();
            if (schema != 0x53434731 && schema != 0x53434732)
                throw new IOException("Unknown issuance journal schema");
            long sequence = in.readLong();
            UUID id = UUID.fromString(in.readUTF()), player = UUID.fromString(in.readUTF());
            long amount = in.readLong();
            String issuer = in.readUTF();
            long createdMillis = in.readLong();
            boolean delivered = in.readBoolean();
            int denominationValue = schema == 0x53434732 ? in.readInt() : 0;
            long coinCount = schema == 0x53434732 ? in.readLong() : 0;
            CoinDenomination denomination = denominationValue == 0 ? null :
                    CoinDenomination.fromValue(denominationValue);
            Grant result = new Grant(sequence, id, player, amount, issuer,
                    createdMillis, delivered, denomination, coinCount);
            if (in.available() != 0) throw new IOException("Trailing journal data");
            return result;
        } catch (IllegalArgumentException ex) { throw new IOException("Invalid journal", ex); }
    }
}
