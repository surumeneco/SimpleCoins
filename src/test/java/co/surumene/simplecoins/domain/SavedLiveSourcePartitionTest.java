package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Saved/live source identity and mandatory coverage are independent of values. */
class SavedLiveSourcePartitionTest {
    private static final UUID ONLINE = UUID.fromString("00000000-0000-0000-0000-000000005111");
    private static final UUID OFFLINE = UUID.fromString("00000000-0000-0000-0000-000000005222");

    @Test void routesBothTerrainAndEntitySavedEntriesToSameLiveChunkWithoutDoubleSource() throws Exception {
        var a = new SavedLiveSourcePartition.Chunk("world", 0, 0);
        var planner = new SavedLiveSourcePartition(List.of(a), List.of());
        assertEquals(SavedLiveSourcePartition.Authority.LIVE,
                planner.savedChunk("world", 0, 0, WorldStorageDiscovery.Kind.REGION));
        assertEquals(SavedLiveSourcePartition.Authority.LIVE,
                planner.savedChunk("world", 0, 0, WorldStorageDiscovery.Kind.ENTITY));
        assertEquals(SavedLiveSourcePartition.Authority.SAVED,
                planner.savedChunk("world", 32, 0, WorldStorageDiscovery.Kind.REGION));
        assertEquals(SavedLiveSourcePartition.Authority.SAVED,
                planner.savedChunk("world", 32, 0, WorldStorageDiscovery.Kind.ENTITY));
        planner.liveChunk(a);
        var report = planner.finish();
        assertEquals(2, report.shadowedDiskChunks());
        assertEquals(2, report.savedDiskChunks());
        assertEquals(1, report.liveChunks());
        assertFalse(report.isCompleteSettlementSource());
        assertThrows(IllegalStateException.class, () -> planner.liveChunk(a));
    }

    @Test void missingLiveSourceCannotBeReportedAsComplete() throws Exception {
        var missing = new SavedLiveSourcePartition.Chunk("world", -5, 17);
        var planner = new SavedLiveSourcePartition(List.of(missing), List.of(ONLINE));
        assertEquals(SavedLiveSourcePartition.Authority.LIVE,
                planner.savedPlayer(ONLINE));
        assertThrows(IOException.class, planner::finish);
        assertThrows(IllegalStateException.class, () -> planner.liveChunk(missing));
    }

    @Test void onlinePlayersShadowDiskButOfflinePlayerdataRemainsSaved() throws Exception {
        var planner = new SavedLiveSourcePartition(List.of(), List.of(ONLINE));
        assertEquals(SavedLiveSourcePartition.Authority.LIVE, planner.savedPlayer(ONLINE));
        assertEquals(SavedLiveSourcePartition.Authority.SAVED, planner.savedPlayer(OFFLINE));
        planner.livePlayer(ONLINE);
        var report = planner.finish();
        assertEquals(1, report.savedDiskPlayers());
        assertEquals(1, report.shadowedDiskPlayers());
        assertEquals(1, report.livePlayers());
    }

    @Test void repeatedDiskSourceOrLiveObservationPoisonsThePlan() throws Exception {
        var planner = new SavedLiveSourcePartition(
                List.of(new SavedLiveSourcePartition.Chunk("w", 0, 0)), List.of());
        planner.savedChunk("w", 0, 0, WorldStorageDiscovery.Kind.REGION);
        assertThrows(IOException.class, () ->
                planner.savedChunk("w", 0, 0, WorldStorageDiscovery.Kind.REGION));
        assertThrows(IllegalStateException.class, planner::finish);

        var other = new SavedLiveSourcePartition(
                List.of(new SavedLiveSourcePartition.Chunk("w", 0, 0)), List.of());
        other.liveChunk(new SavedLiveSourcePartition.Chunk("w", 0, 0));
        assertThrows(IOException.class, () ->
                other.liveChunk(new SavedLiveSourcePartition.Chunk("w", 0, 0)));
        assertThrows(IllegalStateException.class, other::finish);
    }

    @Test void rejectsUnknownOrDuplicatedSelectedSourcesAtInputAndObservation() throws Exception {
        var ref = new SavedLiveSourcePartition.Chunk("world", 7, 7);
        assertThrows(IllegalArgumentException.class, () ->
                new SavedLiveSourcePartition(List.of(ref, ref), List.of()));
        assertThrows(IllegalArgumentException.class, () ->
                new SavedLiveSourcePartition(List.of(), List.of(ONLINE, ONLINE)));
        var planner = new SavedLiveSourcePartition(List.of(ref), List.of(ONLINE));
        assertThrows(IOException.class, () ->
                planner.liveChunk(new SavedLiveSourcePartition.Chunk("other", 7, 7)));
        assertThrows(IllegalStateException.class, planner::finish);
    }

    @Test void differentWorldsAndSignedCoordinatesRemainDistinct() throws Exception {
        var a = new SavedLiveSourcePartition.Chunk("world", -1, 0);
        var b = new SavedLiveSourcePartition.Chunk("other", -1, 0);
        var planner = new SavedLiveSourcePartition(List.of(a, b), List.of());
        assertEquals(SavedLiveSourcePartition.Authority.LIVE,
                planner.savedChunk("world", -1, 0, WorldStorageDiscovery.Kind.REGION));
        assertEquals(SavedLiveSourcePartition.Authority.LIVE,
                planner.savedChunk("other", -1, 0, WorldStorageDiscovery.Kind.REGION));
        assertEquals(SavedLiveSourcePartition.Authority.SAVED,
                planner.savedChunk("world", 1, 0, WorldStorageDiscovery.Kind.REGION));
        planner.liveChunk(a);
        planner.liveChunk(b);
        assertEquals(2, planner.finish().liveChunks());
    }
}
