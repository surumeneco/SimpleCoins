package co.surumene.simplecoins.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.zip.ZipFile;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

class NexoResourceInstallerTest {
    @TempDir Path temp;

    @Test void stagesExactlyTheNineCoinAssetsAndUpdatesOnlyWhenChanged() throws Exception {
        var names = new EnumMap<CoinDenomination, String>(CoinDenomination.class);
        for (var d : CoinDenomination.values()) names.put(d, d.value() + " Coin");
        var artifacts = JavaCurrencyPack.build(temp.resolve("SimpleCoins"), names);
        Path nexo = temp.resolve("Nexo");
        Path dest = nexo.resolve("pack/external_packs/simplecoins-generated.zip");
        Path unrelated = dest.resolveSibling("other.zip");
        Files.createDirectories(unrelated.getParent());
        Files.writeString(unrelated, "other plugin pack");
        assertTrue(NexoResourceInstaller.stage(nexo, artifacts.javaArchive()));
        assertArrayEquals(Files.readAllBytes(artifacts.javaArchive()), Files.readAllBytes(dest));
        try (var zip = new ZipFile(dest.toFile())) {
            for (var d : CoinDenomination.values()) {
                String id = "coin_" + d.value();
                assertNotNull(zip.getEntry("assets/simplecoins/items/" + id + ".json"));
                assertNotNull(zip.getEntry("assets/simplecoins/models/item/" + id + ".json"));
                assertNotNull(zip.getEntry("assets/simplecoins/textures/item/" + id + ".png"));
            }
        }
        assertFalse(NexoResourceInstaller.stage(nexo, artifacts.javaArchive()));
        Files.copy(temp.resolve("SimpleCoins/images/5 Coin.png"),
                temp.resolve("SimpleCoins/images/1 Coin.png"),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        artifacts = JavaCurrencyPack.build(temp.resolve("SimpleCoins"), names);
        assertTrue(NexoResourceInstaller.stage(nexo, artifacts.javaArchive()));
        assertArrayEquals(Files.readAllBytes(artifacts.javaArchive()), Files.readAllBytes(dest));
        assertEquals("other plugin pack", Files.readString(unrelated));
        assertFalse(NexoResourceInstaller.stage(nexo, artifacts.javaArchive()));
    }

    @Test void refusesAnUnownedOrModifiedImportWithoutReplacingIt() throws Exception {
        var names = new EnumMap<CoinDenomination, String>(CoinDenomination.class);
        for (var d : CoinDenomination.values()) names.put(d, d.value() + " Coin");
        Path archive = JavaCurrencyPack.build(temp.resolve("SimpleCoins"), names).javaArchive();
        Path nexo = temp.resolve("Nexo");
        Path dest = nexo.resolve("pack/external_packs/simplecoins-generated.zip");
        Files.createDirectories(dest.getParent());
        Files.writeString(dest, "unowned");
        assertThrows(IOException.class, () -> NexoResourceInstaller.stage(nexo, archive));
        assertEquals("unowned", Files.readString(dest));
        Files.delete(dest);
        assertTrue(NexoResourceInstaller.stage(nexo, archive));
        Files.writeString(dest, "modified");
        assertThrows(IOException.class, () -> NexoResourceInstaller.stage(nexo, archive));
        assertEquals("modified", Files.readString(dest));
    }
}
