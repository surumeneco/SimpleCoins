package co.surumene.simplecoins.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ManagedResourcesTest {
    @TempDir Path directory;

    @Test void installsReplacesAndRemovesOnlyOwnedOutputs() throws Exception {
        var install = new ManagedResources(directory);
        install.replace(Map.of("packs/simplecoins.mcpack", new byte[]{1}, "custom_mappings/simplecoins.json", new byte[]{2}));
        assertArrayEquals(new byte[]{1}, Files.readAllBytes(directory.resolve("packs/simplecoins.mcpack")));
        install.replace(Map.of("packs/simplecoins.mcpack", new byte[]{3}));
        assertArrayEquals(new byte[]{3}, Files.readAllBytes(directory.resolve("packs/simplecoins.mcpack")));
        assertFalse(Files.exists(directory.resolve("custom_mappings/simplecoins.json")));
        install.replace(Map.of());
        assertFalse(Files.exists(directory.resolve("packs/simplecoins.mcpack")));
    }

    @Test void neverOverwritesUnownedOrUserModifiedData() throws Exception {
        Path file = directory.resolve("packs/simplecoins.mcpack");
        Files.createDirectories(file.getParent());
        Files.write(file, new byte[]{99});
        var install = new ManagedResources(directory);
        assertThrows(java.io.IOException.class, () -> install.replace(Map.of("packs/simplecoins.mcpack", new byte[]{1})));
        assertArrayEquals(new byte[]{99}, Files.readAllBytes(file));
        Files.delete(file);
        install.replace(Map.of("packs/simplecoins.mcpack", new byte[]{1}));
        Files.write(file, new byte[]{99});
        assertThrows(java.io.IOException.class, () -> install.replace(Map.of()));
        assertArrayEquals(new byte[]{99}, Files.readAllBytes(file));
    }

    @Test void refusesPathsOutsideManagedPackDirectories() {
        var install = new ManagedResources(directory);
        assertThrows(java.io.IOException.class, () -> install.replace(Map.of("../config.yml", new byte[]{1})));
    }
}
