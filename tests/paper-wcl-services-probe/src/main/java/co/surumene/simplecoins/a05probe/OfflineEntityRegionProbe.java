package co.surumene.simplecoins.a05probe;

import co.surumene.simplecoins.domain.AnvilRegionReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Test-only offline Anvil Entity NBT inspection. Runs in a standalone JVM
 * AFTER the first Paper process has fully stopped and BEFORE Paper restarts.
 * No Bukkit Server, chunk load, game state modification, or production source.
 */
public final class OfflineEntityRegionProbe {
    private static final String MARKER = "simplecoins-a05-persisted-wwc-probe";
    private static final String WOLF_MARKER = "simplecoins-a05-persisted-www-probe";
    private static final UUID FOLLOW =
            UUID.fromString("00000000-0000-0000-0000-00000000a055");

    private OfflineEntityRegionProbe() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 5)
            throw new IllegalArgumentException(
                    "Expected region, WWC UUID/item and WWW UUID/runtime evidence");
        Path region = Path.of(args[0]).toAbsolutePath().normalize();
        Path uuidFile = Path.of(args[1]).toAbsolutePath().normalize();
        Path shulkerFile = Path.of(args[2]).toAbsolutePath().normalize();
        Path wolfUuidFile = Path.of(args[3]).toAbsolutePath().normalize();
        Path wolfRuntimeFile = Path.of(args[4]).toAbsolutePath().normalize();
        for (Path file : List.of(region, uuidFile, shulkerFile, wolfUuidFile, wolfRuntimeFile))
            if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS))
                throw new IOException("Missing or non-regular required fixture file: " + file);

        UUID expectedEntity = UUID.fromString(Files.readString(uuidFile).strip());
        byte[] expectedItem = Base64.getDecoder().decode(Files.readString(shulkerFile).strip());
        if (expectedItem.length < 8) throw new IOException("Invalid original serialized ItemStack");
        UUID expectedWolf = UUID.fromString(Files.readString(wolfUuidFile).strip());
        if (expectedWolf.equals(expectedEntity))
            throw new IOException("WWC and WWW fixtures must have independent Entity identities");
        byte[] expectedWolfRuntime = Base64.getDecoder().decode(
                Files.readString(wolfRuntimeFile).strip());
        if (expectedWolfRuntime.length < 32)
            throw new IOException("Invalid original WWW v3 runtime payload");
        int[] found = {0};
        int[] foundWolf = {0};
        int[] chunks = {0};

        AnvilRegionReader.scan(region, 2 * 1024 * 1024, 16 * 1024 * 1024, chunk -> {
            chunks[0]++;
            Object entitiesRaw = chunk.nbt().get("Entities");
            if (entitiesRaw == null && chunk.x() == 4 && chunk.z() == 4) {
                throw new IOException("Expected Paper Entity chunk has no Entities list, keys="
                        + chunk.nbt().keySet());
            }
            if (entitiesRaw == null) return;
            if (!(entitiesRaw instanceof List<?> entities))
                throw new IOException("Entity Anvil data is not an NBT list at "
                        + chunk.x() + "," + chunk.z());

            for (Object raw : entities) {
                if (!(raw instanceof Map<?, ?> entity))
                    throw new IOException("Non-compound Entity entry");
                Object tags = entity.get("Tags");
                if (!(tags instanceof List<?> list)) continue;
                if (list.contains(WOLF_MARKER)) {
                    foundWolf[0]++;
                    if (list.contains(MARKER) || chunk.x() != 4 || chunk.z() != 4)
                        throw new IOException("Invalid saved WWW fixture source identity/chunk");
                    UUID wolfId = decodeUuid(entity.get("UUID"));
                    if (!expectedWolf.equals(wolfId) || !"minecraft:wolf".equals(entity.get("id")))
                        throw new IOException("Saved WWW type or UUID differs from first JVM");
                    if (!(entity.get("BukkitValues") instanceof Map<?, ?> wolfPdc))
                        throw new IOException("Saved Wolf lacks Paper BukkitValues PDC");
                    if (!"wonderful_wolf".equals(wolfPdc.get("whatawonderfulwolf:type"))
                            || !(wolfPdc.get("whatawonderfulwolf:schema_version")
                                    instanceof Integer schema) || schema != 1)
                        throw new IOException("Saved Wolf lacks expected WWW schema");
                    if (!(wolfPdc.get("whatawonderfulwolf:runtime_state") instanceof byte[] runtime)
                            || !Arrays.equals(expectedWolfRuntime, runtime))
                        throw new IOException("Saved WWW owner/commander/item runtime source changed");
                    System.out.println("PAPER_A05_OFFLINE_MCA_WWW_MONEY_OWNER_PHYSICAL_SOURCE_PASSED"
                            + " entity=" + wolfId + " chunk=" + chunk.x() + "," + chunk.z()
                            + " runtimeBytes=" + runtime.length);
                    continue;
                }
                if (!list.contains(MARKER)) continue;
                found[0]++;
                if (chunk.x() != 4 || chunk.z() != 4)
                    throw new IOException("Tagged Chicken moved outside expected Anvil chunk");
                UUID physicalId = decodeUuid(entity.get("UUID"));
                if (!expectedEntity.equals(physicalId))
                    throw new IOException("Saved Entity UUID differs from real Paper identity");
                if (!"minecraft:chicken".equals(entity.get("id")))
                    throw new IOException("Tagged Entity changed vanilla type");
                Object values = entity.get("BukkitValues");
                if (!(values instanceof Map<?, ?> pdc))
                    throw new IOException("Saved WWC Entity lacks BukkitValues PDC: " + entity.keySet());
                if (!"wonderful_chicken".equals(pdc.get("whatawonderfulchicken:type")))
                    throw new IOException("Wrong saved WWC PDC marker");
                if (!(pdc.get("whatawonderfulchicken:data_version") instanceof Integer version)
                        || version != 3)
                    throw new IOException("Wrong saved WWC PDC schema");
                if (!FOLLOW.toString().equals(pdc.get("whatawonderfulchicken:follow_target")))
                    throw new IOException("Lost saved WWC follow-target UUID");
                if (!(pdc.get("whatawonderfulchicken:equipment_shulker_box") instanceof byte[] stored)
                        || !Arrays.equals(stored, expectedItem))
                    throw new IOException("Actual Entity MCA lost serialized nested money bytes");
                System.out.println("PAPER_A05_OFFLINE_MCA_WWC_MONEY_PHYSICAL_SOURCE_PASSED"
                        + " entity=" + physicalId + " chunk=" + chunk.x() + "," + chunk.z()
                        + " originalItemBytes=" + expectedItem.length
                        + " source=" + region.getFileName());
            }
        });
        if (found[0] != 1 || foundWolf[0] != 1)
            throw new IOException("Saved WWC/WWW Entity sources require one of each, got WWC="
                    + found[0] + " WWW=" + foundWolf[0] + " in " + chunks[0] + " Anvil chunks");
        System.out.println("PAPER_A05_OFFLINE_MCA_TWO_PROVIDERS_DISTINCT_SOURCES_PASSED");
        System.out.println("PAPER_A05_OFFLINE_MCA_SCAN_NO_CHUNK_LOAD_PASSED");
    }

    private static UUID decodeUuid(Object nbtUuid) throws IOException {
        if (!(nbtUuid instanceof int[] values) || values.length != 4)
            throw new IOException("Expected canonical four-int Entity UUID NBT");
        long most = ((long) values[0] << 32) | (values[1] & 0xffffffffL);
        long least = ((long) values[2] << 32) | (values[3] & 0xffffffffL);
        return new UUID(most, least);
    }
}
