package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GeyserResourceInstallerTest {
    @TempDir Path temp;

    private JavaCurrencyPack.Artifacts candidate() throws IOException {
        Map<CoinDenomination,String> names = new EnumMap<>(CoinDenomination.class);
        for (var d : CoinDenomination.values()) names.put(d, d.value() + " Coin");
        return JavaCurrencyPack.build(temp.resolve("source"), names);
    }

    @Test void rejectsGeyserMappingWhenCoinModelAndBedrockIdentifierDisagree()
            throws Exception {
        var generated = candidate();
        String mapping = Files.readString(generated.bedrockMapping());
        char quote = '"';
        String valid = quote + "model" + quote + ":" + quote + "simplecoins:coin_5" + quote;
        String invalid = quote + "model" + quote + ":" + quote + "simplecoins:coin_1" + quote;
        assertTrue(mapping.contains(valid));
        Path changed = temp.resolve("wrong-model.json");
        Files.writeString(changed, mapping.replace(valid, invalid));
        Path provider = temp.resolve("wrong-model");
        assertThrows(IOException.class, () -> GeyserResourceInstaller.stage(
                provider, generated.bedrockArchive(), changed, true));
        assertFalse(Files.exists(provider.resolve("simplecoins-managed-resources")),
                "Invalid Geyser model must never be installed or claimed");
    }

    @Test void rejectsMappingWithAppendedJsonOrInvalidUtf8() throws Exception {
        var generated = candidate();
        byte[] original = Files.readAllBytes(generated.bedrockMapping());
        byte[] invalidUtf8 = java.util.Arrays.copyOf(original, original.length + 1);
        invalidUtf8[original.length] = (byte) 0xC3;
        Path malformed = temp.resolve("invalid-utf8.json");
        Files.write(malformed, invalidUtf8);
        Path utf8Provider = temp.resolve("invalid-utf8-provider");
        assertThrows(IOException.class, () -> GeyserResourceInstaller.stage(
                utf8Provider, generated.bedrockArchive(), malformed, true));
        assertFalse(Files.exists(utf8Provider.resolve("simplecoins-managed-resources")));

        Path appended = temp.resolve("appended-json.json");
        Files.writeString(appended, Files.readString(generated.bedrockMapping())
                + "\n{\"format_version\":2}");
        Path appendedProvider = temp.resolve("extra-json-provider");
        assertThrows(IOException.class, () -> GeyserResourceInstaller.stage(
                appendedProvider, generated.bedrockArchive(), appended, true));
        assertFalse(Files.exists(appendedProvider.resolve("simplecoins-managed-resources")));
    }

    @Test void rejectsMappingWithWrongIconOrStackComponent() throws Exception {
        var generated = candidate();
        String mapping = Files.readString(generated.bedrockMapping());
        Path invalidIcon = temp.resolve("wrong-icon.json");
        String icon = "\"icon\":\"simplecoins:coin_5\"";
        assertTrue(mapping.contains(icon));
        Files.writeString(invalidIcon, mapping.replace(icon, "\"icon\":\"simplecoins:coin_1\""));
        Path iconProvider = temp.resolve("wrong-icon-provider");
        assertThrows(IOException.class, () -> GeyserResourceInstaller.stage(
                iconProvider, generated.bedrockArchive(), invalidIcon, true));
        assertFalse(Files.exists(iconProvider.resolve("simplecoins-managed-resources")));

        Path invalidStack = temp.resolve("bad-stack-size.json");
        String stack = "\"minecraft:max_stack_size\":64";
        assertTrue(mapping.contains(stack));
        Files.writeString(invalidStack, mapping.replace(stack, "\"minecraft:max_stack_size\":32"));
        Path stackProvider = temp.resolve("bad-stack-provider");
        assertThrows(IOException.class, () -> GeyserResourceInstaller.stage(
                stackProvider, generated.bedrockArchive(), invalidStack, true));
        assertFalse(Files.exists(stackProvider.resolve("simplecoins-managed-resources")));
    }

    @Test void installsTwoOwnedGeyserArtifactsWithoutTouchingForeignPacks() throws Exception {
        var staged = candidate();
        Path provider = temp.resolve("Geyser-Spigot");
        Files.createDirectories(provider.resolve("packs"));
        Files.writeString(provider.resolve("packs/another-plugin.mcpack"), "other-owned-content");
        var result = GeyserResourceInstaller.stage(provider, staged.bedrockArchive(),
                staged.bedrockMapping(), true);
        assertTrue(result.changed());
        assertArrayEquals(Files.readAllBytes(staged.bedrockArchive()),
                Files.readAllBytes(provider.resolve("packs/simplecoins-generated.mcpack")));
        assertEquals(Files.readString(staged.bedrockMapping()),
                Files.readString(provider.resolve("custom_mappings/simplecoins-generated.json")));
        assertTrue(Files.isRegularFile(provider.resolve("simplecoins-managed-resources")));
        assertEquals("other-owned-content",
                Files.readString(provider.resolve("packs/another-plugin.mcpack")));
        assertFalse(GeyserResourceInstaller.stage(provider,
                staged.bedrockArchive(), staged.bedrockMapping(), true).changed());
    }

    @Test void modifiedManagedResourceFailsInsteadOfOverwritingAnotherOwnersChanges() throws Exception {
        var staged = candidate();
        Path provider = temp.resolve("mutated");
        GeyserResourceInstaller.stage(provider, staged.bedrockArchive(),
                staged.bedrockMapping(), true);
        Files.writeString(provider.resolve("custom_mappings/simplecoins-generated.json"), "tampered");
        assertThrows(IOException.class, () -> GeyserResourceInstaller.stage(provider,
                staged.bedrockArchive(), staged.bedrockMapping(), true));
        assertEquals("tampered",
                Files.readString(provider.resolve("custom_mappings/simplecoins-generated.json")));
    }

    @Test void nexoModeRemovesOnlyPriorOwnedResources() throws Exception {
        var staged = candidate();
        Path provider = temp.resolve("delegated");
        Files.createDirectories(provider.resolve("packs"));
        Files.writeString(provider.resolve("packs/another.mcpack"), "keep");
        GeyserResourceInstaller.stage(provider, staged.bedrockArchive(),
                staged.bedrockMapping(), true);
        assertTrue(GeyserResourceInstaller.removeOwned(provider));
        assertFalse(Files.exists(provider.resolve("packs/simplecoins-generated.mcpack")));
        assertFalse(Files.exists(provider.resolve("custom_mappings/simplecoins-generated.json")));
        assertEquals("keep", Files.readString(provider.resolve("packs/another.mcpack")));
        assertFalse(GeyserResourceInstaller.removeOwned(provider));
    }

    @Test void absentCustomContentOrMalformedArchiveIsFailClosed() throws Exception {
        var staged = candidate();
        Path provider = temp.resolve("bad");
        assertThrows(IOException.class, () -> GeyserResourceInstaller.stage(provider,
                staged.bedrockArchive(), staged.bedrockMapping(), false));
        Path fake = temp.resolve("not-zip.mcpack");
        Files.writeString(fake, "not a Bedrock pack");
        assertThrows(IOException.class, () -> GeyserResourceInstaller.stage(provider,
                fake, staged.bedrockMapping(), true));
        assertFalse(Files.exists(provider.resolve("simplecoins-managed-resources")));
    }

    @Test void doNotClaimUnownedExistingGeyserArtifact() throws Exception {
        var staged = candidate();
        Path provider = temp.resolve("foreign");
        Files.createDirectories(provider.resolve("packs"));
        Files.writeString(provider.resolve("packs/simplecoins-generated.mcpack"), "foreign");
        assertThrows(IOException.class, () -> GeyserResourceInstaller.stage(provider,
                staged.bedrockArchive(), staged.bedrockMapping(), true));
        assertEquals("foreign",
                Files.readString(provider.resolve("packs/simplecoins-generated.mcpack")));
    }
}
