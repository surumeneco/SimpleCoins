package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.zip.DeflaterOutputStream;
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
        Path world=directory.resolve(ownerHasPlayerdata ? "with-player" : "without-player");
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

    private record ScopedFixture(
            WorldStorageDiscovery.Manifest manifest,
            WorldStorageDiscovery.SourceFile source) {}

    private ScopedFixture selectedRegion(boolean ownerHasPlayerdata) throws IOException {
        Path world=directory.resolve(ownerHasPlayerdata ? "with-player" : "without-player");
        Path region=world.resolve("region/r.0.0.mca");
        Files.createDirectories(region.getParent());
        // This is an identity-scoping fixture, not an actual Anvil read.
        Files.write(region,new byte[8192]);
        var manifest=discover(ownerHasPlayerdata);
        return new ScopedFixture(manifest,manifest.worldFiles().getFirst());
    }

    private static AnvilRegionReader.Chunk physicalChunk() {
        return new AnvilRegionReader.Chunk(0,0,Map.of(
            "xPos",0,"zPos",0,
            "sections",List.of(Map.of("Y",4,"block_states",
                Map.of("palette",List.of(Map.of("Name","minecraft:chest"))))),
            "block_entities",List.of(Map.of(
                "id","minecraft:chest","x",6,"y",65,"z",4))));
    }

    @Test void selectedSavedRegionAndChunkAreRequiredForIndexedOwner() throws Exception {
        var selected=selectedRegion(true);
        var lookup=SavedBlockLockerSettlementOwnerLookup.fromIndex(
            index(),selected.manifest(),ban(BanEvidence.Status.CLEAR));
        var scoped=lookup.sourceScoped();
        var owner=scoped.resolve(selected.source(),physicalChunk(),"6,65,4");
        assertEquals(OWNER,owner.owner());
        assertTrue(owner.hasPlayerdata());
        assertFalse(owner.currentlyBanned());
        assertThrows(IOException.class,()->scoped.resolve("world","6,65,4"),
            "Opt-in source-scoped ownership must never accept missing source/Chunk context");
        assertEquals(owner,lookup.resolve("world","6,65,4"),
            "Legacy already-verified direct lookup remains compatible");
    }

    @Test void selectedSavedSourceCannotBeSubstitutedOrReinterpreted() throws Exception {
        var selected=selectedRegion(false);
        var scoped=SavedBlockLockerSettlementOwnerLookup.fromIndex(index(),
            selected.manifest(),ban(BanEvidence.Status.CLEAR)).sourceScoped();
        assertThrows(IOException.class,()->scoped.resolve(
            new WorldStorageDiscovery.SourceFile("world",directory.resolve("elsewhere/r.0.0.mca"),
                WorldStorageDiscovery.Kind.REGION),physicalChunk(),"6,65,4"));
        assertThrows(IOException.class,()->scoped.resolve(
            new WorldStorageDiscovery.SourceFile("other-world",selected.source().path(),
                WorldStorageDiscovery.Kind.REGION),physicalChunk(),"6,65,4"));
        assertThrows(IOException.class,()->scoped.resolve(
            new WorldStorageDiscovery.SourceFile("world",selected.source().path(),
                WorldStorageDiscovery.Kind.ENTITY),physicalChunk(),"6,65,4"));
        assertThrows(IOException.class,()->scoped.resolve(selected.source(),
            new AnvilRegionReader.Chunk(1,0,physicalChunk().nbt()),"6,65,4"));
        assertThrows(IOException.class,()->scoped.resolve(selected.source(),physicalChunk(),"06,65,4"));
        assertThrows(IOException.class,()->scoped.resolve(selected.source(),physicalChunk(),"6,65"));
        assertThrows(IOException.class,()->scoped.resolve(selected.source(),physicalChunk(),"22,65,4"));
    }

    @Test void malformedMissingOrRepeatedSavedChestNeverInheritsIndexedOwner() throws Exception {
        var selected=selectedRegion(false);
        var scoped=SavedBlockLockerSettlementOwnerLookup.fromIndex(index(),
            selected.manifest(),ban(BanEvidence.Status.CLEAR)).sourceScoped();
        var chest=Map.<String,Object>of("id","minecraft:chest","x",6,"y",65,"z",4);
        var notChest=Map.<String,Object>of("id","minecraft:barrel","x",6,"y",65,"z",4);
        var wrongPos=Map.<String,Object>of("id","minecraft:chest","x",7,"y",65,"z",4);
        for (var entities : List.<List<Map<String,Object>>>of(
                List.of(),List.of(notChest),List.of(wrongPos),List.of(chest,chest),
                List.of(Map.<String,Object>of("id","minecraft:chest","x",6,"y",65)))) {
            var chunk=new AnvilRegionReader.Chunk(0,0,Map.of("block_entities",entities));
            assertThrows(IOException.class,()->scoped.resolve(selected.source(),chunk,"6,65,4"),
                "Only an exact, unique physical saved chest BlockEntity may receive ownership");
        }
        assertThrows(IOException.class,()->scoped.resolve(selected.source(),
            new AnvilRegionReader.Chunk(0,0,Map.of()),"6,65,4"));
    }

    @Test void selectedRegionFilenameMustMatchDecodedChunkCoordinates() throws Exception {
        Path world=directory.resolve("without-player");
        Path region=world.resolve("region/r.1.0.mca");
        Files.createDirectories(region.getParent());
        Files.write(region,new byte[8192]);
        var manifest=discover(false);
        var selected=manifest.worldFiles().getFirst();
        assertEquals("r.1.0.mca",selected.path().getFileName().toString());
        var scoped=SavedBlockLockerSettlementOwnerLookup.fromIndex(index(),manifest,
                ban(BanEvidence.Status.CLEAR)).sourceScoped();
        assertThrows(IOException.class,()->scoped.resolve(selected,physicalChunk(),"6,65,4"),
                "A physically selected but incorrect Region cannot supply this Chunk");
    }

    @Test void orphanChestBlockEntityWithStonePaletteCannotClaimSavedBlockLockerOwner() throws Exception {
        var selected=selectedRegion(false);
        var scoped=SavedBlockLockerSettlementOwnerLookup.fromIndex(index(),
            selected.manifest(),ban(BanEvidence.Status.CLEAR)).sourceScoped();
        var terrain=new java.util.HashMap<String,Object>(physicalChunk().nbt());
        terrain.put("xPos",0);
        terrain.put("zPos",0);
        terrain.put("sections",List.of(Map.of("Y",4,"block_states",
            Map.of("palette",List.of(Map.of("Name","minecraft:stone"))))));
        assertThrows(IOException.class,()->scoped.resolve(selected.source(),
            new AnvilRegionReader.Chunk(0,0,terrain),"6,65,4"),
            "A chest BlockEntity without an actual chest block state is orphaned evidence");
    }

    @Test void actualSelectedAnvilChunkStagesOnlyItsProvenBoundedOwner() throws Exception {
        // Real Anvil/NBT parsing + selected SourceFile traversal, with an
        // intentionally synthetic *prior* BlockLocker index (not G0 proof).
        Path world=directory.resolve("without-player");
        Path file=world.resolve("region/r.0.0.mca");
        Files.createDirectories(file.getParent());
        var data=new ByteArrayOutputStream();
        try (var out=new DataOutputStream(data)) {
            out.writeByte(10); out.writeUTF("");
            out.writeByte(3); out.writeUTF("xPos"); out.writeInt(0);
            out.writeByte(3); out.writeUTF("zPos"); out.writeInt(0);
            // Actual Anvil section: air by default, exactly block(0,64,0)
            // uses palette index 1 (chest), in 4-bit nonspanning packing.
            out.writeByte(9); out.writeUTF("sections");
            out.writeByte(10); out.writeInt(1); // one TAG_Compound section
            out.writeByte(1); out.writeUTF("Y"); out.writeByte(4);
            out.writeByte(10); out.writeUTF("block_states");
            out.writeByte(9); out.writeUTF("palette");
            out.writeByte(10); out.writeInt(2);
            out.writeByte(8); out.writeUTF("Name"); out.writeUTF("minecraft:air");
            out.writeByte(0); // air palette entry
            out.writeByte(8); out.writeUTF("Name"); out.writeUTF("minecraft:chest");
            out.writeByte(10); out.writeUTF("Properties");
            out.writeByte(8); out.writeUTF("facing"); out.writeUTF("north");
            out.writeByte(8); out.writeUTF("type"); out.writeUTF("single");
            out.writeByte(0); // Properties
            out.writeByte(0); // chest palette entry
            out.writeByte(12); out.writeUTF("data"); out.writeInt(256);
            for (int word=0;word<256;word++) out.writeLong(word==0 ? 1L : 0L);
            out.writeByte(0); // block_states
            out.writeByte(0); // section compound
            out.writeByte(9); out.writeUTF("block_entities");
            out.writeByte(10); out.writeInt(1);
            out.writeByte(8); out.writeUTF("id"); out.writeUTF("minecraft:chest");
            out.writeByte(3); out.writeUTF("x"); out.writeInt(0);
            out.writeByte(3); out.writeUTF("y"); out.writeInt(64);
            out.writeByte(3); out.writeUTF("z"); out.writeInt(0);
            out.writeByte(9); out.writeUTF("Items");
            out.writeByte(10); out.writeInt(1);
            out.writeByte(1); out.writeUTF("Slot"); out.writeByte(0);
            out.writeByte(8); out.writeUTF("id"); out.writeUTF("minecraft:paper");
            out.writeByte(3); out.writeUTF("count"); out.writeInt(1);
            out.writeByte(10); out.writeUTF("components");
            out.writeByte(10); out.writeUTF("minecraft:custom_data");
            out.writeByte(10); out.writeUTF("PublicBukkitValues");
            out.writeByte(3); out.writeUTF("simplecoins:denomination"); out.writeInt(5000);
            out.writeByte(0); // PublicBukkitValues
            out.writeByte(0); // custom_data
            out.writeByte(0); // components
            out.writeByte(0); // ItemStack
            out.writeByte(0); // Chest
            out.writeByte(0); // Chunk root
        }
        var zbytes=new ByteArrayOutputStream();
        try (var compressor=new DeflaterOutputStream(zbytes)) {
            compressor.write(data.toByteArray());
        }
        byte[] payload=zbytes.toByteArray();
        byte[] region=new byte[3*4096];
        ByteBuffer.wrap(region).putInt(0,(2<<8)|1);
        ByteBuffer.wrap(region).putInt(8192,payload.length+1);
        region[8196]=2;
        System.arraycopy(payload,0,region,8197,payload.length);
        Files.write(file,region);
        var scope=new WorldScope(true,Set.of(),Set.of());
        var manifest=WorldStorageDiscovery.discover(Map.of("world",world),scope,100);

        var indexBuilder=new SavedBlockLockerPhysicalOwnerIndex.Builder();
        var observed=new SavedBlockLockerChestSigns.Observation(
            List.of(new SavedBlockLockerChestSigns.Position(0,64,0)),
            List.of(new SavedBlockLockerChestSigns.AttachedSign(
                new SavedBlockLockerChestSigns.Position(0,64,1),
                new BlockLockerSavedProfileResolution.SignProfiles(
                    BlockLockerHeaderMatcher.Type.PRIVATE,Optional.of(OWNER),false,true))));
        indexBuilder.accept("world",observed,COMPLETE);
        var lookup=SavedBlockLockerSettlementOwnerLookup.fromIndex(
            indexBuilder.finish(),manifest,ban(BanEvidence.Status.CLEAR));
        var staged=SettlementScanRunner.stageWithBanCoverage(
            manifest,scope,ban(BanEvidence.Status.CLEAR),lookup.sourceScoped(),
            (selectedWorld, chunk)->Map.of(),4096,4096);
        var accounting=staged.pending().commit();
        assertEquals(BigInteger.valueOf(5000),accounting.dead());
        assertEquals(BigInteger.ZERO,accounting.free());
        assertTrue(accounting.personalBalances().isEmpty());
        assertEquals(AssetOwner.DeadReason.MISSING_PLAYERDATA,
            accounting.deadLocations().getFirst().reason());
        assertNotNull(staged.savedContentWitness());
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
