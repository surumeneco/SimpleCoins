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

    @Test void replacementByAnotherRealDirectoryCannotResetGeneration() throws Exception {
        Path root = directory.resolve("settlement");
        var store = new SettlementSnapshotStore(root);
        Files.move(root, directory.resolve("original-settlement"));
        Files.createDirectory(root);
        assertThrows(IOException.class, store::load,
                "the original store instance must remain bound to its physical root");
        var staged = new SettlementAccumulator(
                new WorldScope(true, java.util.Set.of(), java.util.Set.of()),
                java.util.Set.of("source"));
        staged.completeSource("source");
        var first = new SettlementGenerationPublisher.Published(1, staged.commit());
        assertThrows(IOException.class, () -> store.persist(first));
        assertFalse(Files.exists(root.resolve("settlement.snapshot")));
        assertFalse(Files.exists(root.resolve("settlement.initialized")));
    }

    @Test void newlyConstructedRealEmptyDirectoryStillHasNoGeneration() throws Exception {
        var store = new SettlementSnapshotStore(directory.resolve("fresh"));
        assertTrue(store.load().isEmpty());
    }
}
