package co.surumene.simplecoins;

import co.surumene.simplecoins.domain.AdmissionBarrier;
import co.surumene.simplecoins.guard.api.AdmissionPermit;
import co.surumene.simplecoins.domain.SettlementRuntime;
import co.surumene.simplecoins.paper.PaperProtectedSettlementService;
import co.surumene.simplecoins.domain.NormalMutationJournal;
import co.surumene.simplecoins.domain.GrantLedger;
import co.surumene.simplecoins.domain.WriterEpochFence;
import co.surumene.simplecoins.domain.PaperNativeSaveFenceAdapter;
import co.surumene.simplecoins.domain.CoinDenomination;
import co.surumene.simplecoins.domain.JavaCurrencyPack;
import co.surumene.simplecoins.domain.JavaPackDelivery;
import co.surumene.simplecoins.paper.PaperJavaPackDelivery;
import co.surumene.simplecoins.domain.GeyserResourceInstaller;
import co.surumene.simplecoins.paper.WalletItemCodec;
import co.surumene.simplecoins.paper.WalletCrafting;
import co.surumene.simplecoins.paper.WalletMenu;
import co.surumene.simplecoins.paper.WanderingTraderDropConfigReader;
import co.surumene.simplecoins.paper.WorldScopeConfigReader;
import co.surumene.simplecoins.paper.PaperSavedSourceDiscovery;
import co.surumene.simplecoins.paper.PaperSavedGrantDeliveryService;
import co.surumene.simplecoins.paper.PaperWonderfulReaderRegistry;
import co.surumene.simplecoins.paper.PaperNormalJournalWatchdog;
import co.surumene.simplecoins.paper.CoinExchangeRecipes;
import co.surumene.simplecoins.paper.CoinItemCodec;
import co.surumene.simplecoins.paper.PaperAdminCommand;
import co.surumene.simplecoins.paper.PaperPlayerGrantDelivery;
import co.surumene.simplecoins.domain.MemoryAccounting;
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
    private WriterEpochFence nativeWriterFence;
    private SettlementRuntime publication;
    private PaperProtectedSettlementService protectedSettlement;
    private NormalMutationJournal normalJournal;
    private GrantLedger issuanceGrants;
    private PaperNormalJournalWatchdog normalJournalWatchdog;
    private CoinExchangeRecipes exchangeRecipes;
    private WalletCrafting walletCrafting;
    private PaperSavedSourceDiscovery sourceDiscovery;
    private PaperSavedGrantDeliveryService savedGrantDelivery;

    @Override
    public void onEnable() {
        admission = new AdmissionBarrier();
        // Passive bootstrap-hook handshake only. This agent family covers two
        // native save entrypoints, NOT the complete G0/G3 writer universe.
        // NEVER arm this fence or enable tracked writer mode at bootstrap.
        nativeWriterFence = new WriterEpochFence();

        // Register as early as possible in the Paper plugin lifecycle.
        // AdmissionBarrier starts BLOCKED; do not call begin() or complete()
        // until a fully verified settlement pipeline is actually wired.
        try {
            if (!getServer().getOnlinePlayers().isEmpty())
                throw new IllegalStateException("Unsafe startup: players already online");
            try {
                publication = new SettlementRuntime(admission, nativeWriterFence,
                        new co.surumene.simplecoins.domain.RegisteredPlayerHistory(
                                getDataFolder().toPath().resolve("registered-players.txt")));
            } catch (IOException historyFailure) {
                throw new IllegalStateException("Cannot read registered-player history", historyFailure);
            }
            boolean nativeBridgePresent =
                    new PaperNativeSaveFenceAdapter(nativeWriterFence).bindIfAgentPresent();
            getLogger().info("SIMPLECOINS_NATIVE_WRITER_BRIDGE_PASSIVE bound="
                    + nativeBridgePresent + " (not armed; G0/G3/Admission denied)");
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
            // The preexisting issuance GrantLedger is also authoritative
            // durable evidence. Check/replay it at guarded startup instead of
            // silently treating a corrupt or unexamined grant as empty.
            // Opening it DOES NOT issue coins, apply a receipt, or authorize
            // any player to log in; delivery reconciliation remains pending.
            try {
                issuanceGrants = new GrantLedger(
                        getDataFolder().toPath().resolve("grants"));
                getLogger().info("SIMPLECOINS_ISSUANCE_JOURNAL_OPEN_VERIFIED");
            } catch (IOException grantFailure) {
                throw new IllegalStateException(
                        "Cannot open durable issuance grant journal", grantFailure);
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
            protectedSettlement = new PaperProtectedSettlementService(this, publication, normalJournal,
                    failure -> {
                        publication.revoke();
                        getLogger().log(Level.SEVERE, "Protected settlement failed", failure);
                        Bukkit.shutdown();
                    });
            // Validate every operator-controlled monetary setting before
            // installing handlers. Prior money remains identified by fixed PDC,
            // not display name, currency suffix or newly lowered wallet caps.
            saveDefaultConfig();
            // Validate the approved 10%/nine-denomination trader policy now.
            var traderDrops = WanderingTraderDropConfigReader.read(getConfig());
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
            // Only the actual installed Geyser plugin supplies the target
            // data directory. Geyser loads mappings during its own startup,
            // so new files need a subsequent Geyser/server restart.
            org.bukkit.plugin.Plugin geyser =
                    getServer().getPluginManager().getPlugin("Geyser-Spigot");
            org.bukkit.plugin.Plugin nexoPlugin = getServer().getPluginManager().getPlugin("Nexo");
            boolean nexo = nexoPlugin != null && nexoPlugin.isEnabled();
            try {
                if (!nexo) {
                    var generated = JavaCurrencyPack.build(getDataFolder().toPath(), names);
                    getLogger().info("SIMPLECOINS_CURRENCY_PACKS_GENERATED: Java="
                            + generated.javaArchive().getFileName()
                            + ", Bedrock=" + generated.bedrockArchive().getFileName()
                            + ", mapping=" + generated.bedrockMapping().getFileName()
                            + "; client display not yet verified");
                    // The hosting URL must serve this exact generated archive.
                    // The request adds a pack without replacing unrelated packs.
                    Object configuredUrl = getConfig().get("resource-pack.public-url", "");
                    try {
                        if (!(configuredUrl instanceof String url))
                            throw new IOException("Expected a string public URL");
                        var delivery = JavaPackDelivery.prepare(url, generated.javaArchive());
                        if (delivery.isPresent()) {
                            getServer().getPluginManager().registerEvents(
                                    new PaperJavaPackDelivery(this, delivery.orElseThrow()), this);
                            getLogger().info("SIMPLECOINS_JAVA_PACK_DELIVERY_CONFIGURED"
                                    + " (hosting reachability and client acceptance not yet verified)");
                        } else {
                            getLogger().warning("Java currency pack generated; configure resource-pack.public-url after hosting the ZIP.");
                        }
                    } catch (IOException invalidUrlOrArchive) {
                        // Signed URLs must not appear in logs.
                        getLogger().severe("Java pack delivery not configured: invalid URL or unavailable generated archive.");
                    }
                    if (geyser != null && geyser.isEnabled()) {
                        java.nio.file.Path geyserRoot = geyser.getDataFolder().toPath();
                        java.nio.file.Path config = geyserRoot.resolve("config.yml");
                        if (!java.nio.file.Files.isRegularFile(config,
                                java.nio.file.LinkOption.NOFOLLOW_LINKS))
                            throw new IOException("Geyser config.yml is absent or not a real file");
                        var yaml = org.bukkit.configuration.file.YamlConfiguration
                                .loadConfiguration(config.toFile());
                        Object enabled = yaml.get("gameplay.enable-custom-content");
                        boolean permitted = Boolean.TRUE.equals(enabled);
                        var result = GeyserResourceInstaller.stage(geyserRoot,
                                generated.bedrockArchive(), generated.bedrockMapping(), permitted);
                        getLogger().warning("SIMPLECOINS_GEYSER_PACK_STAGED: changed="
                                + result.changed()
                                + "; Geyser restart required if changed; "
                                + "not proof of Bedrock client delivery");
                    } else {
                        getLogger().warning("Geyser-Spigot not present; Bedrock pack not installed.");
                    }
                } else {
                    // Nexo merges the supplied namespaced Java assets and owns
                    // distribution. Generate the same source archive (including
                    // custom images); never register the direct JE listener here.
                    var generated = JavaCurrencyPack.build(getDataFolder().toPath(), names);
                    boolean changed = co.surumene.simplecoins.domain.NexoResourceInstaller.stage(
                            nexoPlugin.getDataFolder().toPath(), generated.javaArchive());
                    getLogger().info("SIMPLECOINS_NEXO_PACK_STAGED: changed=" + changed
                            + "; actual Nexo merge/client delivery not yet verified");
                    if (geyser != null && geyser.isEnabled()) {
                        boolean cleaned = GeyserResourceInstaller.removeOwned(
                                geyser.getDataFolder().toPath());
                        if (cleaned)
                            getLogger().warning(
                                    "SIMPLECOINS_GEYSER_NEXO_CLEANUP: prior owned files removed;"
                                    + " restart Geyser; Scaffolding/BE delegation is not verified");
                    }
                    // softdepend means Nexo may already have built its pack
                    // when this plugin enables. Request one documented rebuild
                    // after ServerLoadEvent if the import actually changed.
                    if (changed) {
                        getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener() {
                            private boolean requested;
                            @org.bukkit.event.EventHandler(priority = org.bukkit.event.EventPriority.MONITOR)
                            public void onServerReady(org.bukkit.event.server.ServerLoadEvent event) {
                                if (requested) return;
                                requested = true;
                                Bukkit.getScheduler().runTask(SimpleCoinsPlugin.this, () -> {
                                    if (!nexoPlugin.isEnabled()) {
                                        getLogger().severe("Nexo disabled before currency pack reload");
                                        return;
                                    }
                                    try {
                                        boolean dispatched = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "nexo reload pack");
                                        if (dispatched) getLogger().info("SIMPLECOINS_NEXO_PACK_RELOAD_REQUESTED");
                                        else getLogger().severe("Nexo pack reload command unavailable");
                                    } catch (RuntimeException failure) {
                                        getLogger().log(Level.SEVERE, "Nexo currency pack reload request failed", failure);
                                    }
                                });
                            }
                        }, this);
                    }
                    getLogger().warning("Nexo owns pack delivery; Bedrock requires a verified conversion and delivery path.");
                }
            } catch (IOException invalidPack) {
                throw new IllegalStateException(
                        "Cannot stage verified JE/BE currency resources", invalidPack);
            }
            CoinItemCodec coinCodec = new CoinItemCodec(names);
            java.util.function.Consumer<Exception> groundFailure = failure -> {
                publication.revoke();
                getLogger().log(Level.SEVERE, "Ground item accounting failed", failure);
            };
            var tickMovements = new java.util.ArrayList<MemoryAccounting.Movement>();
            var hopper = new co.surumene.simplecoins.paper.PaperHopperAccounting(sourceScope.scope(),
                    publication::memoryAccountingReady, tickMovements::addAll, groundFailure);
            var ground = new co.surumene.simplecoins.paper.PaperGroundItemAccounting(sourceScope.scope(),
                    publication::memoryAccountingReady, tickMovements::addAll, groundFailure,
                    hopper::observeInventory);
            getServer().getPluginManager().registerEvents(ground, this);
            getServer().getPluginManager().registerEvents(hopper, this);
            getServer().getScheduler().runTaskTimer(this, () -> {
                try {
                    ground.flush();
                    hopper.flush();
                    if (publication.memoryAccountingReady() && !tickMovements.isEmpty())
                        publication.recordConfirmedMemory(java.util.List.copyOf(tickMovements),java.util.Set.of());
                } catch (RuntimeException failure) { groundFailure.accept(failure); }
                finally { tickMovements.clear(); }
            },1L,1L);
            getServer().getPluginManager().registerEvents(new co.surumene.simplecoins.paper.TraderDeathIssuanceListener(
                    traderDrops, coinCodec, publication::memoryAccountingReady,
                    java.util.random.RandomGenerator.getDefault(), groundFailure), this);
            // Own the real saved-grant lifecycle, but never start it before
            // the complete installed writer coverage and startup gate exist.
            savedGrantDelivery = new PaperSavedGrantDeliveryService(this, admission,
                    nativeWriterFence, issuanceGrants, coinCodec, failure -> {
                        publication.revoke();
                        admission.fatalFailure();
                        getLogger().log(Level.SEVERE, "Protected saved grant delivery failed", failure);
                        Bukkit.shutdown();
                    });
            WalletItemCodec walletCodec = new WalletItemCodec(UUID::randomUUID, unit);
            var liveDelivery = new PaperPlayerGrantDelivery(issuanceGrants, coinCodec, (id, value) -> {
                publication.recordConfirmedMemory(java.util.List.of(new MemoryAccounting.Movement(
                        MemoryAccounting.Endpoint.external(), MemoryAccounting.Endpoint.personal(id),
                        java.math.BigInteger.valueOf(value))), java.util.Set.of());
                ground.excludeConfirmedGrant(id,value);
            });
            var admin = new PaperAdminCommand(this, publication, issuanceGrants, liveDelivery,
                    walletCodec, sourceScope.scope(), names, unit);
            var command = java.util.Objects.requireNonNull(getCommand("simplecoins"), "Missing simplecoins command");
            command.setExecutor(admin);
            command.setTabCompleter(admin);
            getServer().getPluginManager().registerEvents(admin, this);
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
                    SettlementAdmissionListener.forRuntime(admission, publication), this);
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
                    }, issuanceGrants);
            sourceDiscovery.start();
            // Discover the full, currently registered WCL reader set, not the
            // ServicesManager.load() single winner. This is startup diagnostics,
            // not an inventory capture or valid G0 accounting-generation proof.
            var wclRegistration = PaperWonderfulReaderRegistry.inspect(this);
            getLogger().info("SIMPLECOINS_WCL_REGISTRATION_OBSERVED"
                    + " status=" + wclRegistration.status()
                    + " reason=" + wclRegistration.reason()
                    + " providers=" + wclRegistration.owners().keySet()
                    + " (REGISTRATION ONLY; asset collection and admission disabled)");
            getLogger().warning("Settlement engine not connected; player admission remains disabled.");
        } catch (RuntimeException | Error failure) {
            admission.fatalFailure();
            if (protectedSettlement != null) {
                protectedSettlement.close();
                protectedSettlement = null;
            }
            if (savedGrantDelivery != null) {
                savedGrantDelivery.close();
                savedGrantDelivery = null;
            }
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
        if (protectedSettlement != null) {
            protectedSettlement.close();
            protectedSettlement = null;
        }
        if (savedGrantDelivery != null) {
            savedGrantDelivery.close();
            savedGrantDelivery = null;
        }
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
