package co.surumene.simplecoins;

import co.surumene.simplecoins.domain.AdmissionBarrier;
import co.surumene.simplecoins.domain.SettlementSnapshotPublisher;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * Restricted bootstrap feasibility fixture for Paper 26.2.
 *
 * No authoritative settlement scanner, atomic cache publisher, or journal
 * monitor is connected yet. Access is intentionally denied for every player.
 * This MUST NOT be deployed as the finished SimpleCoins 1.0.0 plugin.
 */
public final class SimpleCoinsPlugin extends JavaPlugin {
    private AdmissionBarrier admission;
    private SettlementSnapshotPublisher publication;

    @Override
    public void onEnable() {
        admission = new AdmissionBarrier();
        publication = new SettlementSnapshotPublisher(admission);

        // Register as early as possible in the Paper plugin lifecycle.
        // AdmissionBarrier starts BLOCKED; do not call begin() or complete()
        // until a fully verified settlement pipeline is actually wired.
        try {
            getServer().getPluginManager().registerEvents(
                    new SettlementAdmissionListener(admission, publication), this);
            getLogger().warning("Settlement engine not connected; player admission remains disabled.");
        } catch (RuntimeException | Error failure) {
            admission.fatalFailure();
            getLogger().log(Level.SEVERE, "Failed to register admission protection", failure);
            // An unprotected running Paper server must not be considered safe.
            Bukkit.shutdown();
            throw failure;
        }
    }

    @Override
    public void onDisable() {
        // Revocation is required even for an already published generation.
        if (publication != null) {
            publication.revoke();
        }
        if (admission != null) {
            admission.stop();
        }

        // Bukkit removes the listener during plugin disable. Stop the server
        // rather than allowing an administrator to resume admission without
        // the protection. Paper crash/pre-enable safety needs separate proof.
        getLogger().severe("SimpleCoins admission protection disabled: stopping Paper.");
        Bukkit.shutdown();
    }
}
