package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Explicit selection of disk or memory as the ONE source for each economic
 * holder. A chunk's terrain (REGION) and entities (ENTITY) are separate saved
 * records, but BOTH are shadowed by one loaded live Chunk.
 *
 * This pure routing/coverage transaction does NOT freeze Paper, prove
 * source generation stability, stage money or authorize admission. The caller
 * must construct the selected live inventory from a stable main-thread sample
 * and abort if any world/Player generation changes before publication.
 */
public final class SavedLiveSourcePartition {
    public enum Authority { SAVED, LIVE }

    public record Chunk(String world, int x, int z) {
        public Chunk {
            if (world == null || world.isBlank())
                throw new IllegalArgumentException("A chunk requires a real world name");
        }
    }

    private record DiskChunk(Chunk chunk, WorldStorageDiscovery.Kind kind) {}

    /** This proves only that a declared set was routed and seen, NOT all assets. */
    public record Coverage(int savedDiskChunks, int shadowedDiskChunks,
                           int savedDiskPlayers, int shadowedDiskPlayers,
                           int liveChunks, int livePlayers) {
        public boolean isCompleteSettlementSource() { return false; }
    }

    private final Set<Chunk> selectedLiveChunks;
    private final Set<UUID> selectedLivePlayers;
    private final Set<DiskChunk> seenSavedChunks = new HashSet<>();
    private final Set<UUID> seenSavedPlayers = new HashSet<>();
    private final Set<Chunk> seenLiveChunks = new HashSet<>();
    private final Set<UUID> seenLivePlayers = new HashSet<>();
    private int savedChunks, shadowedChunks, savedPlayers, shadowedPlayers;
    private boolean rejected;
    private boolean completed;

    public SavedLiveSourcePartition(Collection<Chunk> loadedChunks,
                                    Collection<UUID> onlinePlayers) {
        Objects.requireNonNull(loadedChunks, "live chunk declaration");
        Objects.requireNonNull(onlinePlayers, "online Player declaration");
        Set<Chunk> chunkCopy = new HashSet<>();
        for (Chunk chunk : loadedChunks) {
            if (chunk == null || !chunkCopy.add(chunk))
                throw new IllegalArgumentException("Duplicate or null loaded chunk");
        }
        Set<UUID> playerCopy = new HashSet<>();
        for (UUID player : onlinePlayers) {
            if (player == null || !playerCopy.add(player))
                throw new IllegalArgumentException("Duplicate or null online Player");
        }
        selectedLiveChunks = Set.copyOf(chunkCopy);
        selectedLivePlayers = Set.copyOf(playerCopy);
    }

    /**
     * Call exactly once for each chunk physically present in the saved Anvil
     * manifest, whether or not it will be counted. This does NOT bypass
     * validation or SHA checks of the source region and external sidecars.
     */
    public Authority savedChunk(String world, int x, int z,
                                WorldStorageDiscovery.Kind kind) throws IOException {
        ensureOpen();
        if (kind == null) throw refuse("Null saved Anvil source kind");
        final Chunk chunk;
        try { chunk = new Chunk(world, x, z); }
        catch (RuntimeException invalid) {
            rejected = true;
            throw invalid;
        }
        if (!seenSavedChunks.add(new DiskChunk(chunk, kind)))
            throw refuse("Repeated saved Anvil chunk " + chunk + " kind " + kind);
        if (selectedLiveChunks.contains(chunk)) {
            shadowedChunks = Math.addExact(shadowedChunks, 1);
            return Authority.LIVE;
        }
        savedChunks = Math.addExact(savedChunks, 1);
        return Authority.SAVED;
    }

    /** A saved playerdata can never be counted twice with online Player data. */
    public Authority savedPlayer(UUID player) throws IOException {
        ensureOpen();
        if (player == null) throw refuse("Null saved Player UUID");
        if (!seenSavedPlayers.add(player))
            throw refuse("Repeated saved playerdata: " + player);
        if (selectedLivePlayers.contains(player)) {
            shadowedPlayers = Math.addExact(shadowedPlayers, 1);
            return Authority.LIVE;
        }
        savedPlayers = Math.addExact(savedPlayers, 1);
        return Authority.SAVED;
    }

    /** Call only after successfully reading the selected live Chunk. */
    public void liveChunk(Chunk chunk) throws IOException {
        ensureOpen();
        if (chunk == null || !selectedLiveChunks.contains(chunk))
            throw refuse("Unexpected live Chunk: " + chunk);
        if (!seenLiveChunks.add(chunk))
            throw refuse("Repeated live Chunk: " + chunk);
    }

    /** Call only after successfully reading the selected online Player. */
    public void livePlayer(UUID player) throws IOException {
        ensureOpen();
        if (player == null || !selectedLivePlayers.contains(player))
            throw refuse("Unexpected live Player: " + player);
        if (!seenLivePlayers.add(player))
            throw refuse("Repeated live Player: " + player);
    }

    /**
     * Require exact coverage of all explicitly selected memory sources.
     * Disk-source discovery, mutation-generation checks and all asset-value
     * validators remain the coordinator's mandatory separate obligations.
     */
    public Coverage finish() throws IOException {
        ensureOpen();
        if (!seenLiveChunks.equals(selectedLiveChunks)) {
            var missing = new HashSet<>(selectedLiveChunks);
            missing.removeAll(seenLiveChunks);
            throw refuse("Missing required loaded Chunk sources: " + missing);
        }
        if (!seenLivePlayers.equals(selectedLivePlayers)) {
            var missing = new HashSet<>(selectedLivePlayers);
            missing.removeAll(seenLivePlayers);
            throw refuse("Missing required online Player sources: " + missing);
        }
        completed = true;
        return new Coverage(savedChunks, shadowedChunks, savedPlayers,
                shadowedPlayers, seenLiveChunks.size(), seenLivePlayers.size());
    }

    private void ensureOpen() {
        if (rejected || completed)
            throw new IllegalStateException("Selected source generation is failed or complete");
    }

    private IOException refuse(String reason) {
        rejected = true;
        return new IOException(reason);
    }
}
