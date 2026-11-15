package co.surumene.simplecoins.paper;

import static org.junit.jupiter.api.Assertions.*;

import co.surumene.simplecoins.domain.CoinDenomination;
import co.surumene.simplecoins.domain.GrantLedger;
import co.surumene.simplecoins.domain.GrantReceiptState;
import co.surumene.simplecoins.domain.PlayerDataReader;
import co.surumene.simplecoins.domain.WorldScope;
import co.surumene.simplecoins.domain.WorldStorageDiscovery;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Real compressed NBT bytes, not a fabricated successful Paper saveData. */
class SavedGrantDeliveryFileReadbackTest {
    @TempDir Path temp;

    private Path world() throws IOException {
        var folder = temp.resolve("world");
        Files.createDirectories(folder);
        return folder;
    }

    private Path write(UUID player, GrantReceiptState receipt, boolean grantCoin,
                       String preservedItem) throws IOException {
        Path file = world().resolve("playerdata").resolve(player + ".dat");
        Files.createDirectories(file.getParent());
        try (var gzip = new GZIPOutputStream(Files.newOutputStream(file));
             var out = new DataOutputStream(gzip)) {
            out.writeByte(10); out.writeUTF("");
            out.writeByte(10); out.writeUTF("BukkitValues");
            byte[] bytes = PaperGrantReceiptPdc.toSavedBytes(receipt);
            out.writeByte(7); out.writeUTF("simplecoins:grant_receipt_v1");
            out.writeInt(bytes.length); out.write(bytes);
            out.writeByte(0);
            out.writeByte(9); out.writeUTF("Inventory");
            out.writeByte(10); out.writeInt(grantCoin ? 2 : 1);
            if (grantCoin) {
                out.writeByte(1); out.writeUTF("Slot"); out.writeByte(0);
                out.writeByte(8); out.writeUTF("id"); out.writeUTF("minecraft:paper");
                out.writeByte(3); out.writeUTF("count"); out.writeInt(1);
                out.writeByte(10); out.writeUTF("components");
                out.writeByte(8); out.writeUTF("minecraft:item_model");
                out.writeUTF("simplecoins:coin_5000");
                out.writeByte(10); out.writeUTF("minecraft:custom_data");
                out.writeByte(10); out.writeUTF("PublicBukkitValues");
                out.writeByte(3); out.writeUTF("simplecoins:coin_denomination");
                out.writeInt(5000);
                out.writeByte(0); out.writeByte(0); out.writeByte(0); out.writeByte(0);
            }
            out.writeByte(1); out.writeUTF("Slot"); out.writeByte(9);
            out.writeByte(8); out.writeUTF("id"); out.writeUTF(preservedItem);
            out.writeByte(3); out.writeUTF("count"); out.writeInt(2);
            out.writeByte(0);
            out.writeByte(9); out.writeUTF("EnderItems");
            out.writeByte(10); out.writeInt(0);
            out.writeByte(0);
        }
        return file;
    }

    private WorldStorageDiscovery.Manifest discovery() throws IOException {
        return WorldStorageDiscovery.discover(
                Map.of("world", world()), new WorldScope(true, Set.of(), Set.of()), 256);
    }

    private PaperGrantInventorySlotPlan plan(GrantLedger ledger, UUID player,
                                              GrantLedger.Grant grant) {
        var fingerprints = new ArrayList<>(Collections.nCopies(36, "stone"));
        fingerprints.set(0, null);
        return PaperGrantInventorySlotPlan.stage(
                ledger, player, grant, GrantReceiptState.empty(),
                new PaperGrantInventorySlotPlan.StorageSnapshot(fingerprints)).orElseThrow();
    }

    @Test void verifiedFileHasCorrectPostsaveReceiptAndPreservesOldStorage() throws Exception {
        UUID player = UUID.randomUUID();
        var ledger = new GrantLedger(temp.resolve("ledger1"));
        var grant = ledger.reserve(player, 5000, "console");
        var plan = plan(ledger, player, grant);
        Path file = write(player, GrantReceiptState.empty(), false, "minecraft:stone");
        var before = PlayerDataReader.read(file, 65536, 65536);
        write(player, plan.nextReceipt(), true, "minecraft:stone");
        var proof = SavedGrantDeliveryFileReadback.verify(
                discovery(), ledger, player, plan, before, 65536, 65536);
        assertEquals(plan.nextReceipt(), proof.receipt());
        assertEquals(5000, proof.matchedMonetaryValue().longValueExact());
        assertEquals(1, ledger.pending(player).size(), "Readback cannot ack the journal");
    }

    @Test void returningFromVoidSaveWithoutWritingCannotCountAsDelivery() throws Exception {
        UUID player = UUID.randomUUID();
        var ledger = new GrantLedger(temp.resolve("ledger2"));
        var grant = ledger.reserve(player, 5000, "console");
        var plan = plan(ledger, player, grant);
        Path file = write(player, GrantReceiptState.empty(), false, "minecraft:stone");
        var before = PlayerDataReader.read(file, 65536, 65536);
        assertThrows(IOException.class, () -> SavedGrantDeliveryFileReadback.verify(
                discovery(), ledger, player, plan, before, 65536, 65536));
    }

    @Test void savedFileWithLostUnrelatedItemIsRejected() throws Exception {
        UUID player = UUID.randomUUID();
        var ledger = new GrantLedger(temp.resolve("ledger3"));
        var grant = ledger.reserve(player, 5000, "console");
        var plan = plan(ledger, player, grant);
        Path file = write(player, GrantReceiptState.empty(), false, "minecraft:stone");
        var before = PlayerDataReader.read(file, 65536, 65536);
        write(player, plan.nextReceipt(), true, "minecraft:dirt");
        assertThrows(IOException.class, () -> SavedGrantDeliveryFileReadback.verify(
                discovery(), ledger, player, plan, before, 65536, 65536));
    }

    @Test void changedFileAfterReadCannotPassWithOriginalReadOnlyProof() throws Exception {
        UUID player = UUID.randomUUID();
        var ledger = new GrantLedger(temp.resolve("ledger4"));
        var grant = ledger.reserve(player, 5000, "console");
        var plan = plan(ledger, player, grant);
        Path file = write(player, GrantReceiptState.empty(), false, "minecraft:stone");
        var before = PlayerDataReader.read(file, 65536, 65536);
        write(player, plan.nextReceipt(), true, "minecraft:stone");
        var manifest = discovery();
        assertThrows(IOException.class, () -> SavedGrantDeliveryFileReadback.verify(
                manifest, ledger, player, plan, before, 65536, 65536,
                () -> write(player, GrantReceiptState.empty(), false, "minecraft:stone")));
    }

    @Test void forgedManifestAndUnknownPlayerAreNeverTrusted() throws Exception {
        UUID player = UUID.randomUUID();
        var ledger = new GrantLedger(temp.resolve("ledger5"));
        var grant = ledger.reserve(player, 5000, "console");
        var plan = plan(ledger, player, grant);
        Path file = write(player, GrantReceiptState.empty(), false, "minecraft:stone");
        var before = PlayerDataReader.read(file, 65536, 65536);
        write(player, plan.nextReceipt(), true, "minecraft:stone");
        var fabricated = new WorldStorageDiscovery.Manifest(
                java.util.List.of(), Map.of(player, file));
        assertThrows(IOException.class, () -> SavedGrantDeliveryFileReadback.verify(
                fabricated, ledger, player, plan, before, 65536, 65536));
        assertThrows(IOException.class, () -> SavedGrantDeliveryFileReadback.verify(
                discovery(), ledger, UUID.randomUUID(), plan, before, 65536, 65536));
    }
}
