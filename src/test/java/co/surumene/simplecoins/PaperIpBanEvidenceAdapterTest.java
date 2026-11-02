package co.surumene.simplecoins;

import static org.junit.jupiter.api.Assertions.*;

import co.surumene.simplecoins.domain.BanEvidence;
import java.net.InetAddress;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PaperIpBanEvidenceAdapterTest {
    private static final InetAddress FIRST = address(1);
    private static final InetAddress SECOND = address(2);

    private static InetAddress address(int lastOctet) {
        try {
            return InetAddress.getByAddress(new byte[]{127, 0, 0, (byte) lastOctet});
        } catch (java.net.UnknownHostException impossible) {
            throw new AssertionError(impossible);
        }
    }

    @Test void completeMappingAndSuccessfulLookupsCanProveClear() {
        var result = PaperIpBanEvidenceAdapter.read(Set.of(FIRST, SECOND), true,
                address -> false);
        assertEquals("ip-ban", result.id());
        assertEquals(BanEvidence.Status.CLEAR, result.status());
    }

    @Test void anyVerifiedBannedAddressOverridesUnknownOthers() {
        assertEquals(BanEvidence.Status.BANNED,
                PaperIpBanEvidenceAdapter.read(Set.of(FIRST, SECOND), false,
                        address -> address.equals(SECOND)).status());
        assertEquals(BanEvidence.Status.BANNED,
                PaperIpBanEvidenceAdapter.read(Set.of(FIRST, SECOND), true,
                        address -> address.equals(SECOND) ? true : null).status());
    }

    @Test void MissingOrIncompleteMappingCannotProveClear() {
        assertEquals(BanEvidence.Status.UNKNOWN,
                PaperIpBanEvidenceAdapter.read(Set.of(FIRST), false,
                        address -> false).status());
        assertEquals(BanEvidence.Status.UNKNOWN,
                PaperIpBanEvidenceAdapter.read(Set.of(), true,
                        address -> false).status());
        assertEquals(BanEvidence.Status.UNKNOWN,
                PaperIpBanEvidenceAdapter.read(null, true,
                        address -> false).status());
        assertEquals(BanEvidence.Status.UNKNOWN,
                PaperIpBanEvidenceAdapter.read(Set.of(FIRST), true,
                        null).status());
    }

    @Test void FailedOrNullIpLookupRemainsUnknown() {
        assertEquals(BanEvidence.Status.UNKNOWN,
                PaperIpBanEvidenceAdapter.read(Set.of(FIRST), true,
                        address -> null).status());
        assertEquals(BanEvidence.Status.UNKNOWN,
                PaperIpBanEvidenceAdapter.read(Set.of(FIRST), true,
                        address -> { throw new IllegalStateException("IP list unavailable"); })
                        .status());
    }

    @Test void MissingIpSourcePreventsGlobalClearEvenWithPaperProfileClear() throws Exception {
        var paper = PaperBanEvidenceAdapter.read(
                java.util.UUID.fromString("00000000-0000-0000-0000-000000000033"),
                uuid -> false);
        var ip = PaperIpBanEvidenceAdapter.read(Set.of(), false, address -> false);
        var decision = BanEvidence.evaluate(Set.of("paper-profile", "ip-ban"),
                List.of(paper, ip));
        assertEquals(BanEvidence.Status.UNKNOWN, decision.status());
    }
}
