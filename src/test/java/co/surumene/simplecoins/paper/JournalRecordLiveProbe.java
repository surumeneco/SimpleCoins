package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.NormalMutationJournal;
import java.lang.reflect.Field;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Isolated CI-only fixture: writes a PREPARED record but no physical coins.
 * Never enable on a production game world.
 */
public final class JournalRecordLiveProbe extends JavaPlugin {
    @Override public void onEnable() {
        try {
            Plugin production = Bukkit.getPluginManager().getPlugin("SimpleCoins");
            if (production == null || !production.isEnabled())
                throw new IllegalStateException("Guarded SimpleCoins plugin unavailable");
            Field field = production.getClass().getDeclaredField("normalJournal");
            field.setAccessible(true);
            Object raw = field.get(production);
            if (!(raw instanceof NormalMutationJournal journal))
                throw new IllegalStateException("No live normal mutation journal");
            var prepared = journal.prepare(
                    UUID.fromString("7bf1ac01-4b51-4d68-8c6e-982fae4c0003"),
                    NormalMutationJournal.Account.external(),
                    NormalMutationJournal.Account.free(), 1L);
            if (!prepared.newlyCreated() || prepared.mutation().sequence() != 1L)
                throw new IllegalStateException("Test journal fixture preparation failed");
            getLogger().info("PAPER_JOURNAL_RECORD_TEST_PREPARED: 1.mutation,"
                    + " proposed movement only, physical money not issued");
        } catch (Exception error) {
            getLogger().log(Level.SEVERE, "PAPER_JOURNAL_RECORD_TEST_FAILED", error);
            Bukkit.shutdown();
            throw new IllegalStateException("Unable to prepare test-only journal fixture", error);
        }
    }
}
