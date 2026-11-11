package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.TraderDropStackPlan;
import co.surumene.simplecoins.domain.WanderingTraderDropPolicy;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Prepares genuine PAPER currency ItemStacks without publishing them.
 *
 * The caller is responsible for an atomic issuance receipt and for adding the
 * returned stacks to the Paper event only when that transaction is authorized.
 * This class does not register a death listener or issue currency by itself.
 */
public final class TraderCoinStackMaterializer {
    private TraderCoinStackMaterializer() {}

    public static List<ItemStack> prepare(WanderingTraderDropPolicy.Drop drop,
                                          CoinItemCodec codec, int maxStacks) {
        Objects.requireNonNull(codec, "codec");
        List<Integer> counts = TraderDropStackPlan.counts(drop, maxStacks);
        List<ItemStack> prepared = new ArrayList<>(counts.size());
        for (int count : counts) {
            prepared.add(codec.create(drop.denomination(), count));
        }
        return List.copyOf(prepared);
    }
}
