package co.surumene.simplecoins;

import co.surumene.simplecoins.domain.AdmissionBarrier;
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Objects;

/**
 * Experimental Paper 26.2 validation adapter. No listener registration is
 * performed until the authoritative startup settlement lifecycle is wired.
 * In particular, never call event.allow(): other plugins may already reject.
 */
public final class SettlementAdmissionListener implements Listener {
    private static final Component NOT_READY =
            Component.text("Server accounting is initializing. Please retry shortly.");

    private final AdmissionBarrier barrier;

    public SettlementAdmissionListener(AdmissionBarrier barrier) {
        this.barrier = Objects.requireNonNull(barrier, "barrier");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void validate(PlayerConnectionValidateLoginEvent event) {
        Objects.requireNonNull(event, "event");
        if (event.isAllowed() && !barrier.mayAdmit())
            event.kickMessage(NOT_READY);
    }
}
