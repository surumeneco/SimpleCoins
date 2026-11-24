package co.surumene.simplecoins.domain;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class OwnerReconciliationChecks {
    static BigInteger n(long n) { return BigInteger.valueOf(n); }
    static void equal(Object expected, Object actual) { if (!expected.equals(actual)) throw new AssertionError(expected + " != " + actual); }
    public static void main(String[] args) {
        for (boolean complete : List.of(false,true)) for (boolean banned : List.of(false,true))
            for (boolean data : List.of(false,true)) for (boolean basis : List.of(false,true)) for (boolean remains : List.of(false,true)) {
                var input = Map.<String,Object>of("入力.完全",complete,"入力.BAN",banned,"入力.playerdata",data,"入力.登録根拠",basis,"入力.残存",remains);
                var driver = new OwnerReconciliationDriver(); driver.prepare(Map.of(),input);
                String expected = !complete ? "保留" : banned ? "死蔵BAN" : !data ? "死蔵欠損" : basis || remains ? "個人登録" : "個人未登録";
                equal(Map.of("出力.結果",expected),driver.trigger("再分類",input));
            }
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        var account = new MemoryAccounting(Map.of(a,n(10),b,n(20)),n(30),Map.of());
        var ban = AssetOwner.DeadReason.CURRENT_BAN;
        var missing = AssetOwner.DeadReason.MISSING_PLAYERDATA;
        equal(MemoryAccounting.Outcome.APPLIED,account.reconcileOwner(a,true,true,true,true,Map.of("playerdata",n(4),"chest",n(6))));
        equal(Map.of(b,n(20)),account.snapshot().personal());
        equal(Map.of(MemoryAccounting.Endpoint.dead(a,ban,"playerdata"),n(4),MemoryAccounting.Endpoint.dead(a,ban,"chest"),n(6)),account.snapshot().dead());
        equal(MemoryAccounting.Outcome.APPLIED,account.reconcileOwner(a,true,false,false,true,Map.of("chest",n(6))));
        equal(Map.of(MemoryAccounting.Endpoint.dead(a,missing,"chest"),n(6)),account.snapshot().dead());
        equal(MemoryAccounting.Outcome.APPLIED,account.reconcileOwner(a,true,false,true,true,Map.of("chest",n(6))));
        equal(Map.of(a,n(6),b,n(20)),account.snapshot().personal()); equal(Map.of(),account.snapshot().dead()); equal(n(30),account.snapshot().free());
        var before = account.snapshot();
        equal(MemoryAccounting.Outcome.REJECTED,account.reconcileOwner(a,true,false,true,true,Map.of("bad",n(-1)))); equal(before,account.snapshot());
        var scope = new WorldScope(true, java.util.Set.of(), java.util.Set.of("excluded"));
        var index = OwnerStorageIndex.fromAssets(List.of(
            new SettlementAccumulator.Asset("saved:r.0.0", "a", "world", a, false, false, true, n(5), "world/world/1,2,3:0"),
            new SettlementAccumulator.Asset("saved:r.0.0", "b", "world", a, false, false, true, n(7), "world/world/1,2,3:1"),
            new SettlementAccumulator.Asset("saved:r.0.0", "c", "world", a, false, false, true, n(0), "world/world/1,2,4:container"),
            new SettlementAccumulator.Asset("saved:r.0.0", "d", "world", a, true, false, true, n(9), "world/world/1,2,5:0"),
            new SettlementAccumulator.Asset("saved:r.0.0", "e", "excluded", a, false, false, true, n(9), "world/excluded/1,2,5:0")),scope);
        equal(2,index.locations(a).size());
        equal(0,index.locations(b).size());
        var entity = OwnerStorageIndex.Holder.entity(UUID.randomUUID());
        var original = OwnerStorageIndex.Change.replace(a,new OwnerStorageIndex.Location(entity,"world","live:chunk/world/0/0"));
        index.apply(List.of(original));
        long version=index.revision();
        index.apply(List.of(original)); equal(version,index.revision());
        var moved=OwnerStorageIndex.Change.replace(b,new OwnerStorageIndex.Location(entity,"world2","live:chunk/world2/1/0"));
        index.apply(List.of(moved)); equal(2,index.locations(a).size()); equal(java.util.Set.of(moved.location()),index.locations(b));
        index.apply(List.of(OwnerStorageIndex.Change.remove(entity))); equal(0,index.locations(b).size());
        System.out.println("Owner locator deduplication, zero container, excluded/shared storage, idempotence and cross-world transfer passed");
        System.out.println("32 classifications and sequential P->D->D->P, deleted storage, malformed observation, unrelated accounts passed");
    }
}
