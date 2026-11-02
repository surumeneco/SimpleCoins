package co.surumene.simplecoins;

import static org.junit.jupiter.api.Assertions.*;

import co.surumene.simplecoins.domain.BanEvidence;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class PaperBanEvidenceCollectorTest {
    private static final UUID OWNER =
            UUID.fromString("00000000-0000-0000-0000-000000000077");

    private static BanEvidence.Source profile(BanEvidence.Status status) {
        return new BanEvidence.Source("paper-profile", status);
    }
    private static BanEvidence.Source ip(BanEvidence.Status status) {
        return new BanEvidence.Source("ip-ban", status);
    }

    @Test void onlyCompleteClearEvidenceCanClassifyAnUnbannedOwner() throws Exception {
        assertEquals(BanEvidence.Status.CLEAR, PaperBanEvidenceCollector.read(
                OWNER, profile(BanEvidence.Status.CLEAR), ip(BanEvidence.Status.CLEAR),
                true, Map.of("external-plugin", id -> false)).status());
        assertEquals(BanEvidence.Status.UNKNOWN, PaperBanEvidenceCollector.read(
                OWNER, profile(BanEvidence.Status.CLEAR), ip(BanEvidence.Status.CLEAR),
                false, Map.of()).status());
        assertEquals(BanEvidence.Status.UNKNOWN, PaperBanEvidenceCollector.read(
                OWNER, profile(BanEvidence.Status.CLEAR), ip(BanEvidence.Status.UNKNOWN),
                true, Map.of()).status());
    }

    @Test void confirmedBanDominatesUnknownExternalSources() throws Exception {
        assertEquals(BanEvidence.Status.BANNED, PaperBanEvidenceCollector.read(
                OWNER, profile(BanEvidence.Status.BANNED), ip(BanEvidence.Status.UNKNOWN),
                false, Map.of("external-plugin", id -> null)).status());
        assertEquals(BanEvidence.Status.BANNED, PaperBanEvidenceCollector.read(
                OWNER, profile(BanEvidence.Status.CLEAR), ip(BanEvidence.Status.CLEAR),
                true, Map.of("external-plugin", id -> true)).status());
    }

    @Test void confirmedExternalBanDominatesIncompletePluginInventory() throws Exception {
        // BAN-COLLECT-01/03: missing plugin inventory prevents CLEAR, but
        // must not erase an independently verified positive BAN result.
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        var decision = PaperBanEvidenceCollector.read(
                OWNER, profile(BanEvidence.Status.CLEAR), ip(BanEvidence.Status.UNKNOWN),
                false, Map.of("verified-plugin", uuid -> {
                    assertEquals(OWNER, uuid);
                    calls.incrementAndGet();
                    return true;
                }));
        assertEquals(BanEvidence.Status.BANNED, decision.status());
        assertEquals(1, calls.get(), "A required plugin should be queried once per UUID");
    }

    @Test void incompletePluginInventoryWithClearLookupsRemainsUnknown() throws Exception {
        // BAN-COLLECT-02: a known plugin's CLEAR cannot prove all plugins clear
        // when the authoritative plugin inventory itself is incomplete.
        assertEquals(BanEvidence.Status.UNKNOWN, PaperBanEvidenceCollector.read(
                OWNER, profile(BanEvidence.Status.CLEAR), ip(BanEvidence.Status.CLEAR),
                false, Map.of("verified-plugin", uuid -> false)).status());
    }

    @Test void unavailableOrDuplicateExternalSourcesFailClosed() throws Exception {
        Map<String, Function<UUID, Boolean>> missing = new HashMap<>();
        missing.put("external-plugin", null);
        assertEquals(BanEvidence.Status.UNKNOWN, PaperBanEvidenceCollector.read(
                OWNER, profile(BanEvidence.Status.CLEAR), ip(BanEvidence.Status.CLEAR),
                true, missing).status());
        assertEquals(BanEvidence.Status.UNKNOWN, PaperBanEvidenceCollector.read(
                OWNER, profile(BanEvidence.Status.CLEAR), ip(BanEvidence.Status.CLEAR),
                true, Map.of("external-plugin", id -> {
                    throw new IllegalStateException("Plugin lookup unavailable");
                })).status());
        assertThrows(IOException.class, () -> PaperBanEvidenceCollector.read(
                OWNER, profile(BanEvidence.Status.CLEAR), ip(BanEvidence.Status.CLEAR),
                true, Map.of("Plugin", id -> false, " plugin ", id -> false)));
        assertThrows(IOException.class, () -> PaperBanEvidenceCollector.read(
                null, profile(BanEvidence.Status.CLEAR), ip(BanEvidence.Status.CLEAR),
                true, Map.of()));
    }
}
