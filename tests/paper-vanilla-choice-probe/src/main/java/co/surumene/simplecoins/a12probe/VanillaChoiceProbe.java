package co.surumene.simplecoins.a12probe;

import co.surumene.simplecoins.domain.CoinDenomination;
import co.surumene.simplecoins.domain.CoinExchangeFormula;
import co.surumene.simplecoins.paper.CoinExchangeRecipes;
import co.surumene.simplecoins.paper.CoinItemCodec;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Crafter;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.Keyed;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.CraftingRecipe;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Bounded CI oracle: a vanilla PAPER ingredient can match a physical coin,
 * while SimpleCoins' three crafting guards are registered.
 *
 * Does NOT simulate a successful craft, prove cancellation or trading safety.
 */
public final class VanillaChoiceProbe extends JavaPlugin {
    private static final String GUARD = "co.surumene.simplecoins.paper.CoinExchangeRecipes";

    @Override public void onEnable() {
        if (!"127.0.0.1".equals(Bukkit.getServer().getIp())
                || !Bukkit.getOnlinePlayers().isEmpty()) {
            getLogger().severe("PAPER_A12_PROBE_INVALID_ISOLATION");
            Bukkit.shutdown();
            return;
        }
        Bukkit.getScheduler().runTaskLater(this, () -> {
            try {
                ItemStack genuine = new CoinItemCodec(CoinItemCodec.defaultNames())
                        .create(CoinDenomination.fromValue(5000), 1);
                CoinDenomination observed = new CoinItemCodec(CoinItemCodec.defaultNames())
                        .identify(genuine).orElseThrow();
                if (observed.value() != 5000)
                    throw new IllegalStateException("Authentic currency codec not present");
                List<String> vanillaMatches = new ArrayList<>();
                var recipes = Bukkit.recipeIterator();
                while (recipes.hasNext()) {
                    Recipe recipe = recipes.next();
                    if (!(recipe instanceof Keyed keyed)
                            || !"minecraft".equals(keyed.getKey().getNamespace())) continue;
                    List<RecipeChoice> choices = new ArrayList<>();
                    if (recipe instanceof ShapedRecipe shaped) {
                        choices.addAll(shaped.getChoiceMap().values());
                    } else if (recipe instanceof ShapelessRecipe shapeless) {
                        choices.addAll(shapeless.getChoiceList());
                    }
                    for (RecipeChoice choice : choices) {
                        if (choice instanceof RecipeChoice.MaterialChoice && choice.test(genuine)) {
                            vanillaMatches.add(keyed.getKey().toString());
                            break;
                        }
                    }
                }
                if (vanillaMatches.isEmpty())
                    throw new IllegalStateException("No vanilla PAPER MaterialChoice matched genuine coin");
                getLogger().info("PAPER_A12_VANILLA_MATERIAL_CHOICE_MATCHES_CURRENCY_PASSED"
                        + " count=" + vanillaMatches.size()
                        + " recipes=" + vanillaMatches.stream().limit(12).toList());

                requireListener("PrepareItemCraftEvent", PrepareItemCraftEvent.getHandlerList());
                requireListener("CraftItemEvent", CraftItemEvent.getHandlerList());
                requireListener("CrafterCraftEvent", CrafterCraftEvent.getHandlerList());
                getLogger().info("PAPER_A12_THREE_EXISTING_CRAFT_GUARDS_REGISTERED_PASSED");
                verifyRecipeIdentityBoundary();
                Bukkit.shutdown();
            } catch (Throwable error) {
                getLogger().log(Level.SEVERE, "PAPER_A12_PROBE_FAILED", error);
                Bukkit.shutdown();
            }
        }, 50L);
    }

