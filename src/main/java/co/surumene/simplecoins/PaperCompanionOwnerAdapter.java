package co.surumene.simplecoins;

import co.surumene.simplecoins.domain.CompanionOwnerResolution;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Tameable;

/**
 * Read-only evidence from a currently loaded Paper Tameable. This supplies
 * only the vanilla owner: WWC/WWW/WCL commander state is a separate required
 * source and must not be inferred absent from this adapter.
 */
public final class PaperCompanionOwnerAdapter {
    private PaperCompanionOwnerAdapter() {}

    public static CompanionOwnerResolution.Identity readPaper(Tameable entity) {
        if (entity == null)
            return CompanionOwnerResolution.unknown();
        try {
            return read(entity.isTamed(), () -> {
                AnimalTamer tamer = entity.getOwner();
                return tamer == null ? null : tamer.getUniqueId();
            });
        } catch (RuntimeException unavailable) {
            return CompanionOwnerResolution.unknown();
        }
    }

    /** Testable boundary for current Paper ownership evidence. */
    public static CompanionOwnerResolution.Identity read(
            Boolean tamed, Supplier<UUID> ownerLookup) {
        if (tamed == null)
            return CompanionOwnerResolution.unknown();
        if (!tamed)
            return CompanionOwnerResolution.absent();
        if (ownerLookup == null)
            return CompanionOwnerResolution.unknown();
        try {
            UUID owner = ownerLookup.get();
            return owner == null ? CompanionOwnerResolution.unknown()
                    : CompanionOwnerResolution.known(owner);
        } catch (RuntimeException unavailable) {
            return CompanionOwnerResolution.unknown();
        }
    }
}
