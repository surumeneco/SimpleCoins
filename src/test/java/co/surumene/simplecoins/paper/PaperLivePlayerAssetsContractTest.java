package co.surumene.simplecoins.paper;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigInteger;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Pure result oracles; the real Paper Player boundary remains a separate fixture. */
class PaperLivePlayerAssetsContractTest {
    private static final UUID PLAYER_A = UUID.fromString("00000000-0000-0000-0000-000000000a01");
    private static final UUID PLAYER_B = UUID.fromString("00000000-0000-0000-0000-000000000b02");

    @Test void sumsIndependentPartitionsWithoutClaimingCompleteSettlement() {
        var assets = new java.util.ArrayList<>(List.of(
                new PaperLivePlayerAssets.Entry("player/" + PLAYER_A + "/inventory/0",
                        PLAYER_A, BigInteger.valueOf(100)),
                new PaperLivePlayerAssets.Entry("player/" + PLAYER_A + "/armor/0",
                        PLAYER_A, BigInteger.valueOf(500)),
                new PaperLivePlayerAssets.Entry("player/" + PLAYER_A + "/extra/0",
                        PLAYER_A, BigInteger.valueOf(50)),
                new PaperLivePlayerAssets.Entry("player/" + PLAYER_A + "/ender/0",
                        PLAYER_A, BigInteger.valueOf(1000))));
        var report = new PaperLivePlayerAssets.Snapshot(PLAYER_A, 68, assets);
        assets.clear();
        assertEquals(BigInteger.valueOf(1650), report.total());
        assertEquals(4, report.entries().size());
        assertEquals(68, report.scannedSlots());
        assertFalse(report.isCompleteSettlementSource());
        assertThrows(UnsupportedOperationException.class, () -> report.entries().clear());
    }

    @Test void rejectsMalformedAndCrossOwnerSources() {
        assertThrows(IllegalArgumentException.class, () ->
                new PaperLivePlayerAssets.Entry("", PLAYER_A, BigInteger.ONE));
        assertThrows(IllegalArgumentException.class, () ->
                new PaperLivePlayerAssets.Entry("valid", PLAYER_A, BigInteger.ZERO));
        assertThrows(NullPointerException.class, () ->
                new PaperLivePlayerAssets.Entry("valid", null, BigInteger.ONE));
        assertThrows(IllegalArgumentException.class, () ->
                new PaperLivePlayerAssets.Snapshot(PLAYER_A, -1, List.of()));
        assertThrows(IllegalArgumentException.class, () ->
                new PaperLivePlayerAssets.Snapshot(PLAYER_A, 1,
                        List.of(new PaperLivePlayerAssets.Entry("x", PLAYER_B, BigInteger.ONE))));
    }

    @Test void sumsOnlinePlayerSnapshotsWithoutAllowingRepeatedOwnerOrSlotCountMismatch() {
        var first = new PaperLivePlayerAssets.Snapshot(PLAYER_A, 68,
                List.of(new PaperLivePlayerAssets.Entry("a", PLAYER_A, BigInteger.valueOf(700))));
        var second = new PaperLivePlayerAssets.Snapshot(PLAYER_B, 68,
                List.of(new PaperLivePlayerAssets.Entry("b", PLAYER_B, BigInteger.valueOf(300))));
        var mutable = new java.util.ArrayList<>(List.of(first, second));
        var report = new PaperLivePlayerAssets.OnlineSet(2, 136, mutable);
        mutable.clear();
        assertEquals(BigInteger.valueOf(1000), report.total());
        assertFalse(report.isCompleteSettlementSource());
        assertEquals(2, report.players().size());
        assertThrows(UnsupportedOperationException.class, () -> report.players().clear());
        assertThrows(IllegalArgumentException.class, () ->
                new PaperLivePlayerAssets.OnlineSet(2, 136, List.of(first, first)));
        assertThrows(IllegalArgumentException.class, () ->
                new PaperLivePlayerAssets.OnlineSet(2, 135, List.of(first, second)));
        assertThrows(IllegalArgumentException.class, () ->
                new PaperLivePlayerAssets.OnlineSet(1, 136, List.of(first, second)));
    }
}
