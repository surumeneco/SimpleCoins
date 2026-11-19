package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.BanSourceCoverage;
import co.surumene.simplecoins.domain.HybridLiveSourceStage;
import co.surumene.simplecoins.domain.LiveContainerPhysicalSlot;
import co.surumene.simplecoins.domain.SettlementAccumulator;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;

/**
 * One selected LIVE container slot's BlockLocker 1.15 ownership classification.
 *
 * The amount and physical source key came from a prior actual Paper Chunk scan;
 * this adapter only attaches the current protection and independently supplied
 * BAN/playerdata evidence. It does not rescan currency, prevent other writers,
 * prove one consistent LIVE/SAVED epoch, or authorize settlement publication.
 */
public final class PaperBlockLockerLiveClassifier {
    private PaperBlockLockerLiveClassifier() {}

    /**
     * Resolve only an already captured, world-qualified, loaded Block inventory
     * slot. Never load a new Chunk to discover protection or infer an owner.
     * A failed provider or incomplete owner evidence aborts the candidate.
     */
    public static SettlementAccumulator.Asset classify(
            HybridLiveSourceStage.ChunkSource source,
            HybridLiveSourceStage.Value value,
            World capturedWorld,
            BanSourceCoverage banSources,
            Set<UUID> existingPlayerdata) throws IOException {
        LiveContainerPhysicalSlot slot = LiveContainerPhysicalSlot.parse(source, value);
        if (!Bukkit.isPrimaryThread())
            throw new IOException("LIVE BlockLocker classification requires main thread");
        if (capturedWorld == null || !capturedWorld.getName().equals(source.chunk().world())
                || Bukkit.getWorld(capturedWorld.getName()) != capturedWorld
                || Bukkit.getWorld(capturedWorld.getUID()) != capturedWorld)
            throw new IOException("Captured LIVE World identity is not Bukkit authoritative");
        if (!capturedWorld.isChunkLoaded(source.chunk().x(), source.chunk().z()))
            throw new IOException("Selected LIVE BlockLocker Chunk is not loaded");
        if (slot.y() < capturedWorld.getMinHeight() || slot.y() >= capturedWorld.getMaxHeight())
            throw new IOException("LIVE BlockLocker container Y is outside World bounds");
        if (!source.values().contains(value))
            throw new IOException("Currency slot is not in the selected LIVE Chunk source");

        Block block = capturedWorld.getBlockAt(slot.x(), slot.y(), slot.z());
        BlockState state = block.getState();
        if (!(state instanceof Container container))
            throw new IOException("Captured LIVE currency is not in a Block container");
        Inventory inventory = state instanceof Chest chest
                ? chest.getBlockInventory() : container.getInventory();
        if (inventory == null || slot.slot() >= inventory.getSize())
            throw new IOException("Physical LIVE container slot outside actual inventory");

        BlockLockerLiveEvidence.Ownership ownership = BlockLockerLiveEvidence.observe(block);
        if (!capturedWorld.isChunkLoaded(source.chunk().x(), source.chunk().z())
                || Bukkit.getWorld(capturedWorld.getUID()) != capturedWorld)
            throw new IOException("LIVE BlockLocker source disappeared during owner lookup");
        return classifyEvidence(source, value, ownership, banSources, existingPlayerdata);
    }

    /** Pure evidence classification seam; does not assert provider observation. */
    static SettlementAccumulator.Asset classifyEvidence(
            HybridLiveSourceStage.Source source,
            HybridLiveSourceStage.Value value,
            BlockLockerLiveEvidence.Ownership ownership,
            BanSourceCoverage banSources,
            Set<UUID> existingPlayerdata) throws IOException {
        LiveContainerPhysicalSlot.parse(source, value);
        if (ownership == null || banSources == null || existingPlayerdata == null)
            throw new IOException("Missing LIVE BlockLocker, BAN or playerdata evidence");
        UUID owner = ownership.owner();
        boolean banned = owner != null && banSources.currentlyBanned(owner);
        boolean present = owner != null && existingPlayerdata.contains(owner);
        return new SettlementAccumulator.Asset(
                source.id(), value.physicalKey(), value.world(),
                owner, ownership.everyoneProtected(), banned, present,
                value.amount(), value.physicalKey());
    }
}
