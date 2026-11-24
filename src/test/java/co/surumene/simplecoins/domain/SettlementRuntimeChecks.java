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
    public static void main(String[] args) throws Exception {
        run(Files.createTempDirectory("settlement-runtime-checks"));
        runMemory(Files.createTempDirectory("memory-settlement-runtime-checks"));
        System.out.println("SETTLEMENT_RUNTIME_CHECKS_PASSED");
    }
}
