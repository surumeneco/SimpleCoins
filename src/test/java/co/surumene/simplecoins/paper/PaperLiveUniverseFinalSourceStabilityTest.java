package co.surumene.simplecoins.paper;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

/** A later World/Player read must not make an earlier World capture stale. */
class PaperLiveUniverseFinalSourceStabilityTest {
    private record FakeWorld(World world, AtomicReference<Chunk[]> loaded) {}

    private static FakeWorld fakeWorld() {
        AtomicReference<Chunk[]> loaded = new AtomicReference<>(new Chunk[0]);
        World world = (World) Proxy.newProxyInstance(
                World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getLoadedChunks")) return loaded.get();
                    if (method.getName().equals("toString")) return "fake-world";
                    throw new AssertionError("Unexpected World call: " + method.getName());
                });
        return new FakeWorld(world, loaded);
    }

    private static Chunk chunk(World world, int x, int z, boolean loaded) {
        return (Chunk) Proxy.newProxyInstance(
                Chunk.class.getClassLoader(), new Class<?>[]{Chunk.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getWorld" -> world;
                    case "getX" -> x;
                    case "getZ" -> z;
                    case "isLoaded" -> loaded;
                    case "toString" -> "chunk(" + x + "," + z + ")";
                    default -> throw new AssertionError("Unexpected Chunk call: " + method.getName());
                });
    }

    private static long key(int x, int z) {
        return PaperLoadedWorldAssets.chunkCoordinate(x, z);
    }

    private static PaperLiveUniverseAssets.WorldCapture initial(FakeWorld world, long... coordinates) {
        java.util.HashSet<Long> values = new java.util.HashSet<>();
        for (long coordinate : coordinates) values.add(coordinate);
        return new PaperLiveUniverseAssets.WorldCapture(world.world(), Set.copyOf(values));
    }

    @Test void unchangedEarlierAndLaterWorldsRemainValidAfterPlayerCapture() {
        FakeWorld first = fakeWorld();
        FakeWorld second = fakeWorld();
        first.loaded().set(new Chunk[]{chunk(first.world(), -3, 9, true)});
        second.loaded().set(new Chunk[]{chunk(second.world(), 5, 6, true)});
        assertDoesNotThrow(() -> PaperLiveUniverseAssets.requireWorldSourceSetsStillLoaded(
                List.of(initial(first, key(-3, 9)), initial(second, key(5, 6)))));
    }

    @Test void laterChangeOfPreviouslyScannedWorldInvalidatesFullCapture() {
        FakeWorld first = fakeWorld();
        FakeWorld second = fakeWorld();
        first.loaded().set(new Chunk[]{chunk(first.world(), 9, 9, true)});
        second.loaded().set(new Chunk[]{chunk(second.world(), 5, 6, true)});
        assertThrows(IOException.class, () -> PaperLiveUniverseAssets.requireWorldSourceSetsStillLoaded(
                List.of(initial(first, key(-3, 9)), initial(second, key(5, 6)))));
    }

    @Test void addedAndRemovedEarlierChunkAreRejectedEvenIfOtherWorldIsStable() {
        FakeWorld first = fakeWorld();
        first.loaded().set(new Chunk[]{chunk(first.world(), 1, 1, true),
                chunk(first.world(), 2, 2, true)});
        assertThrows(IOException.class, () -> PaperLiveUniverseAssets.requireWorldSourceSetsStillLoaded(
                List.of(initial(first, key(1, 1)))));

        first.loaded().set(new Chunk[0]);
        assertThrows(IOException.class, () -> PaperLiveUniverseAssets.requireWorldSourceSetsStillLoaded(
                List.of(initial(first, key(1, 1)))));
    }

    @Test void nullUnloadedAndDuplicateChunksNeverMeanAnEmptySource() {
        FakeWorld world = fakeWorld();
        var expected = List.of(initial(world, key(1, 1)));
        world.loaded().set(null);
        assertThrows(IOException.class, () ->
                PaperLiveUniverseAssets.requireWorldSourceSetsStillLoaded(expected));
        world.loaded().set(new Chunk[]{null});
        assertThrows(IOException.class, () ->
                PaperLiveUniverseAssets.requireWorldSourceSetsStillLoaded(expected));
        world.loaded().set(new Chunk[]{chunk(world.world(), 1, 1, false)});
        assertThrows(IOException.class, () ->
                PaperLiveUniverseAssets.requireWorldSourceSetsStillLoaded(expected));
        world.loaded().set(new Chunk[]{chunk(world.world(), 1, 1, true),
                chunk(world.world(), 1, 1, true)});
        assertThrows(IOException.class, () ->
                PaperLiveUniverseAssets.requireWorldSourceSetsStillLoaded(expected));
    }

    @Test void foreignChunkAndZeroSelectedWorldsBehaveSafely() {
        FakeWorld first = fakeWorld();
        FakeWorld other = fakeWorld();
        first.loaded().set(new Chunk[]{chunk(other.world(), 1, 1, true)});
        assertThrows(IOException.class, () -> PaperLiveUniverseAssets.requireWorldSourceSetsStillLoaded(
                List.of(initial(first, key(1, 1)))));
        assertDoesNotThrow(() -> PaperLiveUniverseAssets.requireWorldSourceSetsStillLoaded(
                List.of()));
    }
}
