package co.surumene.simplecoins.domain;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPOutputStream;
import java.math.BigInteger;

/** Real saved-data and journal checks; isolated tracking evidence is NOT a writer census. */
public final class SettlementRuntimeChecks {
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final SettlementWriterEpochDecision.Evidence READY =
            new SettlementWriterEpochDecision.Evidence(true,true,true,true,true,true,true,false);
    private static final SettlementWriterEpochDecision.Evidence INCOMPLETE =
            new SettlementWriterEpochDecision.Evidence(true,false,true,true,true,true,true,false);
    static void check(boolean ok) { if (!ok) throw new AssertionError("runtime handoff invariant"); }
    interface Action { void run() throws Exception; }
    static void rejected(Action action) throws Exception {
        try { action.run(); } catch (IllegalStateException | IOException expected) { return; }
        throw new AssertionError("Unsafe runtime handoff accepted");
    }
    private static SettlementRuntime.Captured capture(Path root) throws IOException {
        Path data = root.resolve("world/playerdata/" + PLAYER + ".dat");
        Files.createDirectories(data.getParent());
        try (var out = new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(data)))) {
            out.writeByte(10); out.writeUTF("");
            out.writeByte(9); out.writeUTF("Inventory"); out.writeByte(10); out.writeInt(1);
            out.writeByte(1); out.writeUTF("Slot"); out.writeByte(0);
            out.writeByte(8); out.writeUTF("id"); out.writeUTF("minecraft:paper");
            out.writeByte(3); out.writeUTF("count"); out.writeInt(1);
            out.writeByte(10); out.writeUTF("components");
            out.writeByte(8); out.writeUTF("minecraft:item_model"); out.writeUTF("simplecoins:coin_5000");
            out.writeByte(10); out.writeUTF("minecraft:custom_data");
            out.writeByte(10); out.writeUTF("PublicBukkitValues");
            out.writeByte(3); out.writeUTF("simplecoins:coin_denomination"); out.writeInt(5000);
            out.writeByte(0); out.writeByte(0); out.writeByte(0); out.writeByte(0);
            out.writeByte(9); out.writeUTF("EnderItems"); out.writeByte(10); out.writeInt(0); out.writeByte(0);
        }
        var scope = new WorldScope(true, Set.of(), Set.of());
        var manifest = WorldStorageDiscovery.discover(Map.of("world",root.resolve("world")),scope,256);
        var staged = SettlementScanRunner.stage(manifest,scope,Set.of(),(world,pos)->{
            throw new IOException("Unexpected block lookup");
        },(world,nbt)->Map.of(),65536,65536);
        return new SettlementRuntime.Captured(staged,manifest);
    }
    static void run(Path dir) throws Exception {
        var captured = capture(dir);
        try(var journal = new NormalMutationJournal(dir.resolve("journal"))) {
            var fence=new WriterEpochFence(); var gate=new AdmissionBarrier();
            var runtime=new SettlementRuntime(gate,fence);
            check(!runtime.mayAdmit());
            rejected(()->runtime.begin(null,journal,()->true,()->true));
            check(gate.state()==AdmissionBarrier.State.BLOCKED);
            fence.arm(); var seal=fence.seal(); var tracking=new AtomicBoolean(true);
            var a=runtime.begin(seal,journal,tracking::get,()->true);
            rejected(()->runtime.begin(seal,journal,tracking::get,()->true));
            check(!runtime.mayAdmit());
            var prepared=runtime.prepare(a,()->captured);
            check(!runtime.mayAdmit() && fence.state()==WriterEpochFence.State.SEALED);
            check(runtime.complete(prepared,READY)==SettlementWriterEpochDecision.Verdict.TRY_ATOMIC_PUBLISH);
            check(runtime.mayAdmit() && fence.state()==WriterEpochFence.State.OPEN);
            check(runtime.current().orElseThrow().statistics().worldTotal().equals(BigInteger.valueOf(5000)));
            tracking.set(false); check(!runtime.mayAdmit());
            check(fence.state()==WriterEpochFence.State.FAULTED);
        }
        try(var journal = new NormalMutationJournal(dir.resolve("journal2"))) {
            var fence=new WriterEpochFence(); fence.arm(); var seal=fence.seal(); var gate=new AdmissionBarrier();
            var runtime=new SettlementRuntime(gate,fence); var a=runtime.begin(seal,journal,()->true,()->true);
            var prepared=runtime.prepare(a,()->capture(dir.resolve("hold")));
            check(runtime.complete(prepared,INCOMPLETE)==SettlementWriterEpochDecision.Verdict.HOLD_UNPUBLISHED);
            check(!runtime.mayAdmit());
            runtime.revoke(); rejected(()->runtime.complete(prepared,READY));
            check(!runtime.mayAdmit() && fence.state()==WriterEpochFence.State.FAULTED);
        }
        try(var journal = new NormalMutationJournal(dir.resolve("journal3"))) {
            var fence=new WriterEpochFence(); fence.arm(); var seal=fence.seal(); var gate=new AdmissionBarrier();
            var runtime=new SettlementRuntime(gate,fence); var a=runtime.begin(seal,journal,()->true,()->true);
            rejected(()->runtime.prepare(a,()->{ throw new IOException("source failure"); }));
            check(!runtime.mayAdmit() && fence.state()==WriterEpochFence.State.FAULTED);
        }
    }
    static void runMemory(Path dir) throws Exception {
        var fence = new WriterEpochFence();
        var gate = new AdmissionBarrier();
        var runtime = new SettlementRuntime(gate, fence);
        var tracking = new AtomicBoolean(true);
        var attempt = runtime.beginMemory(tracking::get, () -> true);
        var prepared = runtime.prepareMemory(attempt, () -> capture(dir.resolve("memory")));
        check(!runtime.mayAdmit() && runtime.current().isEmpty());
        check(prepared.summary().personal().equals(Map.of(PLAYER, BigInteger.valueOf(5000))));
        check(prepared.summary().free().signum() == 0);
        check(prepared.summary().dead().isEmpty());
        var hold = new SettlementWriterEpochDecision.Evidence(true,true,false,true,true,false);
        check(runtime.completeMemory(prepared, hold) == SettlementWriterEpochDecision.Verdict.HOLD_UNPUBLISHED);
        check(runtime.current().isEmpty());
        var ready = new SettlementWriterEpochDecision.Evidence(true,true,true,true,true,false);
        check(runtime.completeMemory(prepared, ready) == SettlementWriterEpochDecision.Verdict.TRY_ATOMIC_PUBLISH);
        check(runtime.mayAdmit() && fence.state() == WriterEpochFence.State.DISARMED);
        check(runtime.current().orElseThrow().statistics().worldTotal().equals(BigInteger.valueOf(5000)));
        var issued = new MemoryAccounting.Movement(MemoryAccounting.Endpoint.external(),
                MemoryAccounting.Endpoint.personal(PLAYER), BigInteger.valueOf(100));
        check(runtime.recordMemory(1, java.util.List.of(issued), Set.of()) == MemoryAccounting.Outcome.APPLIED);
        check(runtime.recordMemory(1, java.util.List.of(issued), Set.of()) == MemoryAccounting.Outcome.DUPLICATE);
        check(runtime.current().orElseThrow().statistics().worldTotal().equals(BigInteger.valueOf(5100)));
        tracking.set(false);
        check(!runtime.mayAdmit() && runtime.current().isEmpty());
    }
    static void runRegistrationRestart(Path dir) throws Exception {
        var historyFile = dir.resolve("registered-players.txt");
        var eligibleZero = UUID.fromString("00000000-0000-0000-0000-000000000102");
        var bannedZero = UUID.fromString("00000000-0000-0000-0000-000000000103");
        var neverRegistered = UUID.fromString("00000000-0000-0000-0000-000000000104");
        var missing = UUID.fromString("00000000-0000-0000-0000-000000000105");
        var history = new RegisteredPlayerHistory(historyFile);
        var first = new SettlementRuntime(new AdmissionBarrier(), new WriterEpochFence(), history);
        var attempt = first.beginMemory(() -> true, () -> true);
        var prepared = first.prepareMemory(attempt, () -> capture(dir.resolve("initial")));
        var ready = new SettlementWriterEpochDecision.Evidence(true,true,true,true,true,false);
        check(first.completeMemory(prepared, ready) == SettlementWriterEpochDecision.Verdict.TRY_ATOMIC_PUBLISH);
        first.recordConfirmedMemory(java.util.List.of(), Set.of(eligibleZero, bannedZero, missing));
        check(new RegisteredPlayerHistory(historyFile).snapshot().equals(Set.of(PLAYER, eligibleZero, bannedZero, missing)));
        first.revoke();

        Path secondRoot = dir.resolve("restart");
        Path playerDir = secondRoot.resolve("world/playerdata");
        Files.createDirectories(playerDir);
        for (UUID id : Set.of(eligibleZero, bannedZero, neverRegistered)) {
            try (var out = new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(playerDir.resolve(id + ".dat"))))) {
                out.writeByte(10); out.writeUTF("");
                out.writeByte(9); out.writeUTF("Inventory"); out.writeByte(10); out.writeInt(0);
                out.writeByte(9); out.writeUTF("EnderItems"); out.writeByte(10); out.writeInt(0);
                out.writeByte(0);
            }
        }
        var captured = capture(secondRoot);
        var bans = BanSourceCoverage.captureCompleteLists(Set.of("profile"), Map.of("profile",Set.of(bannedZero)));
        var restarted = new SettlementRuntime(new AdmissionBarrier(), new WriterEpochFence(), new RegisteredPlayerHistory(historyFile));
        var secondAttempt = restarted.beginMemory(() -> true, () -> true);
        var next = restarted.prepareMemory(secondAttempt, () ->
                new SettlementRuntime.Captured(captured.staged(), captured.manifest(), bans));
        check(restarted.completeMemory(next, ready) == SettlementWriterEpochDecision.Verdict.TRY_ATOMIC_PUBLISH);
        check(restarted.current().orElseThrow().personalBalances().equals(Map.of(PLAYER, BigInteger.valueOf(5000), eligibleZero, BigInteger.ZERO)));
        check(restarted.current().orElseThrow().free().equals(BigInteger.ZERO));
        check(restarted.current().orElseThrow().dead().equals(BigInteger.ZERO));

        // An empty LIVE private container is itself a registration reason,
        // independent of monetary slots and prior registration history.
        var freshCapture = capture(secondRoot);
        var fresh = new SettlementRuntime(new AdmissionBarrier(),new WriterEpochFence());
        var freshAttempt = fresh.beginMemory(() -> true, () -> true);
        var freshPrepared = fresh.prepareMemory(freshAttempt, () ->
                new SettlementRuntime.Captured(freshCapture.staged(), freshCapture.manifest(), bans,
                        Set.of(eligibleZero,bannedZero,missing)));
        check(freshPrepared.summary().personal().equals(Map.of(PLAYER,BigInteger.valueOf(5000),eligibleZero,BigInteger.ZERO)));
        check(fresh.completeMemory(freshPrepared,ready)==SettlementWriterEpochDecision.Verdict.TRY_ATOMIC_PUBLISH);
        check(!fresh.current().orElseThrow().personalBalances().containsKey(neverRegistered));

    }
    public static void main(String[] args) throws Exception {
        run(Files.createTempDirectory("settlement-runtime-checks"));
        runMemory(Files.createTempDirectory("memory-settlement-runtime-checks"));
        runRegistrationRestart(Files.createTempDirectory("registration-restart-checks"));
        System.out.println("SETTLEMENT_RUNTIME_CHECKS_PASSED");
    }
}
