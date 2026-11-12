package co.surumene.simplecoins;

import co.surumene.simplecoins.domain.AdmissionBarrier;
import co.surumene.simplecoins.domain.CoinDenomination;
import co.surumene.simplecoins.paper.CoinExchangeRecipes;
import co.surumene.simplecoins.paper.CoinItemCodec;
import co.surumene.simplecoins.paper.WanderingTraderDropConfigReader;
import co.surumene.simplecoins.paper.WalletCrafting;
import co.surumene.simplecoins.paper.WalletItemCodec;
import co.surumene.simplecoins.paper.WalletMenu;
import co.surumene.simplecoins.guard.api.AdmissionPermit;
import co.surumene.simplecoins.domain.SettlementSnapshotPublisher;
import co.surumene.simplecoins.domain.NormalMutationJournal;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.ServicePriority;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
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
    private CoinExchangeRecipes exchanges;
    private WalletCrafting walletCrafting;

    @Override
    public void onEnable() {
        admission = new AdmissionBarrier();
        publication = new SettlementSnapshotPublisher(admission);

        // The independent STARTUP guard must already be loaded and denying
        // new connections. A plugin reload with players present is unsafe.
        // Never begin a settlement scan or open admission in this bootstrap.
        try {
            if (!getServer().getOnlinePlayers().isEmpty())
                throw new IllegalStateException("Unsafe SimpleCoins bootstrap: players already online");
            saveDefaultConfig();
            // Initialize the independently durable journal before registering
            // currency-related Paper listeners. Failure is terminal.
            // A healthy journal alone is NOT permission to admit players:
            // authoritative settlement and event-to-ledger binding are absent.
            try {
                normalJournal = new NormalMutationJournal(
                        getDataFolder().toPath().resolve("normal-mutations"));
                normalJournal.verifyAll();
                if (normalJournal.hasUnreconciledFromStartup())
                    getLogger().warning("Unreconciled monetary mutations: authoritative "
                            + "physical settlement required before any admission.");
            } catch (IOException journalFailure) {
                throw new IllegalStateException(
                        "Cannot initialize durable normal monetary journal", journalFailure);
            }
            WanderingTraderDropConfigReader.read(getConfig()); // strict validation, not issuance
            EnumMap<CoinDenomination, String> names = new EnumMap<>(CoinDenomination.class);
            Map<CoinDenomination, String> fallback = CoinItemCodec.defaultNames();
            for (CoinDenomination coin : CoinDenomination.values()) {
                String key = "coins." + coin.value() + ".name";
                Object value = getConfig().get(key);
                if (value != null && (!(value instanceof String name) || name.isBlank()))
                    throw new IllegalArgumentException("Invalid configured currency display name: " + key);
                names.put(coin, value == null ? fallback.get(coin) : (String) value);
            }
            Object rawUnit = getConfig().get("currency.unit", "C");
            if (!(rawUnit instanceof String unit) || unit.isBlank())
                throw new IllegalArgumentException("Invalid currency.unit");
            Object rawCapacity = getConfig().get("wallet.max-balance", 640000);
            if (!(rawCapacity instanceof Number capacityValue))
                throw new IllegalArgumentException("Invalid wallet.max-balance");
            long capacity;
            try { capacity = new java.math.BigDecimal(capacityValue.toString()).longValueExact(); }
            catch (ArithmeticException | NumberFormatException invalid) {
                throw new IllegalArgumentException("Wallet capacity must be an exact long", invalid);
            }
            if (capacity < 0)
                throw new IllegalArgumentException("Wallet capacity cannot be negative");
            CoinItemCodec codec = new CoinItemCodec(names);
            WalletItemCodec walletCodec = new WalletItemCodec(java.util.UUID::randomUUID, unit);
            exchanges = new CoinExchangeRecipes(this, codec);
            exchanges.register();
            walletCrafting = new WalletCrafting(this, walletCodec);
            walletCrafting.register();
            getServer().getPluginManager().registerEvents(
                    new WalletMenu(walletCodec, codec, capacity, failure -> {
                        publication.revoke();
                        getLogger().log(Level.SEVERE,
                                "Irrecoverable wallet/coin inventory mutation; revoking admission", failure);
                    }), this);
            getServer().getPluginManager().registerEvents(
                    new SettlementAdmissionListener(admission, publication), this);
            // Read-only service is served through the independent guard's
            // classloader; it never opens while journal/scan are not connected.
            getServer().getServicesManager().register(
                    AdmissionPermit.class, publication::mayAdmit, this, ServicePriority.Normal);
            getLogger().warning("Registered " + exchanges.registeredCount()
                    + " value-neutral currency exchanges and " + walletCrafting.registeredCount()
                    + " wallet recipes; settlement and trader issuance remain disabled.");
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
        // Remove the cross-plugin permission service before tearing down state.
        // The independent guard then denies, even after this plugin is disabled.
        getServer().getServicesManager().unregisterAll(this);
        if (walletCrafting != null) {
            try { walletCrafting.unregister(); }
            catch (RuntimeException | Error failure) {
                getLogger().log(Level.SEVERE, "Failed to unregister wallet recipes", failure);
            }
            walletCrafting = null;
        }
        if (exchanges != null) {
            try { exchanges.unregister(); }
            catch (RuntimeException | Error failure) {
                getLogger().log(Level.SEVERE, "Failed to unregister currency recipes", failure);
            }
            exchanges = null;
        }
        if (normalJournal != null) {
            try { normalJournal.close(); }
            catch (IOException journalFailure) {
                getLogger().log(Level.SEVERE,
                        "Failed to close durable normal monetary journal", journalFailure);
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