    /**
     * Paper's actual recipe registry and the already installed production
     * CraftItemEvent listener. No product recipe or wallet changes survive
     * this bounded disposable server test.
     */
    private void verifyRecipeIdentityBoundary() throws Exception {
        Plugin product = Bukkit.getPluginManager().getPlugin("SimpleCoins");
        if (product == null || !product.isEnabled())
            throw new IllegalStateException("Product plugin not active in Paper registry");
        CoinExchangeRecipes guard = null;
        for (RegisteredListener registration :
                CraftItemEvent.getHandlerList().getRegisteredListeners()) {
            if (registration.getPlugin() == product
                    && registration.getListener() instanceof CoinExchangeRecipes listener) {
                if (guard != null) throw new IllegalStateException("Duplicate registered currency guard");
                guard = listener;
            }
        }
        if (guard == null) throw new IllegalStateException("Actual product recipe guard not registered");
        CoinItemCodec coins = new CoinItemCodec(CoinItemCodec.defaultNames());
        ItemStack[] adversarialMatrix = null;
        Recipe replaceTarget = null;
        NamespacedKey replaceKey = null;

        int matched = 0;
        for (CoinExchangeFormula formula : CoinExchangeFormula.all()) {
            NamespacedKey key = new NamespacedKey(product, "exchange_" + formula.id());
            Recipe actual = Bukkit.getRecipe(key);
            if (!(actual instanceof ShapelessRecipe)
                    || actual.getResult().getAmount() != formula.outputCount()
                    || coins.identify(actual.getResult()).orElse(null) != formula.output()) {
                throw new IllegalStateException("Registered monetary result differs from formula " + key);
            }
            ItemStack[] matrix = new ItemStack[9];
            for (int i = 0; i < formula.inputCount(); i++) {
                matrix[i] = coins.create(formula.input(), 1);
            }
            if (!guard.isApproved(actual, matrix))
                throw new IllegalStateException("Legitimate registered exchange rejected: " + key);
            ItemStack[] badMatrix = matrix.clone();
            badMatrix[0] = coins.create(
                    formula.input() == CoinDenomination.TEN_THOUSAND
                            ? CoinDenomination.ONE : CoinDenomination.TEN_THOUSAND, 1);
            if (guard.isApproved(actual, badMatrix))
                throw new IllegalStateException("Wrong denomination authorized: " + key);
            if (replaceTarget == null) {
                replaceTarget = actual;
                replaceKey = key;
                adversarialMatrix = matrix;
            }
            matched++;
        }
        if (matched != 20 || replaceTarget == null)
            throw new IllegalStateException("Expected all 20 actual exchange recipes");
        getLogger().info("PAPER_A12_ALL_TWENTY_REAL_EXCHANGE_INPUT_OUTPUT_APPROVALS_PASSED");

        // The only recipe removed is restored in finally. No actual crafting
        // or player resources: this is a key/result trust-boundary experiment.
        if (!Bukkit.removeRecipe(replaceKey))
            throw new IllegalStateException("Cannot remove disposable copy of recipe " + replaceKey);
        try {
            ShapelessRecipe bogus = new ShapelessRecipe(replaceKey, new ItemStack(Material.STONE, 1));
            for (ItemStack slot : adversarialMatrix) {
                if (slot != null) bogus.addIngredient(Material.PAPER);
            }
            boolean installed = Bukkit.addRecipe(bogus);
            if (!installed) {
                getLogger().info("PAPER_A12_RECIPE_SAME_KEY_SUBSTITUTION_REJECTED_BY_PAPER");
                return;
            }
            Recipe observed = Bukkit.getRecipe(replaceKey);
            if (observed == null || observed.getResult().getType() != Material.STONE)
                throw new IllegalStateException("Paper recipe registry did not expose substituted result");
            boolean approved = guard.isApproved(observed, adversarialMatrix);
            if (approved)
                throw new IllegalStateException("A12_RED: same-key STONE output was approved"
                        + " key=" + replaceKey);
            getLogger().info("PAPER_A12_SUBSTITUTED_OUTPUT_REJECTED_BY_GUARD");
            verifyPaperPrepareEvent(replaceKey, adversarialMatrix);
            verifyPaperCrafterEvent(replaceKey, adversarialMatrix);
        } finally {
            Bukkit.removeRecipe(replaceKey);
            if (!Bukkit.addRecipe(replaceTarget))
                throw new IllegalStateException("Failed to restore exact original recipe " + replaceKey);
            Recipe restored = Bukkit.getRecipe(replaceKey);
            if (restored == null
                    || restored.getResult().getType() == Material.STONE
                    || !guard.isApproved(restored, adversarialMatrix))
                throw new IllegalStateException("Monetary recipe not restored after bounded probe");
            getLogger().info("PAPER_A12_DISPOSABLE_RECIPE_RESTORED_PASSED");
        }
        // A separate late mutation test operates on the genuine restored recipe.
        // Keep the same-key substitution experiment above isolated in its finally.
        verifyLatePrepareOutputMutation(replaceKey, adversarialMatrix);
        getLogger().info("PAPER_A12_RECIPE_REGISTRY_OUTPUT_INTEGRITY_OBSERVED");
    }


