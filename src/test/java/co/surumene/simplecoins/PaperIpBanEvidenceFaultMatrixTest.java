package co.surumene.simplecoins;

import static org.junit.jupiter.api.Assertions.*;

import co.surumene.simplecoins.domain.BanEvidence;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Source-exact pure IP-ban fault-precedence matrix, Candidate IP-FAULT-01..03.
 * The caller must independently verify UUID/address mapping completeness.
 */
class PaperIpBanEvidenceFaultMatrixTest {
    private static final InetAddress FIRST = address(41);
    private static final InetAddress SECOND = address(42);

    private static InetAddress address(int suffix) {
        try {
            return InetAddress.getByAddress(new byte[]{127, 0, 0, (byte) suffix});
        } catch (UnknownHostException impossible) {
            throw new AssertionError(impossible);
        }
    }

    /** 0=BANNED, 1=CLEAR, 2=unavailable/null, 3=lookup exception. */
    private static Boolean lookup(int state) {
        return switch (state) {
            case 0 -> true;
            case 1 -> false;
            case 2 -> null;
            case 3 -> throw new IllegalStateException("BAN list unavailable");
            default -> throw new AssertionError(state);
        };
    }

    @Test void everyTwoAddressFailureCombinationPreservesPositiveBanEvidence() {
        int combinations = 0;
        for (int first = 0; first < 4; first++) {
            for (int second = 0; second < 4; second++) {
                for (boolean complete : new boolean[]{false, true}) {
                    final int a = first;
                    final int b = second;
                    var result = PaperIpBanEvidenceAdapter.read(
                            Set.of(FIRST, SECOND), complete,
                            address -> lookup(address.equals(FIRST) ? a : b));
                    var expected = a == 0 || b == 0 ? BanEvidence.Status.BANNED
                            : complete && a == 1 && b == 1 ? BanEvidence.Status.CLEAR
                            : BanEvidence.Status.UNKNOWN;
                    assertEquals(expected, result.status(),
                            "first=" + a + " second=" + b + " complete=" + complete);
                    assertEquals("ip-ban", result.id());
                    combinations++;
                }
            }
        }
        assertEquals(32, combinations);
    }

    @Test void nullAddressDoesNotErasePositiveBanButPreventsClear() {
        var addresses = new HashSet<InetAddress>();
        addresses.add(FIRST);
        addresses.add(null);
        assertEquals(BanEvidence.Status.BANNED,
                PaperIpBanEvidenceAdapter.read(addresses, true,
                        address -> true).status());
        assertEquals(BanEvidence.Status.UNKNOWN,
                PaperIpBanEvidenceAdapter.read(addresses, true,
                        address -> false).status());
    }
}
