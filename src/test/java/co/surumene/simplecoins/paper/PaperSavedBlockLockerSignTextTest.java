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
    @Test void decodeActualSavedComponentJsonToDisplayedBlockLockerLines() throws Exception {
        assertEquals("[Private]", PaperSavedBlockLockerSignText.read("{\"text\":\"[Private]\"}"));
        assertEquals("Alice", PaperSavedBlockLockerSignText.read("{\"text\":\"Alice\"}"));
        assertEquals("[Everyone]", PaperSavedBlockLockerSignText.read("{\"text\":\"[Everyone]\"}"));
    }
    @Test void malformedJsonIsAnErrorNotUnprotectedEvidence() {
        assertThrows(IOException.class, () -> PaperSavedBlockLockerSignText.read("{bad-json"));
        assertThrows(IOException.class, () -> PaperSavedBlockLockerSignText.read(null));
    }
    @Test void savedPdcAndRealJsonWithBacksideEveryoneStayDistinct() throws Exception {
        UUID uuid = UUID.fromString("11111111-1111-4111-8111-111111111111");
        var sign = Map.<String,Object>of(
            "front_text", Map.of("messages",List.of(
                "{\"text\":\"[Private]\"}", "{\"text\":\"Alice\"}",
                "{\"text\":\"\"}", "{\"text\":\"\"}")),
            "back_text", Map.of("messages",List.of(
                "{\"text\":\"[More Users]\"}", "{\"text\":\"[Everyone]\"}",
                "{\"text\":\"\"}", "{\"text\":\"\"}")),
            "PublicBukkitValues", Map.of(
                "blocklocker:header","PRIVATE",
                "blocklocker:profile_1",Map.of(
                    "blocklocker:n","Alice",
                    "blocklocker:u",new long[]{uuid.getMostSignificantBits(),uuid.getLeastSignificantBits()})));
        var result=BlockLockerSavedProfileResolution.read(
            sign,Set.of("[Private]"),Set.of("[More Users]"),Set.of("Everyone"),
            PaperSavedBlockLockerSignText.jsonComponent()).orElseThrow();
        assertEquals(uuid,result.primaryOwner().orElseThrow());
        assertTrue(result.everyoneEffective());
    }
}
