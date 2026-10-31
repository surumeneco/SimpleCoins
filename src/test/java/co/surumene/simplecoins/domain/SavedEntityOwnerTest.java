package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SavedEntityOwnerTest {
    @Test void extractsVanillaTameOwnerUuidWithoutAssumingPlayerRegistration() throws Exception {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-00000000002a");
        Map<String, Object> wolf = Map.of("id", "minecraft:wolf",
                "UUID", new int[]{0, 0, 0, 70}, "Owner", new int[]{0, 0, 0, 42});
        Map<String, Object> cat = Map.of("id", "minecraft:cat",
                "UUID", new int[]{0, 0, 0, 71});
        assertEquals(Map.of("00000000-0000-0000-0000-000000000046", owner),
                SavedEntityOwner.extract(Map.of("Entities", List.of(wolf, cat))));
    }
}
