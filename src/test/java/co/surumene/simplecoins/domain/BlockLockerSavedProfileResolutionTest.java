package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BlockLockerSavedProfileResolutionTest {
    private static final UUID OWNER = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID OTHER = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final Set<String> PRIVATE = Set.of("[Private]");
    private static final Set<String> MORE = Set.of("[More Users]");
    private static final Set<String> EVERYONE = Set.of("Everyone");
    private static final BlockLockerSavedProfileResolution.DisplayDecoder DISPLAY = raw -> raw;

    private static Map<String,Object> profile(String name, UUID uuid) {
        return Map.of("blocklocker:n",name,"blocklocker:u",
            new long[] {uuid.getMostSignificantBits(),uuid.getLeastSignificantBits()});
    }
    private static Map<String,Object> sign(String ownerDisplay, String backUser, Map<String,Object> pdc) {
        return Map.of(
            "front_text",Map.of("messages",List.of("[Private]",ownerDisplay,"","")),
            "back_text",Map.of("messages",List.of("[More Users]",backUser,"","")),
            "PublicBukkitValues",pdc);
    }
    private static BlockLockerSavedProfileResolution.SignProfiles read(Map<String,Object> saved) throws IOException {
        return BlockLockerSavedProfileResolution.read(saved,PRIVATE,MORE,EVERYONE,DISPLAY).orElseThrow();
    }

    @Test void savedUuidMatchesDisplayedFirstProfileButNotOtherProfiles() throws Exception {
        var s=sign("Alice","",Map.of("blocklocker:header","PRIVATE",
            "blocklocker:profile_1",profile("Alice",OWNER),
            "blocklocker:profile_2",profile("Bob",OTHER)));
        var r=read(s);
        assertEquals(OWNER,r.primaryOwner().orElseThrow());
        assertTrue(r.everyoneAbsenceVerified());
        assertFalse(r.everyoneEffective());
    }

    @Test void editedVisibleNameInvalidatesSavedUuid() throws Exception {
        var s=sign("Other","",Map.of("blocklocker:header","PRIVATE",
            "blocklocker:profile_1",profile("Alice",OWNER)));
        assertTrue(read(s).primaryOwner().isEmpty(),
            "BlockLocker uses edited display over the stored profile");
    }

    @Test void enabledBacksideEveryoneOverridesStaleSavedPlayerProfile() throws Exception {
        var s=sign("Alice","[Everyone]",Map.of("blocklocker:header","PRIVATE",
            "blocklocker:profile_1",profile("Alice",OWNER),
            "blocklocker:profile_4",profile("OldName",OTHER)));
        var r=read(s);
        assertEquals(OWNER,r.primaryOwner().orElseThrow());
        assertTrue(r.everyoneEffective());
        assertFalse(r.everyoneAbsenceVerified());
    }

    @Test void inactiveBacksideAndStalePdcEveryoneAreNotEffective() throws Exception {
        var s=Map.<String,Object>of(
            "front_text",Map.of("messages",List.of("[Private]","Alice","","")),
            "back_text",Map.of("messages",List.of("","[Everyone]","","")),
            "PublicBukkitValues",Map.of("blocklocker:header","PRIVATE",
                "blocklocker:profile_1",profile("Alice",OWNER),
                "blocklocker:profile_2",Map.of("blocklocker:e",(byte)1),
                "blocklocker:profile_4",Map.of("blocklocker:e",(byte)1)));
        var r=read(s);
        assertEquals(OWNER,r.primaryOwner().orElseThrow());
        assertFalse(r.everyoneEffective());
        assertTrue(r.everyoneAbsenceVerified());
    }

    @Test void locketteProDisplayUuidWinsAndLegacyDoesNotInventOwner() throws Exception {
        var saved=sign("User#"+OTHER,"",Map.of("blocklocker:header","PRIVATE",
            "blocklocker:profile_1",profile("Alice",OWNER)));
        assertEquals(OTHER,read(saved).primaryOwner().orElseThrow());
        var legacy=Map.<String,Object>of("Text1","[Private]","Text2","Alice",
            "Text3","[Everyone]","Text4","");
        assertTrue(read(legacy).primaryOwner().isEmpty());
        assertTrue(read(legacy).everyoneEffective());
    }

    @Test void missingModernBackFaceAndHeaderContradictionHold() {
        assertThrows(IOException.class,()->read(Map.of(
            "front_text",Map.of("messages",List.of("[Private]","Alice","","")),
            "PublicBukkitValues",Map.of("blocklocker:header","PRIVATE"))));
        assertThrows(IOException.class,()->read(Map.of(
            "front_text",Map.of("messages",List.of("[More Users]","Alice","","")),
            "back_text",Map.of("messages",List.of("","","","")),
            "PublicBukkitValues",Map.of("blocklocker:header","PRIVATE"))));
    }
}
