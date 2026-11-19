package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.WclLiveTameObservation;
import co.surumene.simplecoins.domain.WclOwnerEvidence;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Wolf;
import org.bukkit.plugin.Plugin;

/**
 * A read-only LIVE counterpart to the SAVED candidate's ownerEvidence().
 * Neither this method nor a successful WCL read establishes G0 source
 * generation, writer exclusion, BAN/playerdata, accounting or admission.
 */
public final class PaperWonderfulLiveOwnerEvidence {
    private PaperWonderfulLiveOwnerEvidence() {}

    /**
     * Keep the WCL snapshot and vanilla owner observation in one main-thread
     * operation for a caller-selected loaded Entity. Any incomplete evidence
     * is an unpublished HOLD, never a zero balance or a FREE classification.
     */
    public static WclOwnerEvidence.Decision readLoaded(
            Plugin consumer, Entity entity, String providerId) throws IOException {
        Objects.requireNonNull(consumer, "consumer plugin");
        Objects.requireNonNull(entity, "selected loaded Entity");
        Objects.requireNonNull(providerId, "WCL provider identity");
        if (!Bukkit.isPrimaryThread())
            throw new IllegalStateException("LIVE WCL owner reads require Paper primary thread");

        PaperWonderfulLoadedEvidence.Result result =
                PaperWonderfulLoadedEvidence.readLoaded(consumer, entity, providerId);
        if (result.status() != PaperWonderfulLoadedEvidence.Status.SUCCESS)
            return hold();
        PaperWonderfulLoadedEvidence.Snapshot snapshot = result.snapshot().orElseThrow();

        UUID entityId = entity.getUniqueId();
        // Re-evaluate authority after the provider call. Never use an
        // independently reconstructed/invalid or wrong-world Entity.
        boolean authoritative = entity.isValid() && Bukkit.getEntity(entityId) == entity
                && entityId.equals(snapshot.entityId())
                && entity.getWorld().getUID().equals(snapshot.worldId())
                && providerId.equals(snapshot.providerId());
        if (!authoritative) return hold();

        WclOwnerEvidence.TameOwner tame;
        if ("wwc".equals(providerId) && entity instanceof Chicken) {
            tame = WclLiveTameObservation.chicken(true);
        } else if ("www".equals(providerId) && entity instanceof Wolf wolf) {
            var vanillaOwner = wolf.getOwner();
            tame = WclLiveTameObservation.wolf(
                    true, wolf.isTamed(),
                    vanillaOwner == null ? null : vanillaOwner.getUniqueId());
        } else {
            return hold();
        }
        return WclOwnerEvidence.decide(true, providerId, snapshot.relationships(), tame);
    }

    private static WclOwnerEvidence.Decision hold() {
        return new WclOwnerEvidence.Decision(WclOwnerEvidence.Kind.HOLD, null);
    }
}
