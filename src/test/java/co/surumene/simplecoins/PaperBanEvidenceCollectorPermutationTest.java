package co.surumene.simplecoins;

import static org.junit.jupiter.api.Assertions.*;

import co.surumene.simplecoins.domain.BanEvidence;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/**
 * Code-to-spec characterization of the existing BAN source collector.
 * The declared inventory's iteration order must not affect the decision.
 */
class PaperBanEvidenceCollectorPermutationTest {
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000077");

    private static BanEvidence.Status evaluate(
            Map<String, Function<UUID, Boolean>> sources, boolean complete) throws Exception {
        return PaperBanEvidenceCollector.read(
                OWNER,
                new BanEvidence.Source("paper-profile", BanEvidence.Status.CLEAR),
                new BanEvidence.Source("ip-ban", BanEvidence.Status.CLEAR),
                complete, sources).status();
    }

    @Test void declaredPluginOrderCannotChangeClearOrBanDecision() throws Exception {
        Map<String, Function<UUID, Boolean>> forward = new LinkedHashMap<>();
        forward.put("first", owner -> false);
        forward.put("second", owner -> true);
        Map<String, Function<UUID, Boolean>> reverse = new LinkedHashMap<>();
        reverse.put("second", owner -> true);
        reverse.put("first", owner -> false);
        assertEquals(BanEvidence.Status.BANNED, evaluate(forward, true));
        assertEquals(BanEvidence.Status.BANNED, evaluate(reverse, true));

        forward.replace("second", owner -> false);
        reverse.replace("second", owner -> false);
        assertEquals(BanEvidence.Status.CLEAR, evaluate(forward, true));
        assertEquals(BanEvidence.Status.CLEAR, evaluate(reverse, true));
    }

    @Test void eachDeclaredPluginIsQueriedOnceAndIncompleteInventoryStaysUnknown()
            throws Exception {
        AtomicInteger firstCalls = new AtomicInteger();
        AtomicInteger secondCalls = new AtomicInteger();
        Map<String, Function<UUID, Boolean>> sources = new LinkedHashMap<>();
        sources.put("first", owner -> {
            assertEquals(OWNER, owner);
            firstCalls.incrementAndGet();
            return false;
        });
        sources.put("second", owner -> {
            assertEquals(OWNER, owner);
            secondCalls.incrementAndGet();
            return false;
        });
        assertEquals(BanEvidence.Status.UNKNOWN, evaluate(sources, false));
        assertEquals(1, firstCalls.get());
        assertEquals(1, secondCalls.get());
    }
}
