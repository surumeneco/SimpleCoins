package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Existing managed resources must be checked without loading their entire bytes into RAM. */
class ManagedResourcesStreamingDigestTest {
    private static final String PACK = "packs/simplecoins.mcpack";

    @TempDir Path directory;

    @Test void existingLargeOwnedPackIsCheckedWithinSmallHeap() throws Exception {
        Path pack = directory.resolve(PACK);
        Files.createDirectories(pack.getParent());
        // Sparse file: large enough to exceed the child JVM's 64 MiB heap.
        try (RandomAccessFile file = new RandomAccessFile(pack.toFile(), "rw")) {
            file.setLength(72L * 1024 * 1024);
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] zeroChunk = new byte[8192];
        for (int i = 0; i < 72 * 1024 * 1024 / zeroChunk.length; i++) digest.update(zeroChunk);
        String manifest = HexFormat.of().formatHex(digest.digest()) + " " + PACK + "\n";
        DurableFile.write(directory.resolve("simplecoins-managed-resources"),
                manifest.getBytes(StandardCharsets.UTF_8));

        // Isolate the heap limit rather than modifying the heap used by unrelated JUnit cases.
        Path javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java");
        String classpath = Path.of(ManagedResources.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI()).toString()
                + java.io.File.pathSeparator
                + Path.of(Probe.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Process process = new ProcessBuilder(javaExecutable.toString(), "-Xmx64m", "-cp", classpath,
                Probe.class.getName(), directory.toString())
                .redirectErrorStream(true).start();
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            fail("bounded-heap digest probe timed out");
        }
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, process.exitValue(), "ManagedResources must not materialize the old 72 MiB file: " + output);
        assertArrayEquals(new byte[] {7}, Files.readAllBytes(pack));
    }

    /** A JUnit-independent child process whose only large input is a pre-existing owned pack. */
    public static final class Probe {
        public static void main(String[] args) throws Exception {
            new ManagedResources(Path.of(args[0])).replace(Map.of(PACK, new byte[] {7}));
        }
    }
}
