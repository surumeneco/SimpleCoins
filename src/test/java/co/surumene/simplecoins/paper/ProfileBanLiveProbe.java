package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.BanEvidence;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.BanList;
import org.bukkit.plugin.java.JavaPlugin;

/** Isolated Paper-only observation: NEVER ship this test plugin with SimpleCoins. */
public final class ProfileBanLiveProbe extends JavaPlugin {
    private static final UUID BANNED = UUID.fromString("7bf1ac01-4b51-4d68-8c6e-982fae4c0001");
    private static final UUID CLEAR = UUID.fromString("7bf1ac01-4b51-4d68-8c6e-982fae4c0002");

    @Override
    public void onEnable() {
        try {
            if (!Bukkit.isPrimaryThread())
                throw new IOException("Probe did not start on Paper primary thread");
            var banned = PaperProfileBanSource.observe(BANNED);
            var clear = PaperProfileBanSource.observe(CLEAR);
            requireProfile(banned, BanEvidence.Status.BANNED);
            requireProfile(clear, BanEvidence.Status.CLEAR);

            // Paper exposes IP bans as a separate IP-string keyed list.
            // An IP entry does not establish which offline UUID used that IP.
            final String isolatedTestIp = "203.0.113.42";
            @SuppressWarnings("deprecation")
            var ipEntries = Bukkit.getBanList(BanList.Type.IP).getEntries();
            long matchingIpEntries = ipEntries.stream()
                    .filter(entry -> isolatedTestIp.equals(String.valueOf(entry.getTarget())))
                    .count();
            if (matchingIpEntries != 1 || ipEntries.size() != 1)
                throw new IOException("Expected one exact Paper IP-ban entry, got " + ipEntries);
            requireProfile(PaperProfileBanSource.observe(CLEAR), BanEvidence.Status.CLEAR);
            getLogger().info("PAPER_A06_IP_BAN_DISTINCT_FROM_PROFILE_UUID_PASSED"
                    + " bannedIp=" + isolatedTestIp + " clearUuid=" + CLEAR);

            var required = java.util.Set.of("paper-profile", "paper-ip");
            try {
                PaperProfileBanSource.captureCompleteStandard(required, java.util.Map.of());
                throw new AssertionError("Unmapped IP ban was accepted");
            } catch (IOException expected) {
                if (!expected.getMessage().contains("IP BAN")) throw expected;
            }
            Bukkit.unbanIP(isolatedTestIp);
            var complete = PaperProfileBanSource.captureCompleteStandard(required, java.util.Map.of());
            if (!complete.currentlyBanned(BANNED) || complete.currentlyBanned(CLEAR)
                    || complete.currentlyBanned(UUID.randomUUID()))
                throw new IOException("Complete standard BAN lists disagree with profile membership");
            try {
                PaperProfileBanSource.captureCompleteStandard(
                        java.util.Set.of("paper-profile", "paper-ip", "external:missing"), java.util.Map.of());
                throw new AssertionError("Missing external provider accepted");
            } catch (IOException expected) {
                // Expected: declaration is incomplete.
            }
            getLogger().info("PAPER_COMPLETE_STANDARD_BAN_CAPTURE_PASSED");

            // Null and non-primary-thread calls MUST fail; they must not become CLEAR.
            try {
                PaperProfileBanSource.observe(null);
                throw new IOException("Null UUID unexpectedly yielded BAN evidence");
            } catch (IOException expected) {
                if (!expected.getMessage().contains("Missing UUID")) throw expected;
            }
            boolean rejectedAsync = CompletableFuture.supplyAsync(() -> {
                try {
                    PaperProfileBanSource.observe(CLEAR);
                    return false;
                } catch (IOException expected) {
                    return expected.getMessage().contains("primary thread");
                }
            }).get(10, TimeUnit.SECONDS);
            if (!rejectedAsync)
                throw new IOException("Non-primary-thread BAN observation was not rejected");

            getLogger().info("PAPER_PROFILE_BAN_LIVE_PASSED: profile BANNED/CLEAR, "
                    + "null UUID, and off-thread failure verified; no IP/external evidence");
        } catch (Exception failure) {
            getLogger().log(Level.SEVERE, "PAPER_PROFILE_BAN_LIVE_FAILED", failure);
            // Probe only: server must not remain online after an invalid observation.
            Bukkit.shutdown();
            throw new IllegalStateException("Profile BAN live probe failed", failure);
        }
    }

    private static void requireProfile(BanEvidence.Source actual, BanEvidence.Status expected)
            throws IOException {
        if (!PaperProfileBanSource.SOURCE_ID.equals(actual.id()) || actual.status() != expected)
            throw new IOException("Unexpected Paper profile BAN observation: " + actual);
    }
}
