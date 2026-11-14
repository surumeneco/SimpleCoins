package co.surumene.simplecoins;

import co.surumene.simplecoins.domain.AdmissionBarrier;
import co.surumene.simplecoins.guard.api.AdmissionPermit;
import co.surumene.simplecoins.domain.SettlementSnapshotPublisher;
import co.surumene.simplecoins.domain.NormalMutationJournal;
import co.surumene.simplecoins.domain.CoinDenomination;
import co.surumene.simplecoins.domain.JavaCurrencyPack;
import co.surumene.simplecoins.paper.WalletItemCodec;
import co.surumene.simplecoins.paper.WalletCrafting;
import co.surumene.simplecoins.paper.WalletMenu;
import co.surumene.simplecoins.paper.WanderingTraderDropConfigReader;
import co.surumene.simplecoins.paper.WorldScopeConfigReader;
import co.surumene.simplecoins.paper.PaperSavedSourceDiscovery;
import co.surumene.simplecoins.paper.PaperNormalJournalWatchdog;
import co.surumene.simplecoins.paper.CoinExchangeRecipes;
import co.surumene.simplecoins.paper.CoinItemCodec;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.ServicePriority;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Restricted bootstrap feasibility fixture for Paper 26.2.
 *
 * No authoritative settlement scanner, atomic cache publisher, or complete
 * journal reconciliation is connected yet. Admission stays blocked for all players.
 * This MUST NOT be deployed as the finished SimpleCoins 1.0.0 plugin.
 */
public final class SimpleCoinsPlugin extends JavaPlugin {
    private AdmissionBarrier admission;
    private SettlementSnapshotPublisher publication;
    private NormalMutationJournal normalJournal;
    private PaperNormalJournalWatchdog normalJournalWatchdog;
    private CoinExchangeRecipes exchangeRecipes;
    private WalletCrafting walletCrafting;
    private PaperSavedSourceDiscovery sourceDiscovery;

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
            // After startup verification, continuously check that the durable
            // monetary journal's writer lock/head remains valid. Corruption
            // must stop Paper without waiting for a login or new mutation.
            // This cheap liveness check does not prove world commit.
            normalJournalWatchdog = new PaperNormalJournalWatchdog(
                    this, normalJournal, () -> {
                        publication.revoke();
                        admission.fatalFailure();
                        Bukkit.shutdown();
                    });
            normalJournalWatchdog.start();
            // Validate every operator-controlled monetary setting before
            // installing handlers. Prior money remains identified by fixed PDC,
            // not display name, currency suffix or newly lowered wallet caps.
            saveDefaultConfig();
            // Validate the approved 10%/nine-denomination trader policy now.
            // The death listener is intentionally NOT registered until real
            // issuance and physical world settlement can be reconciled.
            WanderingTraderDropConfigReader.read(getConfig());
            WorldScopeConfigReader.Loaded sourceScope = WorldScopeConfigReader.read(getConfig());
            EnumMap<CoinDenomination, String> names = new EnumMap<>(CoinDenomination.class);
            Map<CoinDenomination, String> fallback = CoinItemCodec.defaultNames();
            for (CoinDenomination coin : CoinDenomination.values()) {
                String key = "coins." + coin.value() + ".name";
                Object configured = getConfig().get(key);
                if (configured != null && (!(configured instanceof String name) || name.isBlank()))
                    throw new IllegalArgumentException("Invalid currency display name: " + key);
                names.put(coin, configured == null ? fallback.get(coin) : (String) configured);
            }
            Object rawUnit = getConfig().get("currency.unit", "C");
            if (!(rawUnit instanceof String unit) || unit.isBlank())
                throw new IllegalArgumentException("Invalid currency.unit");
            Object rawCap = getConfig().get("wallet.max-balance", 640000);
            if (!(rawCap instanceof Number capNumber))
                throw new IllegalArgumentException("Invalid wallet.max-balance");
            long capacity;
            try { capacity = new java.math.BigDecimal(capNumber.toString()).longValueExact(); }
            catch (ArithmeticException | NumberFormatException invalid) {
                throw new IllegalArgumentException("Wallet max balance must be an exact long", invalid);
            }
            if (capacity < 0) throw new IllegalArgumentException("Negative wallet max balance");

