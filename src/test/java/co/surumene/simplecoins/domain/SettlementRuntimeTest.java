package co.surumene.simplecoins.domain;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
final class SettlementRuntimeTest {
    @TempDir Path dir;
    @Test void protectedRuntimeUsesOneActualHandoff() throws Exception { SettlementRuntimeChecks.run(dir); }
}
