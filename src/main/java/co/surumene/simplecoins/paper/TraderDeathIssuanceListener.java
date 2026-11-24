package co.surumene.simplecoins.paper;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

/** Currency is issued by the actual death drop; accounting observes its ItemSpawnEvent. */
public final class TraderDeathIssuanceListener implements Listener {
    private final WanderingTraderDropConfigReader.Loaded config;
    private final CoinItemCodec codec;
    private final BooleanSupplier ready;
    private final RandomGenerator random;
    private final Consumer<Exception> fatal;

    public TraderDeathIssuanceListener(WanderingTraderDropConfigReader.Loaded config,
            CoinItemCodec codec, BooleanSupplier ready, RandomGenerator random, Consumer<Exception> fatal) {
        this.config=Objects.requireNonNull(config);
        this.codec=Objects.requireNonNull(codec);
        this.ready=Objects.requireNonNull(ready);
        this.random=Objects.requireNonNull(random);
        this.fatal=Objects.requireNonNull(fatal);
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onDeath(EntityDeathEvent event) {
        try {
            var selected=config.policy().drawForDeath(ready.getAsBoolean(),
                    event.getEntity() instanceof WanderingTrader,event.isCancelled(),random);
            if(selected.isPresent()) event.getDrops().addAll(TraderCoinStackMaterializer.prepare(
                    selected.orElseThrow(),codec,config.maxStacksPerEvent()));
        } catch(RuntimeException failure) { fatal.accept(failure); }
    }
}