            // Build private JE/BE pack candidates from the approved nine
            // stable coin IDs. Pack distribution and Nexo delegation remain
            // separate and do NOT qualify as a full 1.0.0 release.
            if (!getServer().getPluginManager().isPluginEnabled("Nexo")) {
                try {
                    var generated = JavaCurrencyPack.build(getDataFolder().toPath(), names);
                    getLogger().info("SIMPLECOINS_CURRENCY_PACKS_GENERATED: Java="
                            + generated.javaArchive().getFileName()
                            + ", Bedrock=" + generated.bedrockArchive().getFileName()
                            + ", mapping=" + generated.bedrockMapping().getFileName()
                            + "; private staging only; not distributed to clients");
                } catch (IOException invalidPack) {
                    throw new IllegalStateException(
                            "Cannot build complete self-managed JE/BE currency pack", invalidPack);
                }
            } else {
                getLogger().warning(
                        "Nexo detected; SimpleCoins pack generation suppressed. "
                        + "Delegated JE/BE distribution is not yet verified.");
            }
            CoinItemCodec coinCodec = new CoinItemCodec(names);
            WalletItemCodec walletCodec = new WalletItemCodec(UUID::randomUUID, unit);
            exchangeRecipes = new CoinExchangeRecipes(this, coinCodec);
            exchangeRecipes.register();
            if (exchangeRecipes.registeredCount() != 20)
                throw new IllegalStateException("Incomplete coin exchange registration");
            walletCrafting = new WalletCrafting(this, walletCodec);
            walletCrafting.register();
            if (walletCrafting.registeredCount() != 17)
                throw new IllegalStateException("Incomplete wallet crafting registration");
            getServer().getPluginManager().registerEvents(
                    new WalletMenu(walletCodec, coinCodec, capacity, failure -> {
                        publication.revoke();
                        getLogger().log(Level.SEVERE,
                                "Irrecoverable wallet inventory mutation; revoking admission", failure);
                    }), this);
            getServer().getPluginManager().registerEvents(
                    new SettlementAdmissionListener(admission, publication), this);
            // Read-only service is served through the independent guard's
            // classloader; it never opens while journal/scan are not connected.
            getServer().getServicesManager().register(
                    AdmissionPermit.class, publication::mayAdmit, this, ServicePriority.Normal);
            // Capture Paper world folders on its main thread, but walk their
            // saved files off-thread. This is not a staged monetary settlement
            // and MUST NOT publish statistics or open player admission.
            sourceDiscovery = new PaperSavedSourceDiscovery(this, sourceScope.scope(),
                    sourceScope.maxSourceEntries(), () -> {
                        publication.revoke();
                        admission.fatalFailure();
                        getLogger().severe("Saved-source discovery failed; stopping Paper.");
                        Bukkit.shutdown();
                    });
            sourceDiscovery.start();
            getLogger().warning("Settlement engine not connected; player admission remains disabled.");
        } catch (RuntimeException | Error failure) {
            admission.fatalFailure();
            if (normalJournalWatchdog != null) {
                try { normalJournalWatchdog.close(); }
                catch (RuntimeException closeFailure) { failure.addSuppressed(closeFailure); }
                normalJournalWatchdog = null;
            }
            if (sourceDiscovery != null) {
                sourceDiscovery.close();
                sourceDiscovery = null;
            }
            if (walletCrafting != null) {
                try { walletCrafting.unregister(); }
                catch (RuntimeException rollbackFailure) { failure.addSuppressed(rollbackFailure); }
                walletCrafting = null;
            }
            if (exchangeRecipes != null) {
                try { exchangeRecipes.unregister(); }
                catch (RuntimeException rollbackFailure) { failure.addSuppressed(rollbackFailure); }
                exchangeRecipes = null;
            }
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
        if (normalJournalWatchdog != null) {
            normalJournalWatchdog.close();
            normalJournalWatchdog = null;
        }
        if (sourceDiscovery != null) {
            sourceDiscovery.close();
            sourceDiscovery = null;
        }
        // Remove the cross-plugin permission service before tearing down state.
        // The independent guard then denies, even after this plugin is disabled.
        getServer().getServicesManager().unregisterAll(this);
        if (walletCrafting != null) {
            try { walletCrafting.unregister(); }
            catch (RuntimeException walletFailure) {
                getLogger().log(Level.SEVERE, "Failed to unregister wallet recipes", walletFailure);
            }
            walletCrafting = null;
        }
        if (exchangeRecipes != null) {
            try { exchangeRecipes.unregister(); }
            catch (RuntimeException recipeFailure) {
                getLogger().log(Level.SEVERE, "Failed to remove coin exchange recipes", recipeFailure);
            }
            exchangeRecipes = null;
        }
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
