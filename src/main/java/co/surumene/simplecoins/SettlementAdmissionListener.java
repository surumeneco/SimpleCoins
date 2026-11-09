package co.surumene.simplecoins;

import co.surumene.simplecoins.domain.AdmissionBarrier;
import co.surumene.simplecoins.domain.SettlementSnapshotPublisher;
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Objects;

/**
 * Paper 26.2 validation adapter. A READY flag without a published complete
 * settlement snapshot is not admission authority. No listener registration is
 * performed until the authoritative startup settlement lifecycle is wired.
 * In particular, never call event.allow(): other plugins may already reject.
 */
public final class SettlementAdmissionListener implements Listener {
    private static final Component NOT_READY =
            Component.text("Server accounting is initializing. Please retry shortly.");

    private final AdmissionBarrier barrier;
    private final SettlementSnapshotPublisher publication;

    /**
     * Compatibility constructor: without a bound published snapshot, access
     * stays denied even when a caller sets the barrier to READY.
     */
    @Deprecated
    public SettlementAdmissionListener(AdmissionBarrier barrier) {
        this(barrier, null);
    }

    public SettlementAdmissionListener(AdmissionBarrier barrier,
                                       SettlementSnapshotPublisher publication) {
        this.barrier = Objects.requireNonNull(barrier, "barrier");
        if (publication != null && !publication.usesBarrier(barrier))
            throw new IllegalArgumentException("Publication belongs to a different admission barrier");
        this.publication = publication;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void validate(PlayerConnectionValidateLoginEvent event) {
        Objects.requireNonNull(event, "event");
        if (!event.isAllowed()) {
            // Another listener has already rejected admission; preserve its reason.
            return;
        }
        if (!barrier.mayAdmit() || publication == null || !publication.mayAdmit()) {
            event.kickMessage(NOT_READY);
        }
    }
}
