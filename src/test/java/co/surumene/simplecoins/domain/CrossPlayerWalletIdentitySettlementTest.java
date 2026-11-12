package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CrossPlayerWalletIdentitySettlementTest {
    @TempDir Path root;

    private Path playerdata(UUID owner, UUID walletId, long balance) throws Exception {
        Path file = root.resolve(owner + ".dat");
        var nbt = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(nbt)) {
            out.writeByte(10); out.writeUTF("");
            out.writeByte(9); out.writeUTF("Inventory"); out.writeByte(10); out.writeInt(1);
            out.writeByte(1); out.writeUTF("Slot"); out.writeByte(0);
            out.writeByte(8); out.writeUTF("id"); out.writeUTF("minecraft:bundle");
            out.writeByte(3); out.writeUTF("count"); out.writeInt(1);
            out.writeByte(10); out.writeUTF("components");
            out.writeByte(10); out.writeUTF("minecraft:custom_data");
            out.writeByte(10); out.writeUTF("PublicBukkitValues");
            out.writeByte(8); out.writeUTF("simplecoins:wallet_id"); out.writeUTF(walletId.toString());
            out.writeByte(4); out.writeUTF("simplecoins:wallet_balance"); out.writeLong(balance);
            out.writeByte(0); out.writeByte(0); out.writeByte(0);
            out.writeByte(0); // Item compound
            out.writeByte(9); out.writeUTF("EnderItems"); out.writeByte(10); out.writeInt(0);
            out.writeByte(0); // Root
        }
        var bytes = new ByteArrayOutputStream();
        try (var zipped = new GZIPOutputStream(bytes)) { zipped.write(nbt.toByteArray()); }
        Files.write(file, bytes.toByteArray());
        return file;
    }

    @Test void walletAlreadySeenInLiveSourceMustAbortSavedSettlement() throws Exception {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000001101");
        UUID copied = UUID.fromString("00000000-0000-0000-0000-000000009901");
        var manifest = new WorldStorageDiscovery.Manifest(List.of(),
                Map.of(owner, playerdata(owner, copied, 340L)));
        Set<UUID> sharedGeneration = new java.util.HashSet<>(Set.of(copied));
        assertThrows(IOException.class, () -> SettlementScanRunner.stage(
                manifest, new WorldScope(true, Set.of(), Set.of()), Set.of(),
                (world, position) -> null, (world, chunk) -> Map.of(),
                1048576, 1048576, sharedGeneration));
        assertTrue(sharedGeneration.contains(copied));
    }

    @Test void differentWalletAlreadySeenInLiveSourceRemainsDistinct() throws Exception {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000001102");
        UUID live = UUID.fromString("00000000-0000-0000-0000-000000009902");
        UUID saved = UUID.fromString("00000000-0000-0000-0000-000000009903");
        var manifest = new WorldStorageDiscovery.Manifest(List.of(),
                Map.of(owner, playerdata(owner, saved, 200L)));
        Set<UUID> sharedGeneration = new java.util.HashSet<>(Set.of(live));
        var staged = SettlementScanRunner.stage(
                manifest, new WorldScope(true, Set.of(), Set.of()), Set.of(),
                (world, position) -> null, (world, chunk) -> Map.of(),
                1048576, 1048576, sharedGeneration);
        assertEquals(Set.of(live, saved), sharedGeneration);
        assertEquals(BigInteger.valueOf(200L),
                staged.pending().commit().personalBalances().get(owner));
    }

    @Test void clonedWalletAcrossTwoDistinctPlayerdataMustAbortEntireSettlement() throws Exception {
        UUID a = UUID.fromString("00000000-0000-0000-0000-000000001001");
        UUID b = UUID.fromString("00000000-0000-0000-0000-000000001002");
        UUID sameWallet = UUID.fromString("00000000-0000-0000-0000-000000009999");
        var manifest = new WorldStorageDiscovery.Manifest(List.of(), Map.of(
                a, playerdata(a, sameWallet, 100L),
                b, playerdata(b, sameWallet, 100L)));
        assertThrows(IOException.class, () -> SettlementScanRunner.stage(manifest,
                new WorldScope(true, Set.of(), Set.of()), Set.of(),
                (world, position) -> null, (world, chunk) -> Map.of(),
                1048576, 1048576));
    }

    @Test void independentWalletsAcrossTwoPlayersRemainIndependentlyCounted() throws Exception {
        UUID a = UUID.fromString("00000000-0000-0000-0000-000000001001");
        UUID b = UUID.fromString("00000000-0000-0000-0000-000000001002");
        var manifest = new WorldStorageDiscovery.Manifest(List.of(), Map.of(
                a, playerdata(a, UUID.fromString("00000000-0000-0000-0000-000000009991"), 100L),
                b, playerdata(b, UUID.fromString("00000000-0000-0000-0000-000000009992"), 200L)));
        var staged = SettlementScanRunner.stage(manifest,
                new WorldScope(true, Set.of(), Set.of()), Set.of(),
                (world, position) -> null, (world, chunk) -> Map.of(),
                1048576, 1048576);
        var balances = staged.pending().commit().personalBalances();
        assertEquals(BigInteger.valueOf(100L), balances.get(a));
        assertEquals(BigInteger.valueOf(200L), balances.get(b));
    }
}