    /**
     * Exercises the actual Paper Bukkit craftItemResult path. It creates a
     * DETACHED, non-network NMS player solely because Paper requires a Player
     * witness for PrepareItemCraftEvent. It does not install a crafted output
     * in any inventory or cause a client/transaction to happen.
     */
    private void verifyPaperPrepareEvent(NamespacedKey substituted,
                                         ItemStack[] originalMoney) throws Exception {
        AtomicInteger prepareCount = new AtomicInteger();
        ItemStack[] resultObservedAtMonitor = {null};
        Listener monitor = new Listener() {
            @EventHandler(priority = EventPriority.MONITOR)
            public void observePrepare(PrepareItemCraftEvent event) {
                if (event.getRecipe() instanceof Keyed keyed
                        && substituted.equals(keyed.getKey())) {
                    prepareCount.incrementAndGet();
                    ItemStack item = event.getInventory().getResult();
                    resultObservedAtMonitor[0] = item == null ? null : item.clone();
                }
            }
        };
        Bukkit.getPluginManager().registerEvents(monitor, this);
        try {
            Player synthetic = detachedPlayer();
            if (synthetic.isOnline())
                throw new IllegalStateException("Synthetic crafting witness became online");
            var produced = Bukkit.craftItemResult(
                    originalMoney.clone(), Bukkit.getWorlds().getFirst(), synthetic);
            if (prepareCount.get() < 1)
                throw new IllegalStateException(
                        "Paper craftItemResult did not dispatch our targeted Prepare event");
            ItemStack finalOutput = produced.getResult();
            Material actual = finalOutput == null ? Material.AIR : finalOutput.getType();
            Material observed = resultObservedAtMonitor[0] == null
                    ? Material.AIR : resultObservedAtMonitor[0].getType();
            if (observed == Material.STONE || actual == Material.STONE)
                throw new IllegalStateException("A12_RED: Paper Prepare produced nonmonetary STONE"
                        + " recipe=" + substituted + " observed=" + observed
                        + " result=" + actual);
            getLogger().info("PAPER_A12_REAL_PREPARE_EVENT_SUBSTITUTION_BLOCKED"
                    + " recipe=" + substituted + " observed=" + observed
                    + " result=" + actual);
            getLogger().info("PAPER_A12_ACTUAL_PREPARE_RESULT_OBSERVED"
                    + " prepareEventCount=" + prepareCount.get()
                    + " postMonitor=" + observed + " result=" + actual
                    + " disconnectedSyntheticPlayer=true");
        } finally {
            HandlerList.unregisterAll(monitor);
        }
    }


