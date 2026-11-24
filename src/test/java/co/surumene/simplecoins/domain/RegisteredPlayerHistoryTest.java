package co.surumene.simplecoins.domain;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class RegisteredPlayerHistoryTest {
    @TempDir Path temp;
    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID C = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID NEVER = UUID.fromString("00000000-0000-0000-0000-000000000004");

    @Test void restartRestoresOnlyPreviouslyRegisteredEligiblePlayers() throws Exception {
        Path file = temp.resolve("registered-players.txt");
        var history = new RegisteredPlayerHistory(file);
        assertEquals(Set.of(), history.snapshot());
        history.remember(Set.of(A, B, C));
        var restarted = new RegisteredPlayerHistory(file);
        var bans = BanSourceCoverage.captureCompleteLists(Set.of("profile"), Map.of("profile", Set.of(B)));
        assertEquals(Set.of(A), restarted.eligible(Set.of(A, B, NEVER), bans));
        assertEquals(Set.of(A, B, C), restarted.snapshot());
        assertEquals(Set.of(A, B), restarted.eligible(Set.of(A, B, NEVER),
                BanSourceCoverage.captureCompleteLists(Set.of("profile"), Map.of("profile", Set.of()))));
    }

    @Test void remembersOnlyNewUuidsAndRejectsMalformedExistingHistory() throws Exception {
        Path file = temp.resolve("registered-players.txt");
        var history = new RegisteredPlayerHistory(file);
        history.remember(Set.of(A));
        var before = Files.getLastModifiedTime(file);
        history.remember(Set.of(A));
        assertEquals(before, Files.getLastModifiedTime(file));
        Files.writeString(file, "not-a-uuid\n");
        assertThrows(java.io.IOException.class, () -> new RegisteredPlayerHistory(file));
    }

    @Test void missingBanEvidenceIsNotClear() throws Exception {
        var history = new RegisteredPlayerHistory(temp.resolve("registered-players.txt"));
        history.remember(Set.of(A));
        var partial = BanSourceCoverage.capture(Set.of("profile"), Map.of());
        assertThrows(java.io.IOException.class, () -> history.eligible(Set.of(A), partial));
    }
}
