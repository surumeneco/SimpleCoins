package co.surumene.simplecoins.domain;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
final class GrantPartialCustodyTest {
    @Test void durablePartialCustody(@TempDir Path root) throws Exception {
        GrantPartialCustodyChecks.run(root);
    }
}
