package co.surumene.simplecoins.domain;
import java.util.Map;

public final class WriterRosterWitnessChecks {
    private static void require(boolean ok) { if (!ok) throw new AssertionError("roster witness"); }
    private static WriterRosterWitness.Identity identity(Object instance, Object loader, boolean enabled) {
        return new WriterRosterWitness.Identity(instance, loader, "1", enabled);
    }
    public static void main(String[] args) {
        Object instance = new Object(), loader = new Object();
        var base = Map.of("A", identity(instance, loader, true), "B", identity(new Object(), loader, false));
        var witness = new WriterRosterWitness();
        require(!witness.current());
        witness.capture(base);
        require(witness.current() && witness.check(base));
        witness.transition();
        require(!witness.current() && !witness.check(base));
        try { witness.capture(base); throw new AssertionError("recapture accepted"); }
        catch (IllegalStateException expected) { }
        for (var changed : java.util.List.of(Map.of("A", identity(instance, loader, true)),
                Map.of("A", identity(new Object(), loader, true), "B", base.get("B")),
                Map.of("A", identity(instance, new Object(), true), "B", base.get("B")),
                Map.of("A", identity(instance, loader, false), "B", base.get("B")),
                Map.of("A", identity(instance, loader, true), "B", base.get("B"), "C", identity(new Object(), loader, true)))) {
            var fresh = new WriterRosterWitness(); fresh.capture(base);
            require(!fresh.check(changed) && !fresh.check(base));
        }
        var closed = new WriterRosterWitness(); closed.capture(base); closed.close();
        require(!closed.current() && !closed.check(base));
        System.out.println("WRITER_ROSTER_WITNESS_CHECKS_PASSED");
    }
}
