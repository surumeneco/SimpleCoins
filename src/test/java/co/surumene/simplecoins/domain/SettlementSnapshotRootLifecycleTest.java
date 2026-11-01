package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** TASK-20261101-005: lost durable root cannot masquerade as generation zero. */
class SettlementSnapshotRootLifecycleTest {
    @TempDir Path directory;

    @Test void deletedRootAfterConstructionMustNotLookLikeEmptyStore() throws Exception {
        Path root = directory.resolve("settlement");
        var store = new SettlementSnapshotStore(root);
        Files.delete(root);
        assertThrows(IOException.class, store::load);
    }

    @Test void replacedRootFileAfterConstructionMustFailClosed() throws Exception {
        Path root = directory.resolve("settlement");
        var store = new SettlementSnapshotStore(root);
        Files.delete(root);
        Files.writeString(root, "not a directory");
        assertThrows(IOException.class, store::load);
    }

    @Test void newlyConstructedRealEmptyDirectoryStillHasNoGeneration() throws Exception {
        var store = new SettlementSnapshotStore(directory.resolve("fresh"));
        assertTrue(store.load().isEmpty());
    }
}
