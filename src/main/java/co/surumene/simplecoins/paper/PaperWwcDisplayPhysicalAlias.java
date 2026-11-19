package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.WclPhysicalSlotIdentity;
import java.io.IOException;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;

/**
 * Read-only identification of temporary WWC 2.1.1 ArmorStand visuals.
 *
 * Those stands copy the Chicken's carpet/shulker ItemStack into an invisible
 * helmet solely for rendering. Their stand UUID is NOT a second physical
 * economic holder. This returns a collision key, never a value-dedup decision
 * or confirmation of same-generation WCL inventory evidence.
 */
public final class PaperWwcDisplayPhysicalAlias {
    private static final NamespacedKey OWNER =
            new NamespacedKey("whatawonderfulchicken", "display_owner");
    private static final NamespacedKey ROLE =
            new NamespacedKey("whatawonderfulchicken", "display_role");
    private static final NamespacedKey TYPE =
            new NamespacedKey("whatawonderfulchicken", "type");

    private PaperWwcDisplayPhysicalAlias() {}

    /**
     * @return a source-local, world-qualified alias of its original WWC WCL
     *         slot, or null when this is not a tagged WWC projection.
     * @throws IOException when tagged projection provenance is incomplete,
     *         foreign, unsupported or incompatible with the shipped renderer.
     */
    public static String verifiedPhysicalKey(Entity observed, String equipmentSlot,
                                             String observedWorldName) throws IOException {
        if (!(observed instanceof ArmorStand stand)) return null;
        if (observedWorldName == null || observedWorldName.isBlank()
                || !observedWorldName.equals(observed.getWorld().getName()))
            throw new IOException("WWC display observed in a foreign World");
        var pdc = stand.getPersistentDataContainer();
        boolean hasOwner = pdc.has(OWNER);
        boolean hasRole = pdc.has(ROLE);
        if (!hasOwner && !hasRole) return null;
        if (!hasOwner || !hasRole || !"head".equals(equipmentSlot))
            throw new IOException("Incomplete/foreign WWC monetary display equipment");
        if (stand.isPersistent() || !stand.isMarker() || !stand.isInvisible()
                || !stand.isInvulnerable())
            throw new IOException("Tagged ArmorStand is not an ephemeral WWC display");
        String ownerText = pdc.get(OWNER, PersistentDataType.STRING);
        String role = pdc.get(ROLE, PersistentDataType.STRING);
        UUID ownerId;
        try {
            if (ownerText == null) throw new IllegalArgumentException("Missing owner");
            ownerId = UUID.fromString(ownerText);
            if (!ownerId.toString().equals(ownerText))
                throw new IllegalArgumentException("Noncanonical display owner");
        } catch (IllegalArgumentException invalid) {
            throw new IOException("Malformed WWC display owner", invalid);
        }
        String sourceKey = WclPhysicalSlotIdentity.wwcDisplayKey(ownerId, role);
        Entity parent = Bukkit.getEntity(ownerId);
        if (!(parent instanceof Chicken chicken) || !chicken.isValid()
                || chicken.getWorld() != stand.getWorld()
                || !"wonderful_chicken".equals(chicken.getPersistentDataContainer()
                        .get(TYPE, PersistentDataType.STRING)))
            throw new IOException("WWC display owner not an observed valid source Chicken");
        return "world/" + observedWorldName + "/" + sourceKey;
    }
}
