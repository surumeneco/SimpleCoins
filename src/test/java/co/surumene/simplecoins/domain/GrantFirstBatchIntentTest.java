package co.surumene.simplecoins.domain;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
final class GrantFirstBatchIntentTest {
    @TempDir Path root;
    @Test void firstBatchIntentSurvivesRestartWithoutOverwrite() throws Exception {
        GrantFirstBatchIntentChecks.run(root);
    }
}