    /**
     * Real Block/Crafter inventory and Bukkit event-bus dispatch; no
     * powered redstone craft, real inventory consumption or physical money.
     */
    private void verifyPaperCrafterEvent(NamespacedKey key, ItemStack[] money) {
        World world = Bukkit.getWorlds().getFirst();
        int x = 40, z = 40;
        int y = Math.max(world.getMinHeight() + 20,
                Math.min(world.getMaxHeight() - 4,
                        world.getHighestBlockYAt(x, z) + 4));
        Block block = world.getBlockAt(x, y, z);
        if (!block.getType().isAir())
            throw new IllegalStateException("Disposable Crafter probe location not vacant");
        AtomicInteger observed = new AtomicInteger();
        Listener monitor = new Listener() {
            @EventHandler(priority = EventPriority.MONITOR)
            public void observedCrafter(CrafterCraftEvent event) {
                if (event.getRecipe() instanceof Keyed keyed
                        && key.equals(keyed.getKey()))
                    observed.incrementAndGet();
            }
        };
        Bukkit.getPluginManager().registerEvents(monitor, this);
        try {
            block.setType(Material.CRAFTER);
            if (!(block.getState() instanceof Crafter crafter))
                throw new IllegalStateException("Real Paper Crafter block state unavailable");
            int idx = 0;
            for (ItemStack item : money) {
                if (item == null || item.getType().isAir()) continue;
                crafter.getInventory().setItem(idx++, item.clone());
            }
            if (idx < 1)
                throw new IllegalStateException("No genuine monetary item placed in Crafter");
            ItemStack physical = ((Crafter) block.getState()).getInventory().getItem(0);
            if (physical == null
                    || new CoinItemCodec(CoinItemCodec.defaultNames())
                            .identify(physical).isEmpty())
                throw new IllegalStateException("Crafter lost the genuine money before event");
            Recipe current = Bukkit.getRecipe(key);
            if (!(current instanceof CraftingRecipe crafting))
                throw new IllegalStateException("Substituted recipe is not a crafting recipe");
            CrafterCraftEvent event = new CrafterCraftEvent(
                    block, crafting, new ItemStack(Material.STONE, 1));
            Bukkit.getPluginManager().callEvent(event);
            if (observed.get() != 1)
                throw new IllegalStateException("Actual target CrafterCraftEvent not dispatched");
            Material after = event.getResult() == null
                    ? Material.AIR : event.getResult().getType();
            if (!event.isCancelled())
                throw new IllegalStateException("A12_RED: modified-result Crafter event not cancelled"
                        + " recipe=" + key + " result=" + after);
            getLogger().info("PAPER_A12_REAL_CRAFTER_SUBSTITUTED_OUTPUT_CANCELLED"
                    + " recipe=" + key + " cancelled=" + event.isCancelled()
                    + " result=" + after);
            getLogger().info("PAPER_A12_ACTUAL_CRAFTER_EVENT_RESULT_OBSERVED"
                    + " count=" + observed.get() + " result=" + after
                    + " cancelled=" + event.isCancelled()
                    + " redstoneFired=false");
        } finally {
            HandlerList.unregisterAll(monitor);
            block.setType(Material.AIR);
        }
    }

