package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Both LIVE and saved scans must interpret the shipped WCL marker scheme strictly. */
class WclLiveMarkerTest {
    @Test void canonicalChickenPdcIsAnExplicitWwcHolder() throws Exception {
        assertEquals(Optional.of("wwc"), WclLiveMarker.provider(
                "minecraft:chicken",
                Set.of("whatawonderfulchicken:type", "whatawonderfulchicken:genome_id"),
                "wonderful_chicken", null));
    }

    @Test void canonicalWolfPdcIsAnExplicitWwwHolder() throws Exception {
        assertEquals(Optional.of("www"), WclLiveMarker.provider(
                "minecraft:wolf",
                Set.of("whatawonderfulwolf:type", "whatawonderfulwolf:runtime_state"),
                null, "wonderful_wolf"));
    }

    @Test void untaggedVanillaIsNotFabricatedAsAStorageHolder() throws Exception {
        assertEquals(Optional.empty(), WclLiveMarker.provider(
                "minecraft:chicken", Set.of("minecraft:generic", "someotherplugin:value"),
                null, null));
    }

    @Test void unrelatedPrefixIsNotMistakenForARealProviderNamespace() throws Exception {
        assertEquals(Optional.empty(), WclLiveMarker.provider(
                "minecraft:wolf", Set.of("whatawonderfulwolfother:type"), null, null));
    }

    @Test void partialOrInvalidMarkerFailsClosed() {
        assertThrows(IOException.class, () -> WclLiveMarker.provider(
                "minecraft:chicken", Set.of("whatawonderfulchicken:genome_id"), null, null));
        assertThrows(IOException.class, () -> WclLiveMarker.provider(
                "minecraft:wolf", Set.of("whatawonderfulwolf:type"), null, null));
        assertThrows(IOException.class, () -> WclLiveMarker.provider(
                "minecraft:chicken", Set.of("whatawonderfulchicken:type"), "not_wonderful_chicken", null));
    }

    @Test void contradictoryProviderNamespacesOrTypeFieldsFailClosed() {
        assertThrows(IOException.class, () -> WclLiveMarker.provider(
                "minecraft:chicken",
                Set.of("whatawonderfulchicken:type", "whatawonderfulwolf:type"),
                "wonderful_chicken", "wonderful_wolf"));
        assertThrows(IOException.class, () -> WclLiveMarker.provider(
                "minecraft:wolf", Set.of("whatawonderfulwolf:type"),
                "wonderful_chicken", "wonderful_wolf"));
    }

    @Test void speciesMismatchCannotBeAHolderEvenWithExpectedMarker() {
        assertThrows(IOException.class, () -> WclLiveMarker.provider(
                "minecraft:cow", Set.of("whatawonderfulchicken:type"), "wonderful_chicken", null));
        assertThrows(IOException.class, () -> WclLiveMarker.provider(
                "minecraft:chicken", Set.of("whatawonderfulwolf:type"), null, "wonderful_wolf"));
    }
}
