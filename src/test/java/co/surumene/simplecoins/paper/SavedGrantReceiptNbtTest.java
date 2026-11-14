package co.surumene.simplecoins.paper;

import static org.junit.jupiter.api.Assertions.*;

import co.surumene.simplecoins.domain.GrantLedger;
import co.surumene.simplecoins.domain.GrantReceiptState;
import co.surumene.simplecoins.domain.PlayerDataReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SavedGrantReceiptNbtTest {
    @TempDir Path temp;
    private static final String KEY = "simplecoins:grant_receipt_v1";

    private static Map<String, Object> nbt(GrantReceiptState state) {
        return Map.of("BukkitValues", Map.of(KEY,
                PaperGrantReceiptPdc.toSavedBytes(state)));
    }

    @Test void validDurablePartialStateCanBeReadFromPaperPlayerdataMap() throws Exception {
        UUID player = UUID.randomUUID();
        var ledger = new GrantLedger(temp.resolve("ledger"));
        var grant = ledger.reserve(player, 5200, "console");
        var active = GrantReceiptState.empty().begin(ledger, player, grant);
        var partial = active.advance(ledger, player, grant,
                co.surumene.simplecoins.domain.GrantDeliveryBatchPlanner.next(grant, 0, 1));
        assertEquals(partial, SavedGrantReceiptNbt.readAndVerify(nbt(partial), ledger, player));
        assertEquals(GrantReceiptState.empty(),
                SavedGrantReceiptNbt.readAndVerify(Map.of(), ledger, player));
    }

    @Test void corruptedMissingOrForeignPlayerReceiptCannotBecomeClear() throws Exception {
        UUID player = UUID.randomUUID();
        var ledger = new GrantLedger(temp.resolve("corrupt"));
        var grant = ledger.reserve(player, 5200, "console");
        var active = GrantReceiptState.empty().begin(ledger, player, grant);
        assertThrows(IOException.class,
                () -> SavedGrantReceiptNbt.readAndVerify(
                        Map.of("BukkitValues", "not-a-compound"), ledger, player));
        assertThrows(IOException.class,
                () -> SavedGrantReceiptNbt.readAndVerify(
                        Map.of("BukkitValues", Map.of(KEY, "wrong-type")), ledger, player));
        byte[] altered = PaperGrantReceiptPdc.toSavedBytes(active);
        altered[20] ^= 1;
        assertThrows(IOException.class,
                () -> SavedGrantReceiptNbt.readAndVerify(
                        Map.of("BukkitValues", Map.of(KEY, altered)), ledger, player));
        assertThrows(IOException.class,
                () -> SavedGrantReceiptNbt.readAndVerify(nbt(active), ledger, UUID.randomUUID()));
        var fakeProgress = new GrantReceiptState(0, grant.sequence(), grant.id(), 100);
        assertThrows(IOException.class,
                () -> SavedGrantReceiptNbt.readAndVerify(nbt(fakeProgress), ledger, player));
        ledger.acknowledge(grant, grant.sequence());
        assertThrows(IOException.class,
                () -> SavedGrantReceiptNbt.readAndVerify(Map.of(), ledger, player));
    }

    @Test void actualCompressedPlayerdataNbtDecodesExactlyOneSavedReceipt() throws Exception {
        UUID player = UUID.randomUUID();
        var ledger = new GrantLedger(temp.resolve("saved-disk"));
        var grant = ledger.reserve(player, 5000, "console");
        var active = GrantReceiptState.empty().begin(ledger, player, grant);
        var progress = active.advance(ledger, player, grant,
                co.surumene.simplecoins.domain.GrantDeliveryBatchPlanner.next(grant, 0, 1));
        byte[] encoded = PaperGrantReceiptPdc.toSavedBytes(progress);
        var nbtBytes = new ByteArrayOutputStream();
        try (var output = new DataOutputStream(nbtBytes)) {
            output.writeByte(10); output.writeUTF("");
            output.writeByte(10); output.writeUTF("BukkitValues");
            output.writeByte(7); output.writeUTF(KEY);
            output.writeInt(encoded.length); output.write(encoded);
            output.writeByte(0); // end BukkitValues
            output.writeByte(0); // end player root
        }
        Path playerdata = temp.resolve("playerdata/" + player + ".dat");
        Files.createDirectories(playerdata.getParent());
        try (var gzip = new GZIPOutputStream(Files.newOutputStream(playerdata))) {
            gzip.write(nbtBytes.toByteArray());
        }
        var saved = PlayerDataReader.read(playerdata, 65536, 65536);
        assertEquals(progress, SavedGrantReceiptNbt.readAndVerify(saved, ledger, player));
    }
}
