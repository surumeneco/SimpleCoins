package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class KnownOfflineRecipientTest {
    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test void resolvesOnlyUniquePreviouslySeenIdentity() {
        var known = List.of(
                new KnownOfflineRecipient.Identity(A, "Alice", true),
                new KnownOfflineRecipient.Identity(B, "Bob", true));
        assertEquals(A, KnownOfflineRecipient.resolve("alice", known));
        assertEquals(A, KnownOfflineRecipient.resolve(A.toString(), known));
        assertThrows(IllegalArgumentException.class,
                () -> KnownOfflineRecipient.resolve("unknown", known));
        assertThrows(IllegalArgumentException.class,
                () -> KnownOfflineRecipient.resolve("@a", known));
    }

    @Test void ambiguousNameAndNeverJoinedAccountFailClosed() {
        assertThrows(IllegalArgumentException.class, () -> KnownOfflineRecipient.resolve("alice",
                List.of(new KnownOfflineRecipient.Identity(A, "Alice", true),
                        new KnownOfflineRecipient.Identity(B, "ALICE", true))));
        assertThrows(IllegalArgumentException.class, () -> KnownOfflineRecipient.resolve("alice",
                List.of(new KnownOfflineRecipient.Identity(A, "Alice", false))));
        assertEquals(A, KnownOfflineRecipient.resolve("alice",
                List.of(new KnownOfflineRecipient.Identity(A, "Alice", true),
                        new KnownOfflineRecipient.Identity(A, "ALICE", true))));
    }
}
