package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.TraderIssuanceJournal;
import co.surumene.simplecoins.domain.WanderingTraderDropPolicy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;
import org.bukkit.Bukkit;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Candidate death-event adapter. No startup registration until an actual
 * settlement reconciliation and fail-closed admission authority are available.
 */
public final class TraderDeathIssuanceListener implements Listener {
    private final WanderingTraderDropConfigReader.Loaded config;
    private final CoinItemCodec codec;
    private final TraderIssuanceJournal journal;
    private final RandomGenerator random;
    private final Consumer<Exception> fatal;
    private boolean halted;

    public TraderDeathIssuanceListener(WanderingTraderDropConfigReader.Loaded config,
            CoinItemCodec codec, TraderIssuanceJournal journal, RandomGenerator random,
            Consumer<Exception> fatal) {
        this.config = Objects.requireNonNull(config);
        this.codec = Objects.requireNonNull(codec);
        this.journal = Objects.requireNonNull(journal);
        this.random = Objects.requireNonNull(random);
        this.fatal = Objects.requireNonNull(fatal);
        if (this.journal.hasUnreconciledFromStartup())
            throw new IllegalStateException("Unreconciled trader issuance receipts: "
                    + "startup reconciliation is required before issuing new coins");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof WanderingTrader) || halted) return;
        try {
            if (!Bukkit.isPrimaryThread())
                throw new IllegalStateException("Trader death issuance must run on main thread");
            UUID traderId = event.getEntity().getUniqueId();
            if (journal.has(traderId)) return;
            Optional<WanderingTraderDropPolicy.Drop> selected = config.policy().draw(random);
            List<ItemStack> minted = selected.isPresent()
                    ? TraderCoinStackMaterializer.prepare(selected.orElseThrow(), codec,
                    config.maxStacksPerEvent()) : List.of();
            if (!journal.begin(traderId, selected)) return;
            if (minted.isEmpty()) return;
            List<ItemStack> before = new ArrayList<>(event.getDrops());
            try {
                event.getDrops().addAll(minted);
                journal.markEventRecorded(traderId);
            } catch (RuntimeException | IOException fail) {
                event.getDrops().clear();
                event.getDrops().addAll(before);
                throw fail;
            }
        } catch (RuntimeException | IOException failure) {
            halted = true;
            fatal.accept(failure);
        }
    }

    public boolean isHalted() { return halted; }
}
