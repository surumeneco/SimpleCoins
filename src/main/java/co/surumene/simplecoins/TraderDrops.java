package co.surumene.simplecoins;

import co.surumene.simplecoins.domain.CoinDenomination;
import co.surumene.simplecoins.domain.TraderDropChance;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public final class TraderDrops implements Listener {
    private final SimpleCoinsPlugin plugin;
    private final CoinItems items;

    public TraderDrops(SimpleCoinsPlugin plugin, CoinItems items) {
        this.plugin = plugin; this.items = items;
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (event.getEntityType() != EntityType.WANDERING_TRADER ||
                !plugin.getConfig().getBoolean("trader-drops.enabled", true)) return;
        double chance = plugin.getConfig().getDouble("trader-drops.chance", 0.001);
        if (!TraderDropChance.shouldDrop(chance, ThreadLocalRandom.current().nextDouble())) return;
        ConfigurationSection weights = plugin.getConfig().getConfigurationSection("trader-drops.weights");
        if (weights == null) return;
        List<CoinDenomination> denominations = new ArrayList<>();
        List<Integer> amounts = new ArrayList<>();
        long sum = 0;
        for (CoinDenomination d : CoinDenomination.values()) {
            int weight = weights.getInt(String.valueOf(d.value()), 0);
            if (weight > 0) {
                sum += weight;
                if (sum > Integer.MAX_VALUE) return;
                denominations.add(d);
                amounts.add(weight);
            }
        }
        if (sum == 0) return;
        int roll = ThreadLocalRandom.current().nextInt((int) sum);
        CoinDenomination selected = denominations.getFirst();
        for (int i = 0; i < amounts.size(); i++) {
            if (roll < amounts.get(i)) { selected = denominations.get(i); break; }
            roll -= amounts.get(i);
        }
        int min = Math.max(1, plugin.getConfig().getInt("trader-drops.minimum-count", 1));
        int max = Math.min(64, plugin.getConfig().getInt("trader-drops.maximum-count", 1));
        if (min > max) return;
        event.getDrops().add(items.coin(selected, ThreadLocalRandom.current().nextInt(min, max + 1)));
    }
}
