package co.surumene.simplecoins.paper;

import static org.junit.jupiter.api.Assertions.*;

import co.surumene.simplecoins.domain.GrantLedger;
import co.surumene.simplecoins.domain.GrantReceiptState;
import co.surumene.simplecoins.domain.WorldScope;
import co.surumene.simplecoins.domain.WorldStorageDiscovery;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** An observed saved-file set is not permission to issue coins or open admission. */
class SavedGrantReceiptUniverseAuditTest {
    @TempDir Path temp;
    private static final String KEY = "simplecoins:grant_receipt_v1";

    private Path world() throws IOException {
        Path path = temp.resolve("world");
        Files.createDirectories(path);
        return path;
    }

    private WorldStorageDiscovery.Manifest discover() throws IOException {
        return WorldStorageDiscovery.discover(Map.of("world", world()),
                new WorldScope(true, Set.of(), Set.of()), 256);
    }

    private Path save(UUID owner, GrantReceiptState receipt) throws IOException {
        byte[] pdc = PaperGrantReceiptPdc.toSavedBytes(receipt);
        var nbt = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(nbt)) {
            out.writeByte(10); out.writeUTF("");
            out.writeByte(10); out.writeUTF("BukkitValues");
            out.writeByte(7); out.writeUTF(KEY);
            out.writeInt(pdc.length); out.write(pdc);
            out.writeByte(0);
            out.writeByte(0);
        }
        return saveBytes(owner, nbt.toByteArray());
    }

    private Path saveBytes(UUID owner, byte[] nbt) throws IOException {
        Path file = world().resolve("playerdata/" + owner + ".dat");
        Files.createDirectories(file.getParent());
        try (var gzip = new GZIPOutputStream(Files.newOutputStream(file))) {
            gzip.write(nbt);
        }
        return file;
    }

    private static SavedGrantReceiptUniverseAudit.Report audit(
            WorldStorageDiscovery.Manifest manifest, GrantLedger ledger) throws IOException {
        return SavedGrantReceiptUniverseAudit.scan(manifest, ledger, 65536, 65536);
    }

    @Test void acknowledgedGrantWhosePlayerdataDisappearedMustFailClosed() throws Exception {
        var ledger = new GrantLedger(temp.resolve("grants"));
        UUID player = UUID.randomUUID();
        var granted = ledger.reserve(player, 5000, "console");
        ledger.acknowledge(granted, granted.sequence());

        // A vanished authoritative playerdata file must not make the grant
        // look as though it had never been delivered.
        assertThrows(IOException.class, () -> audit(discover(), ledger));
    }

    @Test void rolledBackPlayerdataCannotValidateAcknowledgedGrant() throws Exception {
        var ledger = new GrantLedger(temp.resolve("grants"));
        UUID player = UUID.randomUUID();
        var granted = ledger.reserve(player, 100, "console");
        ledger.acknowledge(granted, granted.sequence());
        save(player, GrantReceiptState.empty());

        assertThrows(IOException.class, () -> audit(discover(), ledger));
    }

    @Test void anOfflinePendingGrantWithoutPlayerdataIsNotAutomaticallyIssued() throws Exception {
        var ledger = new GrantLedger(temp.resolve("grants"));
        ledger.reserve(UUID.randomUUID(), 5200, "console");

        var report = audit(discover(), ledger);
        assertEquals(0, report.playersRead());
        assertEquals(0, report.acknowledgedGrants());
        assertEquals(1, report.pendingGrants());
        assertEquals(0, report.awaitingAcknowledgement());
    }

    @Test void savedCompletedReceiptCanBeObservedBeforeJournalAckWithoutMutation() throws Exception {
        var ledger = new GrantLedger(temp.resolve("grants"));
        UUID player = UUID.randomUUID();
        var granted = ledger.reserve(player, 5200, "console");
        save(player, new GrantReceiptState(granted.sequence(), 0, null, 0));

        var report = audit(discover(), ledger);
        assertEquals(1, report.playersRead());
        assertEquals(0, report.acknowledgedGrants());
        assertEquals(1, report.pendingGrants());
        assertEquals(1, report.awaitingAcknowledgement());
        assertEquals(1, ledger.pending(player).size(),
                "A read-only audit must not acknowledge a pending grant");
    }

    @Test void acknowledgedGrantWithSavedReceiptPassesWithoutReissue() throws Exception {
        var ledger = new GrantLedger(temp.resolve("grants"));
        UUID player = UUID.randomUUID();
        var granted = ledger.reserve(player, 25, "console");
        ledger.acknowledge(granted, granted.sequence());
        save(player, new GrantReceiptState(granted.sequence(), 0, null, 0));

        var report = audit(discover(), ledger);
        assertEquals(1, report.playersRead());
        assertEquals(1, report.acknowledgedGrants());
        assertEquals(0, report.pendingGrants());
        assertEquals(0, report.awaitingAcknowledgement());
    }

    @Test void inventedManifestAndCorruptSavedReceiptCannotClaimSuccessfulAudit()
            throws Exception {
        var ledger = new GrantLedger(temp.resolve("grants"));
        UUID player = UUID.randomUUID();
        Path file = save(player, GrantReceiptState.empty());
        var manual = new WorldStorageDiscovery.Manifest(List.of(), Map.of(player, file));
        assertThrows(IOException.class, () -> audit(manual, ledger));

        byte[] pdc = PaperGrantReceiptPdc.toSavedBytes(GrantReceiptState.empty());
        pdc[21] ^= 1;
        var nbt = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(nbt)) {
            out.writeByte(10); out.writeUTF("");
            out.writeByte(10); out.writeUTF("BukkitValues");
            out.writeByte(7); out.writeUTF(KEY);
            out.writeInt(pdc.length); out.write(pdc);
            out.writeByte(0); out.writeByte(0);
        }
        saveBytes(player, nbt.toByteArray());
        assertThrows(IOException.class, () -> audit(discover(), ledger));
    }
}
