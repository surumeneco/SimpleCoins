package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

/** Stages the generated Java archive for Nexo's own merge and delivery.
 * The returned flag describes the import file, not a successful Nexo rebuild. */
public final class NexoResourceInstaller {
    private static final String PACK = "pack/external_packs/simplecoins-generated.zip";
    private static final int MAX_PACK_BYTES = 32 * 1024 * 1024;

    private NexoResourceInstaller() {}

    public static boolean stage(Path nexoData, Path javaArchive) throws IOException {
        if (nexoData == null || javaArchive == null)
            throw new IOException("Missing Nexo data or generated Java archive");
        Path source = javaArchive.toAbsolutePath().normalize();
        for (Path part = source; part != null; part = part.getParent())
            if (Files.isSymbolicLink(part))
                throw new IOException("Symbolic resource-pack path: " + part);
        if (!Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Missing generated Java archive");
        byte[] pack;
        try (var input = Files.newInputStream(source, LinkOption.NOFOLLOW_LINKS)) {
            pack = DurableFile.readBounded(input, MAX_PACK_BYTES);
        }
        if (pack.length == 0) throw new IOException("Empty generated Java archive");
        return new ManagedResources(nexoData, Set.of(PACK))
                .replaceIfChanged(Map.of(PACK, pack));
    }
}
