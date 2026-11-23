package co.surumene.simplecoins.a05probe;

import co.surumene.simplecoins.domain.AdmissionBarrier;
import co.surumene.simplecoins.domain.SettlementAccumulator;
import co.surumene.simplecoins.domain.SettlementScanRunner;
import co.surumene.simplecoins.domain.SettlementWriterEpochDecision;
import co.surumene.simplecoins.domain.WorldStorageDiscovery;
import co.surumene.simplecoins.domain.WriterEpochFence;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * Disposable Paper-only G1/G0 integration. The TRUE Confirmed-A01 evidence
 * below is TEST DATA, NOT a credential to publish a real settlement. It does
 * NOT instrument loaded Paper/WCL/BlockLocker/NMS/async/file writers.
 *
 * Reflective product access is intentional: SettlementProtectedPublisher is
 * package-private and unregistered until actual writer closure is proven.
 */
final class BoundedStagedPublicationProbe {
    private BoundedStagedPublicationProbe() {}

    static SettlementAccumulator.Result commitAndVerify(
            SettlementScanRunner.Staged realStage,
            WorldStorageDiscovery.Manifest originalManifest) throws Exception {
        if (!Bukkit.isPrimaryThread() || !Bukkit.getOnlinePlayers().isEmpty())
            throw new IOException("Bounded Paper publication test requires isolated main thread");
        if (realStage.savedContentWitness() == null || originalManifest.origin() == null)
            throw new IOException("Actual saved Stage/discovery was not source-attested");
        realStage.savedContentWitness().requireOriginalManifest(originalManifest);

        // Test-local independent gate. Never use the actual product/guard
        // AdmissionBarrier or invoke SimpleCoinsPlugin startup controls.
        WriterEpochFence isolatedFence = new WriterEpochFence();
        isolatedFence.arm(); // positive-control ONLY; not installed-writer closure
        WriterEpochFence.Seal seal = isolatedFence.seal();
        AdmissionBarrier isolatedGate = new AdmissionBarrier();
        long generation = isolatedGate.begin();
        Class<?> product = Class.forName(
                "co.surumene.simplecoins.domain.SettlementProtectedPublisher");
        Constructor<?> constructor = product.getDeclaredConstructor(
                WriterEpochFence.class, WriterEpochFence.Seal.class,
                AdmissionBarrier.class, BooleanSupplier.class, BooleanSupplier.class);
        constructor.setAccessible(true);
        // TRUE is artificial downstream authorization test input. No actual
        // physical writer or normal mutation-journal handoff is proven here.
        Object publisher = constructor.newInstance(isolatedFence, seal, isolatedGate,
                (BooleanSupplier) () -> true,
                (BooleanSupplier) () -> Bukkit.getOnlinePlayers().isEmpty());
        Method staged = product.getDeclaredMethod("evaluateAndPublishStaged",
                long.class, SettlementScanRunner.Staged.class,
                WorldStorageDiscovery.Manifest.class,
                SettlementWriterEpochDecision.Evidence.class);
        staged.setAccessible(true);
        Method current = product.getDeclaredMethod("current");
        current.setAccessible(true);

        SettlementWriterEpochDecision.Evidence fixtureOnly =
                new SettlementWriterEpochDecision.Evidence(
                        true, true, true, true, true, true, true, false);
        Object verdict;
        try {
            verdict = staged.invoke(publisher, generation,
                    realStage, originalManifest, fixtureOnly);
        } catch (InvocationTargetException failure) {
            throw new IOException("Real Paper source-attested product publisher failed",
                    failure.getCause());
        }
        if (verdict != SettlementWriterEpochDecision.Verdict.TRY_ATOMIC_PUBLISH
                || isolatedGate.state() != AdmissionBarrier.State.READY
                || isolatedFence.state() != WriterEpochFence.State.SEALED)
            throw new IOException("Bounded source publisher did not atomically stage and publish");

        @SuppressWarnings("unchecked")
        Optional<SettlementAccumulator.Result> published =
                (Optional<SettlementAccumulator.Result>) current.invoke(publisher);
        SettlementAccumulator.Result result = published.orElseThrow(
                () -> new IOException("Paper G1 source-staged money was not published in test gate"));

        // The product's actual independent Guard still denies. Reflect the
        // API from SimpleCoins' real plugin ClassLoader instead of assuming
        // test-only code is registered as a guard permit.
        Plugin actualPlugin = Bukkit.getPluginManager().getPlugin("SimpleCoins");
        if (actualPlugin == null || !actualPlugin.isEnabled())
            throw new IOException("SimpleCoins product not enabled in isolated Paper test");
        Class<?> permitApi = Class.forName(
                "co.surumene.simplecoins.guard.api.AdmissionPermit", true,
                actualPlugin.getClass().getClassLoader());
        @SuppressWarnings({"rawtypes", "unchecked"})
        Object permit = Bukkit.getServicesManager().load((Class) permitApi);
        if (permit == null || !permitApi.isInstance(permit))
            throw new IOException("Real independent admission permit service missing");
        if (Boolean.TRUE.equals(permitApi.getMethod("mayAdmit").invoke(permit)))
            throw new IOException("Real SimpleCoins AdmissionPermit was unexpectedly opened");

        // Revoke the test-only epoch, not the real player's admission status.
        // Reader must lose its cached published view on the next access.
        isolatedFence.invalidate();
        @SuppressWarnings("unchecked")
        Optional<SettlementAccumulator.Result> afterFault =
                (Optional<SettlementAccumulator.Result>) current.invoke(publisher);
        if (afterFault.isPresent() || isolatedGate.state() != AdmissionBarrier.State.FAILED)
            throw new IOException("Test-only published view survived writer epoch invalidation");
        if (Boolean.TRUE.equals(permitApi.getMethod("mayAdmit").invoke(permit)))
            throw new IOException("Faulted test-only epoch incorrectly allowed real player login");
        return result;
    }
}
