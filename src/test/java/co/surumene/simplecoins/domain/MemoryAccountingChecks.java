package co.surumene.simplecoins.domain;
import java.util.*;
import java.math.BigInteger;
public final class MemoryAccountingChecks {
    static BigInteger n(long v) { return BigInteger.valueOf(v); }
    static void equal(Object expected, Object actual) { if (!expected.equals(actual)) throw new AssertionError(expected + " != " + actual); }
    public static void main(String[] args) {
        for (String order : List.of("記帳済", "次", "欠落")) for (boolean valid : List.of(false, true)) {
            var input = Map.<String,Object>of("入力.順序", order, "入力.差額整合", valid);
            var driver = new MemoryAccountingDriver(); driver.prepare(Map.of(), input);
            equal(Map.of("出力.結果", order.equals("記帳済") ? "重複" : order.equals("次") && valid ? "反映" : "拒否"), driver.trigger("確定仕訳",input));
        }
        var a = UUID.randomUUID(); var b = UUID.randomUUID(); var c = UUID.randomUUID();
        var A = MemoryAccounting.Endpoint.personal(a); var B = MemoryAccounting.Endpoint.personal(b);
        var C1 = MemoryAccounting.Endpoint.dead(c, AssetOwner.DeadReason.CURRENT_BAN, "w:1");
        var C2 = MemoryAccounting.Endpoint.dead(c, AssetOwner.DeadReason.CURRENT_BAN, "w:2");
        var F = MemoryAccounting.Endpoint.free(); var X = MemoryAccounting.Endpoint.external();
        var m = new MemoryAccounting(Map.of(a,n(100)),n(20),Map.of(C1,n(30)));
        var first = List.of(new MemoryAccounting.Movement(A,B,n(100)),new MemoryAccounting.Movement(F,X,n(10)),new MemoryAccounting.Movement(C1,C2,n(30)));
        equal(MemoryAccounting.Outcome.APPLIED,m.record(1,first));
        equal(Map.of(a,n(0),b,n(100)),m.snapshot().personal()); equal(n(10),m.snapshot().free()); equal(Map.of(C2,n(30)),m.snapshot().dead());
        var before = m.snapshot();
        equal(MemoryAccounting.Outcome.DUPLICATE,m.record(1,first)); equal(before,m.snapshot());
        equal(MemoryAccounting.Outcome.REJECTED,m.record(2,List.of(new MemoryAccounting.Movement(B,F,n(101)),new MemoryAccounting.Movement(X,C2,n(10))))); equal(before,m.snapshot());
        equal(MemoryAccounting.Outcome.APPLIED,m.record(2,List.of(new MemoryAccounting.Movement(B,F,n(100))))); equal(Map.of(a,n(0),b,n(0)),m.snapshot().personal()); equal(n(110),m.snapshot().free());
        equal(MemoryAccounting.Outcome.APPLIED,m.record(3,List.of(new MemoryAccounting.Movement(F,B,n(25)),new MemoryAccounting.Movement(B,MemoryAccounting.Endpoint.dead(b, AssetOwner.DeadReason.CURRENT_BAN,"w:3"),n(25))),Set.of(b)));
        equal(Map.of(a,n(0)),m.snapshot().personal()); equal(n(85),m.snapshot().free());
        System.out.println("Six CAT input valuations and observed multi-account movements, atomic failure, zero registration, reclassification passed");
    }
}
