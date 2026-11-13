package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Explicit complete BAN-source evidence before any monetary owner is marked clear. */
class BanSourceCoverageTest {
    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-000000000051");
    private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-000000000052");
    private static final Set<String> REQUIRED = Set.of("paper-profile", "paper-ip", "external:ban");

    private static BanEvidence.Source source(String id, BanEvidence.Status state) {
        return new BanEvidence.Source(id, state);
    }

    private static List<BanEvidence.Source> allClear() {
        return List.of(source("paper-profile", BanEvidence.Status.CLEAR),
                source("paper-ip", BanEvidence.Status.CLEAR),
                source("external:ban", BanEvidence.Status.CLEAR));
    }

    @Test void completeSourcesDistinguishIpBanFromFullyClearPlayer() throws Exception {
        var snapshot = BanSourceCoverage.capture(REQUIRED, Map.of(
                ALICE, allClear(),
                BOB, List.of(source("paper-profile", BanEvidence.Status.CLEAR),
                        source("paper-ip", BanEvidence.Status.BANNED),
                        source("external:ban", BanEvidence.Status.CLEAR))));
        assertFalse(snapshot.currentlyBanned(ALICE));
        assertTrue(snapshot.currentlyBanned(BOB));
        assertEquals(Set.of(BOB), snapshot.bannedAmong(Set.of(ALICE, BOB)));
    }

    @Test void missingExternalSourceIsNotCompleteEvenIfPaperSourceSaysBanned() {
        assertThrows(IOException.class, () -> BanSourceCoverage.capture(REQUIRED,
                Map.of(ALICE, List.of(
                        source("paper-profile", BanEvidence.Status.BANNED),
                        source("paper-ip", BanEvidence.Status.CLEAR)))));
    }

    @Test void unknownExternalSourceIsNotEquivalentToClearOrBanned() {
        assertThrows(IOException.class, () -> BanSourceCoverage.capture(REQUIRED,
                Map.of(ALICE, List.of(
                        source("paper-profile", BanEvidence.Status.BANNED),
                        source("paper-ip", BanEvidence.Status.CLEAR),
                        source("external:ban", BanEvidence.Status.UNKNOWN)))));
    }

    @Test void everyOwnerNeedsIndividualEvidenceNotAnEmptyDefault() throws Exception {
        var snapshot = BanSourceCoverage.capture(REQUIRED, Map.of(ALICE, allClear()));
        assertThrows(IOException.class, () -> snapshot.currentlyBanned(BOB));
        assertThrows(IOException.class, () -> snapshot.bannedAmong(Set.of(ALICE, BOB)));
    }

    @Test void duplicatedAndAliasedRequiredProvidersCannotFakeFullCoverage() {
        assertThrows(IOException.class, () -> BanSourceCoverage.capture(
                Set.of("external:ban", " EXTERNAL:BAN "), Map.of(ALICE, allClear())));
        assertThrows(IOException.class, () -> BanSourceCoverage.capture(
                REQUIRED, Map.of(ALICE, List.of(
                        source("paper-profile", BanEvidence.Status.CLEAR),
                        source(" PAPER-PROFILE ", BanEvidence.Status.CLEAR),
                        source("external:ban", BanEvidence.Status.CLEAR)))));
        assertThrows(IOException.class, () -> BanSourceCoverage.capture(
                Set.of(), Map.of(ALICE, allClear())));
    }

    @Test void retainedSnapshotIsImmutableAgainstCallerMutation() throws Exception {
        var list = new ArrayList<>(allClear());
        var reports = new HashMap<UUID, List<BanEvidence.Source>>();
        reports.put(ALICE, list);
        var snapshot = BanSourceCoverage.capture(REQUIRED, reports);
        list.clear();
        reports.clear();
        assertFalse(snapshot.currentlyBanned(ALICE));
    }
}
