package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The saved settlement must derive BAN/playerdata facts, not trust booleans from owner callbacks. */
class BanCoveredSettlementStageTest {
    @TempDir Path root;
    private static final UUID OWNER =
            UUID.fromString("00000000-0000-0000-0000-000000000091");
    private static final WorldScope SCOPE = new WorldScope(true, Set.of(), Set.of());
    private static final Set<String> PROVIDERS = Set.of("paper-profile", "paper-ip", "external");
    private static final SettlementScanRunner.BlockOwnerLookup NO_BLOCKS =
            (world, position) -> { throw new IOException("Unexpected block owner call"); };
    private static final SettlementScanRunner.EntityOwnerLookup NO_ENTITIES =
            (world, chunk) -> Map.of();

    private static BanSourceCoverage evidence(BanEvidence.Status ip) throws IOException {
        return BanSourceCoverage.capture(PROVIDERS, Map.of(OWNER, List.of(
                new BanEvidence.Source("paper-profile", BanEvidence.Status.CLEAR),
                new BanEvidence.Source("paper-ip", ip),
                new BanEvidence.Source("external", BanEvidence.Status.CLEAR))));
    }

    private Path playerData(int count) throws IOException {
        Path file = root.resolve("playerdata/" + OWNER + ".dat");
        Files.createDirectories(file.getParent());
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeByte(10); out.writeUTF("");
            out.writeByte(9); out.writeUTF("Inventory"); out.writeByte(10);
            out.writeInt(1);
            out.writeByte(1); out.writeUTF("Slot"); out.writeByte(0);
            out.writeByte(8); out.writeUTF("id"); out.writeUTF("minecraft:paper");
            out.writeByte(3); out.writeUTF("count"); out.writeInt(count);
            out.writeByte(10); out.writeUTF("components");
            out.writeByte(10); out.writeUTF("minecraft:custom_data");
            out.writeByte(10); out.writeUTF("PublicBukkitValues");
            out.writeByte(3); out.writeUTF("simplecoins:denomination"); out.writeInt(100);
            out.writeByte(0); out.writeByte(0); out.writeByte(0); out.writeByte(0);
            out.writeByte(9); out.writeUTF("EnderItems"); out.writeByte(10); out.writeInt(0);
            out.writeByte(0);
        }
        ByteArrayOutputStream zipped = new ByteArrayOutputStream();
        try (GZIPOutputStream out = new GZIPOutputStream(zipped)) {
            out.write(bytes.toByteArray());
        }
        Files.write(file, zipped.toByteArray());
        return file;
    }

    private Path chestRegion() throws IOException {
        Path file = root.resolve("region/r.0.0.mca");
        Files.createDirectories(file.getParent());
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeByte(10); out.writeUTF("");
            out.writeByte(9); out.writeUTF("block_entities"); out.writeByte(10); out.writeInt(1);
            out.writeByte(8); out.writeUTF("id"); out.writeUTF("minecraft:chest");
            out.writeByte(3); out.writeUTF("x"); out.writeInt(0);
            out.writeByte(3); out.writeUTF("y"); out.writeInt(64);
            out.writeByte(3); out.writeUTF("z"); out.writeInt(0);
            out.writeByte(0);
            out.writeByte(0);
        }
        ByteArrayOutputStream packed = new ByteArrayOutputStream();
        try (DeflaterOutputStream out = new DeflaterOutputStream(packed)) {
            out.write(bytes.toByteArray());
        }
        byte[] compressed = packed.toByteArray();
        byte[] region = new byte[3 * 4096];
        ByteBuffer.wrap(region).putInt(0, (2 << 8) | 1);
        ByteBuffer.wrap(region).putInt(8192, compressed.length + 1);
        region[8196] = 2;
        System.arraycopy(compressed, 0, region, 8197, compressed.length);
        Files.write(file, region);
        return file;
    }

    @Test void ipBanAloneMovesSavedPlayerMoneyToDeadWithoutRawUuidBanList() throws Exception {
        var manifest = new WorldStorageDiscovery.Manifest(List.of(), Map.of(OWNER, playerData(2)));
        var staged = SettlementScanRunner.stageWithBanCoverage(manifest, SCOPE,
                evidence(BanEvidence.Status.BANNED), NO_BLOCKS, NO_ENTITIES, 4096, 4096);
        assertEquals(BigInteger.valueOf(200), staged.pending().commit().dead());
    }

    @Test void completeClearCoveragePreservesPlayerMoneyAsPersonal() throws Exception {
        var manifest = new WorldStorageDiscovery.Manifest(List.of(), Map.of(OWNER, playerData(2)));
        var staged = SettlementScanRunner.stageWithBanCoverage(manifest, SCOPE,
                evidence(BanEvidence.Status.CLEAR), NO_BLOCKS, NO_ENTITIES, 4096, 4096);
        assertEquals(BigInteger.valueOf(200),
                staged.pending().commit().personalBalances().get(OWNER));
    }

    @Test void playerdataWithoutBanEvidenceCannotStartEvenWithNoEntities() throws Exception {
        var manifest = new WorldStorageDiscovery.Manifest(List.of(), Map.of(OWNER, playerData(1)));
        var emptyEvidence = BanSourceCoverage.capture(PROVIDERS, Map.of());
        assertThrows(IOException.class, () -> SettlementScanRunner.stageWithBanCoverage(
                manifest, SCOPE, emptyEvidence, NO_BLOCKS, NO_ENTITIES, 4096, 4096));
    }

    @Test void blockOwnerFlagsCannotOverrideBanAndPlayerdataEvidence() throws Exception {
        var manifest = new WorldStorageDiscovery.Manifest(List.of(
                new WorldStorageDiscovery.SourceFile("world", chestRegion(),
                        WorldStorageDiscovery.Kind.REGION)), Map.of(OWNER, playerData(0)));
        var staged = SettlementScanRunner.stageWithBanCoverage(manifest, SCOPE,
                evidence(BanEvidence.Status.CLEAR),
                (world, block) -> new BlockEntitySettlementImport.Ownership(
                        OWNER, false, true, false),
                NO_ENTITIES, 4096, 4096);
        var result = staged.pending().commit();
        assertEquals(BigInteger.ZERO, result.personalBalances().get(OWNER));
    }

    @Test void unknownBlockOwnerBanEvidenceIsRejectedNotAssumedClear() throws Exception {
        var manifest = new WorldStorageDiscovery.Manifest(List.of(
                new WorldStorageDiscovery.SourceFile("world", chestRegion(),
                        WorldStorageDiscovery.Kind.REGION)), Map.of());
        var emptyEvidence = BanSourceCoverage.capture(PROVIDERS, Map.of());
        assertThrows(IOException.class, () -> SettlementScanRunner.stageWithBanCoverage(
                manifest, SCOPE, emptyEvidence,
                (world, block) -> new BlockEntitySettlementImport.Ownership(
                        OWNER, false, false, false),
                NO_ENTITIES, 4096, 4096));
    }
}
