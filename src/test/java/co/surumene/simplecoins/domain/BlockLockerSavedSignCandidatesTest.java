package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BlockLockerSavedSignCandidatesTest {
    private static final UUID OWNER = UUID.fromString("00000000-0000-0001-0000-000000000002");

    private static Map<String,Object> privatePdc() {
        return Map.of("blocklocker:header", "PRIVATE",
                "blocklocker:profile_1", Map.of(
                        "blocklocker:n", "Alice",
                        "blocklocker:u", new long[]{OWNER.getMostSignificantBits(),
                                                     OWNER.getLeastSignificantBits()}));
    }

    private static Map<String,Object> sign(String id, int x, Map<String,Object> pdc) {
        return Map.of("id", id, "x", x, "y", 64, "z", 2,
                "PublicBukkitValues", pdc);
    }

    @Test void enumeratesOnlySavedBlockLockerSignHintsWithoutAssigningOwnership() throws Exception {
        Map<String,Object> protectedSign = sign("minecraft:sign", 1, privatePdc());
        Map<String,Object> ordinarySign = Map.of(
                "id", "minecraft:sign", "x", 2, "y", 64, "z", 2);
        Map<String,Object> chest = sign("minecraft:chest", 3, privatePdc());
        var found = BlockLockerSavedSignCandidates.extract(Map.of(
                "block_entities", List.of(protectedSign, ordinarySign, chest)));
        assertEquals(1, found.size());
        assertEquals(OWNER, found.get("1,64,2").firstProfileUuid().orElseThrow());
    }

    @Test void malformedSavedSignAndDuplicateCandidateCoordinatesFailClosed() {
        Map<String,Object> badPdc = sign("minecraft:hanging_sign", 1,
                Map.of("blocklocker:profile_1", Map.of("blocklocker:n", "Alice")));
        assertThrows(IOException.class, () -> BlockLockerSavedSignCandidates.extract(
                Map.of("block_entities", List.of(badPdc))));
        Map<String,Object> duplicate = sign("minecraft:sign", 1, privatePdc());
        assertThrows(IOException.class, () -> BlockLockerSavedSignCandidates.extract(
                Map.of("block_entities", List.of(duplicate, duplicate))));
    }

    @Test void readsNestedBlockLockerProfileFromRealNbtBytes() throws Exception {
        var bytes = new java.io.ByteArrayOutputStream();
        try (var out = new java.io.DataOutputStream(bytes)) {
            out.writeByte(10); out.writeUTF(""); // root compound
            out.writeByte(9); out.writeUTF("block_entities");
            out.writeByte(10); out.writeInt(1); // one compound in list
            out.writeByte(8); out.writeUTF("id"); out.writeUTF("minecraft:sign");
            out.writeByte(3); out.writeUTF("x"); out.writeInt(1);
            out.writeByte(3); out.writeUTF("y"); out.writeInt(64);
            out.writeByte(3); out.writeUTF("z"); out.writeInt(2);
            out.writeByte(10); out.writeUTF("PublicBukkitValues");
            out.writeByte(8); out.writeUTF("blocklocker:header"); out.writeUTF("PRIVATE");
            out.writeByte(10); out.writeUTF("blocklocker:profile_1");
            out.writeByte(8); out.writeUTF("blocklocker:n"); out.writeUTF("Alice");
            out.writeByte(12); out.writeUTF("blocklocker:u");
            out.writeInt(2);
            out.writeLong(OWNER.getMostSignificantBits());
            out.writeLong(OWNER.getLeastSignificantBits());
            out.writeByte(0); // profile compound
            out.writeByte(0); // PublicBukkitValues compound
            out.writeByte(0); // block entity compound
            out.writeByte(0); // root compound
        }
        var parsed = NbtReader.read(
                new java.io.ByteArrayInputStream(bytes.toByteArray()), 8192);
        var hints = BlockLockerSavedSignCandidates.extract(parsed);
        assertEquals(OWNER, hints.get("1,64,2").firstProfileUuid().orElseThrow());
    }

    @Test void signCandidateMustBelongToItsDeclaringTerrainChunk() throws Exception {
        Map<String,Object> outside = Map.of(
                "xPos", 0, "zPos", 0,
                "block_entities", List.of(sign("minecraft:sign", 16, privatePdc())));
        assertThrows(IOException.class, () ->
                BlockLockerSavedSignCandidates.extract(outside));
        Map<String,Object> inside = Map.of(
                "xPos", 0, "zPos", 0,
                "block_entities", List.of(sign("minecraft:sign", 15, privatePdc())));
        assertTrue(BlockLockerSavedSignCandidates.extract(inside).containsKey("15,64,2"));
        Map<String,Object> incompleteChunk = Map.of(
                "xPos", 0,
                "block_entities", List.of(sign("minecraft:sign", 1, privatePdc())));
        assertThrows(IOException.class, () ->
                BlockLockerSavedSignCandidates.extract(incompleteChunk));
    }
    @Test void duplicatePhysicalSignPositionFailsEvenIfOneHasNoSavedPdc() {
        Map<String,Object> protectedSign = sign("minecraft:sign", 1, privatePdc());
        Map<String,Object> textOnly = Map.of(
                "id", "minecraft:sign", "x", 1, "y", 64, "z", 2);
        assertThrows(IOException.class, () -> BlockLockerSavedSignCandidates.extract(
                Map.of("block_entities", List.of(protectedSign, textOnly))));
        assertThrows(IOException.class, () -> BlockLockerSavedSignCandidates.extract(
                Map.of("block_entities", List.of(textOnly, protectedSign))));
    }

    @Test void duplicateTextOnlySignsFailWithoutInventingOwnership() throws Exception {
        Map<String,Object> first = Map.of(
                "id", "minecraft:sign", "x", 1, "y", 64, "z", 2);
        Map<String,Object> duplicate = Map.of(
                "id", "minecraft:hanging_sign", "x", 1, "y", 64, "z", 2);
        assertThrows(IOException.class, () -> BlockLockerSavedSignCandidates.extract(
                Map.of("block_entities", List.of(first, duplicate))));
        Map<String,Object> distinct = Map.of(
                "id", "minecraft:sign", "x", 2, "y", 64, "z", 2);
        assertTrue(BlockLockerSavedSignCandidates.extract(
                Map.of("block_entities", List.of(first, distinct))).isEmpty());
    }

    @Test void textOnlyLegacySignIsVisibleWithoutInventingSavedPdcOwnership() throws Exception {
        Map<String,Object> legacy = Map.of(
                "id", "minecraft:sign", "x", 1, "y", 64, "z", 2,
                "Text1", "[Private]", "Text2", "Alice",
                "Text3", "[Everyone]", "Text4", "");
        Map<String,Object> chunk = Map.of("xPos", 0, "zPos", 0,
                "block_entities", List.of(legacy));
        assertTrue(BlockLockerSavedSignCandidates.extract(chunk).isEmpty());
        var text = BlockLockerSavedSignCandidates.extractTextCandidates(chunk);
        assertEquals(1, text.size());
        assertEquals("[Private]", text.get("1,64,2").front().get(0));
        assertTrue(text.get("1,64,2").legacy());
    }

    @Test void modernTextAndPdcHintsRemainSeparateAtSamePhysicalPosition() throws Exception {
        List<String> lines = List.of("{\\\"text\\\":\\\"[Private]\\\"}",
                "{\\\"text\\\":\\\"Alice\\\"}", "{\\\"text\\\":\\\"\\\"}",
                "{\\\"text\\\":\\\"\\\"}");
        Map<String,Object> modern = Map.of(
                "id", "minecraft:sign", "x", 1, "y", 64, "z", 2,
                "front_text", Map.of("messages", lines),
                "PublicBukkitValues", privatePdc());
        Map<String,Object> chunk = Map.of("block_entities", List.of(modern));
        assertEquals(OWNER, BlockLockerSavedSignCandidates.extract(chunk)
                .get("1,64,2").firstProfileUuid().orElseThrow());
        assertEquals(lines, BlockLockerSavedSignCandidates.extractTextCandidates(chunk)
                .get("1,64,2").front());
    }

    @Test void textCandidateReaderRejectsDuplicateAndOutOfChunkSigns() {
        Map<String,Object> sign = Map.of(
                "id", "minecraft:sign", "x", 16, "y", 64, "z", 2,
                "Text1", "[Private]", "Text2", "Alice",
                "Text3", "", "Text4", "");
        assertThrows(IOException.class, () -> BlockLockerSavedSignCandidates
                .extractTextCandidates(Map.of("xPos", 0, "zPos", 0,
                        "block_entities", List.of(sign))));
        assertThrows(IOException.class, () -> BlockLockerSavedSignCandidates
                .extractTextCandidates(Map.of("block_entities", List.of(sign, sign))));
    }

}
