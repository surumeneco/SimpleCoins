package co.surumene.simplecoins.domain;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Converts observed endpoint balances to a single atomic memory-journal operation.
 * External legs are bookkeeping offsets; conservation is measured from physical deltas. */
public final class InventoryAccountingDelta {
    private InventoryAccountingDelta() {}

    public static List<MemoryAccounting.Movement> between(
            Map<MemoryAccounting.Endpoint, BigInteger> before,
            Map<MemoryAccounting.Endpoint, BigInteger> after) {
        var changes = new HashMap<MemoryAccounting.Endpoint, BigInteger>();
        add(changes, before, true);
        add(changes, after, false);
        var result = new ArrayList<MemoryAccounting.Movement>();
        var external = MemoryAccounting.Endpoint.external();
        changes.forEach((endpoint, amount) -> {
            if (amount.signum() < 0)
                result.add(new MemoryAccounting.Movement(endpoint, external, amount.negate()));
            else if (amount.signum() > 0)
                result.add(new MemoryAccounting.Movement(external, endpoint, amount));
        });
        return List.copyOf(result);
    }

    private static void add(Map<MemoryAccounting.Endpoint, BigInteger> changes,
            Map<MemoryAccounting.Endpoint, BigInteger> values, boolean debit) {
        Map.copyOf(values).forEach((key, value) -> {
            if (key.owner() == null || value.signum() < 0)
                throw new IllegalArgumentException("Invalid physical endpoint balance");
            changes.merge(key, debit ? value.negate() : value, BigInteger::add);
        });
    }
}
