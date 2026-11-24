package co.surumene.simplecoins.domain;

import java.math.BigInteger;
import java.util.Map;
import java.util.UUID;

public final class OwnerReconciliationDriver {
    private Map<String, Object> prepared;
    public void prepare(Map<String,Object> state, Map<String,Object> input) {
        if (!state.isEmpty()) throw new IllegalArgumentException("Unexpected state");
        prepared = Map.copyOf(input);
    }
    public Map<String,Object> observeState() { return Map.of(); }
    public Map<String,Object> trigger(String trigger, Map<String,Object> input) {
        if (!"再分類".equals(trigger) || !input.equals(prepared)) throw new IllegalArgumentException();
        UUID owner = UUID.randomUUID(), other = UUID.randomUUID();
        var otherDead = MemoryAccounting.Endpoint.dead(other, AssetOwner.DeadReason.CURRENT_BAN, "other");
        var account = new MemoryAccounting(Map.of(owner, BigInteger.TEN), BigInteger.ONE, Map.of(otherDead, BigInteger.ONE));
        var old = account.snapshot();
        boolean complete = (Boolean) input.get("入力.完全"), banned = (Boolean) input.get("入力.BAN"),
                hasData = (Boolean) input.get("入力.playerdata"), basis = (Boolean) input.get("入力.登録根拠"),
                remains = (Boolean) input.get("入力.残存");
        var actual = remains ? Map.of("one", BigInteger.valueOf(3), "two", BigInteger.valueOf(4)) : Map.<String,BigInteger>of();
        var result = account.reconcileOwner(owner, complete, banned, hasData, basis, actual);
        var after = account.snapshot();
        if (result == MemoryAccounting.Outcome.REJECTED) {
            if (!old.equals(after)) throw new AssertionError("Partial replacement");
            return Map.of("出力.結果", "保留");
        }
        if (!after.free().equals(BigInteger.ONE) || !BigInteger.ONE.equals(after.dead().get(otherDead)))
            throw new AssertionError("Unrelated accounts changed");
        var ownerDead = after.dead().entrySet().stream().filter(e -> owner.equals(e.getKey().owner().player())).toList();
        if (banned || !hasData) {
            var reason = banned ? AssetOwner.DeadReason.CURRENT_BAN : AssetOwner.DeadReason.MISSING_PLAYERDATA;
            var expected = new java.util.HashMap<MemoryAccounting.Endpoint,BigInteger>();
            actual.forEach((location,amount) -> expected.put(MemoryAccounting.Endpoint.dead(owner, reason, location),amount));
            if (after.personal().containsKey(owner) || !Map.copyOf(expected).equals(ownerDead.stream().collect(java.util.stream.Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue))))
                throw new AssertionError("Incorrect dead locations");
            return Map.of("出力.結果", banned ? "死蔵BAN" : "死蔵欠損");
        }
        if (!ownerDead.isEmpty()) throw new AssertionError("Stale dead location");
        if (after.personal().containsKey(owner)) {
            if (!after.personal().get(owner).equals(remains ? BigInteger.valueOf(7) : BigInteger.ZERO)) throw new AssertionError("Incorrect new personal balance");
            return Map.of("出力.結果", "個人登録");
        }
        return Map.of("出力.結果", "個人未登録");
    }
}
