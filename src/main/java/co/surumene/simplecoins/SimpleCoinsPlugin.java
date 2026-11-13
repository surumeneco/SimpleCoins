package co.surumene.simplecoins;

import co.surumene.simplecoins.domain.AdmissionBarrier;
import co.surumene.simplecoins.guard.api.AdmissionPermit;
import co.surumene.simplecoins.domain.SettlementSnapshotPublisher;
import co.surumene.simplecoins.domain.NormalMutationJournal;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.ServicePriority;

import java.io.IOException;
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
    private NormalMutationJournal normalJournal;

    @Override
    public void onEnable() {
        admission = new AdmissionBarrier();
        publication = new SettlementSnapshotPublisher(admission);

        // Register as early as possible in the Paper plugin lifecycle.
        // AdmissionBarrier starts BLOCKED; do not call begin() or complete()
        // until a fully verified settlement pipeline is actually wired.
        try {
            if (!getServer().getOnlinePlayers().isEmpty())
                throw new IllegalStateException("Unsafe startup: players already online");
            // Open and check persistent monetary evidence before registering
            // any runtime listener/service. This is NOT world reconciliation.
            try {
                normalJournal = new NormalMutationJournal(
                        getDataFolder().toPath().resolve("normal-mutations"));
                normalJournal.verifyAll();
                if (normalJournal.hasUnreconciledFromStartup())
                    getLogger().warning("Unreconciled monetary mutations: "
                            + "physical settlement required before admission.");
            } catch (IOException journalFailure) {
                throw new IllegalStateException(
                        "Cannot open durable normal mutation journal", journalFailure);
            }
            getServer().getPluginManager().registerEvents(
                    new SettlementAdmissionListener(admission, publication), this);
            // Read-only service is served through the independent guard's
            // classloader; it never opens while journal/scan are not connected.
            getServer().getServicesManager().register(
                    AdmissionPermit.class, publication::mayAdmit, this, ServicePriority.Normal);
            getLogger().warning("Settlement engine not connected; player admission remains disabled.");
        } catch (RuntimeException | Error failure) {
            admission.fatalFailure();
            if (normalJournal != null) {
                try { normalJournal.close(); }
                catch (IOException closeFailure) { failure.addSuppressed(closeFailure); }
                normalJournal = null;
            }
            getLogger().log(Level.SEVERE, "Failed to initialize guarded accounting", failure);
            // An unprotected running Paper server must not be considered safe.
            Bukkit.shutdown();
            throw failure;
        }
    }

    @Override
    public void onDisable() {
        // Remove the cross-plugin permission service before tearing down state.
        // The independent guard then denies, even after this plugin is disabled.
        getServer().getServicesManager().unregisterAll(this);
        if (normalJournal != null) {
            try { normalJournal.close(); }
            catch (IOException closeFailure) {
                getLogger().log(Level.SEVERE, "Failed to close normal mutation journal", closeFailure);
            }
            normalJournal = null;
        }
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
