package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WclHybridLiveOverlayTest {
    private static final UUID CHICKEN = UUID.fromString("00000000-0000-0000-0000-000000000071");
    private static final UUID WOLF = UUID.fromString("00000000-0000-0000-0000-000000000072");
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000073");
    private static final SavedLiveSourcePartition.Chunk CHUNK =
            new SavedLiveSourcePartition.Chunk("world", 0, 0);
    private static final HybridLiveSourceStage.ChunkSource VANILLA =
            new HybridLiveSourceStage.ChunkSource(CHUNK, List.of(
                    new HybridLiveSourceStage.Value("world/world/1,64,1:0",
                            "world", BigInteger.valueOf(200))));

    private static WclHybridLiveOverlay.Selected selected(UUID entity, String provider,
                                                            String slot, int amount,
                                                            WclOwnerEvidence.Decision owner) {
        return new WclHybridLiveOverlay.Selected(CHUNK, entity, provider,
                owner, Map.of(slot, BigInteger.valueOf(amount)));
    }

    @Test void uniqueWwcShulkerJoinsExactlyTheAlreadySelectedChunkSource() throws Exception {
        var input = selected(CHICKEN, "wwc", "shulker", 10000,
                new WclOwnerEvidence.Decision(WclOwnerEvidence.Kind.OWNER, OWNER));
        var result = WclHybridLiveOverlay.attach(List.of(VANILLA), List.of(input));
        assertEquals(1, result.sources().size());
        var source = (HybridLiveSourceStage.ChunkSource) result.sources().get(0);
        assertEquals(CHUNK, source.chunk());
        assertEquals(VANILLA.id(), source.id());
        assertEquals(2, source.values().size());
        String id = "world/world/entity/" + CHICKEN + "/wcl/wwc/shulker";
        assertEquals(BigInteger.valueOf(10000), source.values().get(1).amount());
        assertEquals(id, source.values().get(1).physicalKey());
        assertEquals(OWNER, result.wclOwners().get(id).owner());
    }

    @Test void displayProjectionCollidingWithShulkerAlwaysHoldsEvenWhenAmountsAgree() {
        String key = "world/world/entity/" + CHICKEN + "/wcl/wwc/shulker";
        var vanilla = new HybridLiveSourceStage.ChunkSource(CHUNK, List.of(
                new HybridLiveSourceStage.Value(key, "world", BigInteger.valueOf(10000))));
        assertThrows(IOException.class, () -> WclHybridLiveOverlay.attach(
                List.of(vanilla), List.of(selected(CHICKEN, "wwc", "shulker", 10000,
                new WclOwnerEvidence.Decision(WclOwnerEvidence.Kind.FREE, null)))));
    }

    @Test void wolfWeaponHeadMirrorsAreNeverAdmittedWithoutIndependentEquivalenceProof() {
        assertThrows(IOException.class, () -> WclHybridLiveOverlay.attach(
                List.of(VANILLA), List.of(selected(WOLF, "www", "weapon", 10000,
                new WclOwnerEvidence.Decision(WclOwnerEvidence.Kind.FREE, null)))));
        assertThrows(IOException.class, () -> WclHybridLiveOverlay.attach(
                List.of(VANILLA), List.of(selected(CHICKEN, "wwc", "head", 10000,
                new WclOwnerEvidence.Decision(WclOwnerEvidence.Kind.FREE, null)))));
    }

    @Test void unknownOwnerIsNotTreatedAsFreeOrZero() {
        assertThrows(IOException.class, () -> WclHybridLiveOverlay.attach(
                List.of(VANILLA), List.of(selected(CHICKEN, "wwc", "shulker", 0,
                new WclOwnerEvidence.Decision(WclOwnerEvidence.Kind.HOLD, null)))));
    }

    @Test void nonexistentLoadedChunkAndDuplicateEntityMustReject() {
        var other = new WclHybridLiveOverlay.Selected(
                new SavedLiveSourcePartition.Chunk("world", 3, 4),
                CHICKEN, "wwc", new WclOwnerEvidence.Decision(WclOwnerEvidence.Kind.FREE, null),
                Map.of("shulker", BigInteger.ONE));
        assertThrows(IOException.class, () -> WclHybridLiveOverlay.attach(
                List.of(VANILLA), List.of(other)));
        var one = selected(CHICKEN, "wwc", "shulker", 10000,
                new WclOwnerEvidence.Decision(WclOwnerEvidence.Kind.FREE, null));
        assertThrows(IOException.class, () -> WclHybridLiveOverlay.attach(
                List.of(VANILLA), List.of(one, one)));
    }

    @Test void verifiedZeroSlotIsRetainedAsEvidenceWithoutIntroducingNewMoney() throws Exception {
        var zero = selected(CHICKEN, "wwc", "shulker", 0,
                new WclOwnerEvidence.Decision(WclOwnerEvidence.Kind.FREE, null));
        var result = WclHybridLiveOverlay.attach(List.of(VANILLA), List.of(zero));
        assertEquals(VANILLA.values(), result.sources().get(0).values());
        assertEquals(Map.of(), result.wclOwners());
    }

    @Test void malformedProviderOrNegativeValueIsRejected() {
        assertThrows(IOException.class, () -> WclHybridLiveOverlay.attach(
                List.of(VANILLA), List.of(selected(CHICKEN, "wwc", "cargo:0", 500,
                new WclOwnerEvidence.Decision(WclOwnerEvidence.Kind.FREE, null)))));
        assertThrows(IllegalArgumentException.class, () ->
                new WclHybridLiveOverlay.Selected(CHUNK, CHICKEN, "wwc",
                        new WclOwnerEvidence.Decision(WclOwnerEvidence.Kind.FREE, null),
                        Map.of("shulker", BigInteger.valueOf(-1))));
    }

    @Test void provenNativeWolfMirrorIsPresentExactlyOnceAndRetainsOwnerEvidence() throws Exception {
        String physical = "world/world/entity/" + WOLF + "/equipment/mainhand";
        var vanilla = new HybridLiveSourceStage.ChunkSource(CHUNK, List.of(
                new HybridLiveSourceStage.Value(physical, "world", BigInteger.valueOf(10000))));
        var wolf = new WclHybridLiveOverlay.Selected(CHUNK, WOLF, "www",
                new WclOwnerEvidence.Decision(WclOwnerEvidence.Kind.OWNER, OWNER),
                Map.of("weapon", BigInteger.valueOf(10000), "cargo:7", BigInteger.valueOf(5000)),
                Set.of("weapon"));
        var overlay = WclHybridLiveOverlay.attach(List.of(vanilla), List.of(wolf));
        assertEquals(1, overlay.sources().size());
        var contents = overlay.sources().get(0).values();
        assertEquals(2, contents.size());
        assertEquals(1, contents.stream().filter(v -> v.physicalKey().equals(physical)).count());
        assertEquals(BigInteger.valueOf(15000),
                contents.stream().map(HybridLiveSourceStage.Value::amount)
                        .reduce(BigInteger.ZERO, BigInteger::add));
        assertEquals(Set.of(physical), overlay.mirroredPhysicalKeys());
        assertEquals(OWNER, overlay.wclOwners().get(physical).owner());
    }

    @Test void proofWithoutExactCapturedNativeAmountOrPositionMustReject() {
        String physical = "world/world/entity/" + WOLF + "/equipment/mainhand";
        var mismatch = new HybridLiveSourceStage.ChunkSource(CHUNK, List.of(
                new HybridLiveSourceStage.Value(physical, "world", BigInteger.valueOf(1000))));
        var proven = new WclHybridLiveOverlay.Selected(CHUNK, WOLF, "www",
                new WclOwnerEvidence.Decision(WclOwnerEvidence.Kind.FREE, null),
                Map.of("weapon", BigInteger.valueOf(10000)), Set.of("weapon"));
        assertThrows(IOException.class, () ->
                WclHybridLiveOverlay.attach(List.of(mismatch), List.of(proven)));
        assertThrows(IOException.class, () ->
                WclHybridLiveOverlay.attach(List.of(VANILLA), List.of(proven)));
    }

    @Test void positiveMirrorDoesNotInheritUnverifiedOrForeignProof() {
        var invalid = new WclHybridLiveOverlay.Selected(CHUNK, WOLF, "www",
                new WclOwnerEvidence.Decision(WclOwnerEvidence.Kind.FREE, null),
                Map.of("weapon", BigInteger.valueOf(10000)), Set.of("cargo:7"));
        assertThrows(IOException.class, () ->
                WclHybridLiveOverlay.attach(List.of(VANILLA), List.of(invalid)));
        var zeroProof = new WclHybridLiveOverlay.Selected(CHUNK, WOLF, "www",
                new WclOwnerEvidence.Decision(WclOwnerEvidence.Kind.FREE, null),
                Map.of("weapon", BigInteger.ZERO), Set.of("weapon"));
        assertThrows(IOException.class, () ->
                WclHybridLiveOverlay.attach(List.of(VANILLA), List.of(zeroProof)));
    }
}
