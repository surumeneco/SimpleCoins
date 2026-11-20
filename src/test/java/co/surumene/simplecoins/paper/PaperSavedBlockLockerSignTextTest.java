package co.surumene.simplecoins.paper;

import static org.junit.jupiter.api.Assertions.*;
import co.surumene.simplecoins.domain.BlockLockerSavedProfileResolution;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaperSavedBlockLockerSignTextTest {
    @Test void readActualPaper262SavedTextWithoutReparsingIt() throws Exception {
        assertEquals("[Private]", PaperSavedBlockLockerSignText.read("[Private]"));
        assertEquals("Alice", PaperSavedBlockLockerSignText.read("Alice"));
        assertEquals("[Everyone]", PaperSavedBlockLockerSignText.read("[Everyone]"));
    }

    @Test void jsonLookingLiteralIsNotAnImplicitPermission() throws Exception {
        // Real 26.2 stores plain visible messages[] text. A player can write
        // these exact characters; they must never become parsed permissions.
        assertEquals("{\"text\":\"[Everyone]\"}",
                PaperSavedBlockLockerSignText.read("{\"text\":\"[Everyone]\"}"));
        assertEquals("{bad-json", PaperSavedBlockLockerSignText.read("{bad-json"));
        assertThrows(IOException.class, () -> PaperSavedBlockLockerSignText.read(null));
    }

    @Test void savedPdcAndNativeBacksideEveryoneStayDistinct() throws Exception {
        UUID uuid = UUID.fromString("11111111-1111-4111-8111-111111111111");
        var sign = Map.<String,Object>of(
            "front_text", Map.of("messages",List.of("[Private]", "Alice", "", "")),
            "back_text", Map.of("messages",List.of("[More Users]", "[Everyone]", "", "")),
            "PublicBukkitValues", Map.of(
                "blocklocker:header","PRIVATE",
                "blocklocker:profile_1",Map.of(
                    "blocklocker:n","Alice",
                    "blocklocker:u",new long[]{uuid.getMostSignificantBits(),uuid.getLeastSignificantBits()})));
        var result=BlockLockerSavedProfileResolution.read(
            sign,Set.of("[Private]"),Set.of("[More Users]"),Set.of("Everyone"),
            PaperSavedBlockLockerSignText.nativeText()).orElseThrow();
        assertEquals(uuid,result.primaryOwner().orElseThrow());
        assertTrue(result.everyoneEffective());
    }
}