    /**
     * Verify a distinct plugin can or cannot mutate a *legitimate* prepared
     * monetary result after the product's already-registered HIGHEST guard.
     * This does not perform a CraftItemEvent or consume any item.
     */
    private void verifyLatePrepareOutputMutation(NamespacedKey key,
                                                 ItemStack[] money) throws Exception {
        Plugin product = Bukkit.getPluginManager().getPlugin("SimpleCoins");
        Recipe original = Bukkit.getRecipe(key);
        CoinItemCodec coinCodec = new CoinItemCodec(CoinItemCodec.defaultNames());
        if (product == null || !product.isEnabled() || original == null
                || coinCodec.identify(original.getResult()).isEmpty())
            throw new IllegalStateException("Expected intact genuine exchange before late mutation");
        AtomicInteger lateCount = new AtomicInteger();
        AtomicInteger monitorCount = new AtomicInteger();
        boolean[] originalWasMoney = {false};
        Material[] observedAtMonitor = {Material.AIR};
        Listener lateReplacement = new Listener() {
            @EventHandler(priority = EventPriority.HIGHEST)
            public void replacePreparedResult(PrepareItemCraftEvent event) {
                if (!(event.getRecipe() instanceof Keyed keyed)
                        || !key.equals(keyed.getKey())) return;
                lateCount.incrementAndGet();
                ItemStack current = event.getInventory().getResult();
                originalWasMoney[0] = current != null
                        && coinCodec.identify(current).isPresent();
                event.getInventory().setResult(new ItemStack(Material.STONE, 1));
            }
        };
        Listener monitor = new Listener() {
            @EventHandler(priority = EventPriority.MONITOR)
            public void observeLateResult(PrepareItemCraftEvent event) {
                if (!(event.getRecipe() instanceof Keyed keyed)
                        || !key.equals(keyed.getKey())) return;
                monitorCount.incrementAndGet();
                ItemStack current = event.getInventory().getResult();
                observedAtMonitor[0] = current == null ? Material.AIR : current.getType();
            }
        };
        // Both handlers are deliberately registered *after* the product.
        Bukkit.getPluginManager().registerEvents(lateReplacement, this);
        Bukkit.getPluginManager().registerEvents(monitor, this);
        try {
            Player player = detachedPlayer();
            if (player.isOnline() || !Bukkit.getOnlinePlayers().isEmpty())
                throw new IllegalStateException("Late Prepare probe is not offline");
            var crafted = Bukkit.craftItemResult(
                    money.clone(), Bukkit.getWorlds().getFirst(), player);
            ItemStack result = crafted.getResult();
            Material returned = result == null ? Material.AIR : result.getType();
            if (lateCount.get() != 1 || monitorCount.get() != 1 || !originalWasMoney[0])
                throw new IllegalStateException("Late Prepare observer did not see exactly one genuine monetary result");
            Recipe current = Bukkit.getRecipe(key);
            if (current == null || coinCodec.identify(current.getResult()).isEmpty())
                throw new IllegalStateException("Late Prepare probe mutated genuine recipe registry");
            getLogger().info("PAPER_A12_LATE_PREPARE_MUTATION_OBSERVED"
                    + " lateEvents=" + lateCount.get()
                    + " monitorEvents=" + monitorCount.get()
                    + " originallyMoney=true observed=" + observedAtMonitor[0]
                    + " apiResult=" + returned + " onlinePlayers=0");
        } finally {
            HandlerList.unregisterAll(lateReplacement);
            HandlerList.unregisterAll(monitor);
        }
    }

    private Player detachedPlayer() throws ReflectiveOperationException {
        Object craftServer = Bukkit.getServer();
        Object minecraft = craftServer.getClass().getMethod("getServer").invoke(craftServer);
        Class<?> mcType = Class.forName("net.minecraft.server.MinecraftServer");
        Object world = mcType.getMethod("overworld").invoke(minecraft);
        Class<?> levelType = Class.forName("net.minecraft.server.level.ServerLevel");
        Class<?> profileType = Class.forName("com.mojang.authlib.GameProfile");
        Object profile = profileType.getConstructor(UUID.class, String.class)
                .newInstance(UUID.fromString("00000000-0000-0000-0000-00000000a012"),
                        "ProbeCrafting");
        Class<?> infoType = Class.forName("net.minecraft.server.level.ClientInformation");
        Object info = infoType.getMethod("createDefault").invoke(null);
        Class<?> playerType = Class.forName("net.minecraft.server.level.ServerPlayer");
        Constructor<?> ctor = playerType.getConstructor(
                mcType, levelType, profileType, infoType);
        Object serverPlayer = ctor.newInstance(minecraft, world, profile, info);
        Object bukkitPlayer = playerType.getMethod("getBukkitEntity").invoke(serverPlayer);
        if (!(bukkitPlayer instanceof Player paper))
            throw new IllegalStateException("Detached synthetic player unavailable");
        return paper;
    }

    private static void requireListener(String name, HandlerList list) {
        for (RegisteredListener registration : list.getRegisteredListeners()) {
            if (GUARD.equals(registration.getListener().getClass().getName())
                    && registration.getPlugin().isEnabled()) {
                return;
            }
        }
        throw new IllegalStateException("Missing registered SimpleCoins guard for " + name);
    }
}
