package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SavedBlockLockerSettlementOwnerLookupTest {
    @TempDir Path directory;
    private static final UUID OWNER=UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID OTHER=UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final SavedBlockLockerOwnerCandidate.ExternalProof COMPLETE =
        new SavedBlockLockerOwnerCandidate.ExternalProof(true,true,true,true,false);
    private static SavedBlockLockerChestSigns.Position p(int x) {
        return new SavedBlockLockerChestSigns.Position(x,65,4);
    }
    private static SavedBlockLockerChestSigns.Observation protectedOwner(int x) {
        return new SavedBlockLockerChestSigns.Observation(List.of(p(x)),
            List.of(new SavedBlockLockerChestSigns.AttachedSign(
                new SavedBlockLockerChestSigns.Position(x,65,5),
                new BlockLockerSavedProfileResolution.SignProfiles(
                    BlockLockerHeaderMatcher.Type.PRIVATE,Optional.of(OWNER),false,true))));
    }
    private static SavedBlockLockerPhysicalOwnerIndex index() throws IOException {
        var b=new SavedBlockLockerPhysicalOwnerIndex.Builder();
        b.accept("world",protectedOwner(6),COMPLETE);
        return b.finish();
    }
    private WorldStorageDiscovery.Manifest discover(boolean ownerHasPlayerdata) throws IOException {
        Path world=directory.resolve("world");
        Files.createDirectories(world);
        if (ownerHasPlayerdata) {
            Path file=world.resolve("playerdata").resolve(OWNER+".dat");
            Files.createDirectories(file.getParent());
            Files.writeString(file,"discovery fixture; no parsing or writer proof");
        }
        return WorldStorageDiscovery.discover(Map.of("world",world),
            new WorldScope(true,Set.of(),Set.of()),100);
    }
    private static BanSourceCoverage ban(BanEvidence.Status ownerStatus) throws IOException {
        return BanSourceCoverage.capture(Set.of("paper-profile","external"),
            Map.of(OWNER,List.of(
                new BanEvidence.Source("paper-profile",ownerStatus),
                new BanEvidence.Source("external",BanEvidence.Status.CLEAR))));
    }

    @Test void positivePhysicalOwnerUsesIndependentBanAndPlayerdataForSettlementStaging() throws Exception {
        var l=SavedBlockLockerSettlementOwnerLookup.fromIndex(
            index(),discover(true),ban(BanEvidence.Status.CLEAR));
        var owner=l.resolve("world","6,65,4");
        assertEquals(OWNER,owner.owner());
        assertFalse(owner.everyoneProtected());
        assertFalse(owner.currentlyBanned());
        assertTrue(owner.hasPlayerdata());
        assertEquals(AssetOwner.Kind.PERSONAL,AssetOwner.resolve(owner.owner(),
            owner.everyoneProtected(),owner.currentlyBanned(),owner.hasPlayerdata()).kind());
    }

    @Test void banAndMissingPlayerdataStayDeadNotFree() throws Exception {
        var banLookup=SavedBlockLockerSettlementOwnerLookup.fromIndex(index(),discover(true),
            ban(BanEvidence.Status.BANNED));
        var banned=banLookup.resolve("world","6,65,4");
        assertTrue(banned.currentlyBanned());
        assertTrue(banned.hasPlayerdata());
        assertEquals(AssetOwner.Kind.DEAD,AssetOwner.resolve(banned.owner(),
            banned.everyoneProtected(),banned.currentlyBanned(),banned.hasPlayerdata()).kind());

        var missingData=SavedBlockLockerSettlementOwnerLookup.fromIndex(index(),discover(false),
            ban(BanEvidence.Status.CLEAR)).resolve("world","6,65,4");
        assertEquals(OWNER,missingData.owner());
        assertFalse(missingData.hasPlayerdata());
        assertEquals(AssetOwner.DeadReason.MISSING_PLAYERDATA,
            AssetOwner.resolve(missingData.owner(),missingData.everyoneProtected(),
                missingData.currentlyBanned(),missingData.hasPlayerdata()).reason());
    }

    @Test void noIndexedBlockAndIncompleteBanCanNeverBecomeFreeOrClear() throws Exception {
        var ownerLookup=SavedBlockLockerSettlementOwnerLookup.fromIndex(
            index(),discover(true),ban(BanEvidence.Status.CLEAR));
        assertThrows(IOException.class,()->ownerLookup.resolve("world","99,65,4"));
        assertThrows(IOException.class,()->ownerLookup.resolve("another-world","6,65,4"));
        assertThrows(IOException.class,()->ownerLookup.resolve("world","06,65,4"));
        var unknownBan=BanSourceCoverage.capture(Set.of("paper-profile"),Map.of());
        assertThrows(IOException.class,()->SavedBlockLockerSettlementOwnerLookup.fromIndex(
            index(),discover(true),unknownBan));
    }

    @Test void manuallyConstructedManifestDoesNotClaimDiscoveredSourceAuthority() throws Exception {
        var manifest=new WorldStorageDiscovery.Manifest(List.of(),Map.of(OWNER,directory.resolve("owner.dat")));
        assertThrows(IOException.class,()->SavedBlockLockerSettlementOwnerLookup.fromIndex(
            index(),manifest,ban(BanEvidence.Status.CLEAR)));
    }

    @Test void everyoneAndProvenNonProtectionProduceDistinctFreeOwnership() throws Exception {
        var builder=new SavedBlockLockerPhysicalOwnerIndex.Builder();
        var shared=new SavedBlockLockerChestSigns.Observation(
            List.of(p(9)),
            List.of(new SavedBlockLockerChestSigns.AttachedSign(
                new SavedBlockLockerChestSigns.Position(9,65,5),
                new BlockLockerSavedProfileResolution.SignProfiles(
                    BlockLockerHeaderMatcher.Type.PRIVATE,Optional.of(OWNER),true,false))));
        builder.accept("world",shared,COMPLETE);
        var unprotected=new SavedBlockLockerChestSigns.Observation(List.of(p(12)),List.of());
        builder.accept("world",unprotected,
            new SavedBlockLockerOwnerCandidate.ExternalProof(true,true,true,true,true));
        var lookup=SavedBlockLockerSettlementOwnerLookup.fromIndex(builder.finish(),
            discover(false),ban(BanEvidence.Status.CLEAR));
        var everyone=lookup.resolve("world","9,65,4");
        assertNull(everyone.owner());
        assertTrue(everyone.everyoneProtected());
        assertEquals(AssetOwner.Kind.FREE,AssetOwner.resolve(everyone.owner(),
            everyone.everyoneProtected(),everyone.currentlyBanned(),everyone.hasPlayerdata()).kind());
        var noProtection=lookup.resolve("world","12,65,4");
        assertNull(noProtection.owner());
        assertFalse(noProtection.everyoneProtected());
        assertEquals(AssetOwner.Kind.FREE,AssetOwner.resolve(noProtection.owner(),
            noProtection.everyoneProtected(),noProtection.currentlyBanned(),noProtection.hasPlayerdata()).kind());
    }
}
