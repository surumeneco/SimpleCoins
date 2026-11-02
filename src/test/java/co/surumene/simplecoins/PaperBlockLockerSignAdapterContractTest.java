package co.surumene.simplecoins;

import static org.junit.jupiter.api.Assertions.*;

import co.surumene.simplecoins.domain.BlockLockerProtectionResolution;
import co.surumene.simplecoins.domain.BlockLockerSignAttachment;
import co.surumene.simplecoins.domain.ExternalOwnerDecision;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.text.Component;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallSign;
import org.junit.jupiter.api.Test;

/**
 * Paper API boundary contract only: proxy objects are not a running Paper
 * server or BlockLocker plugin. Candidate BL-LIVE-01..05.
 */
class PaperBlockLockerSignAdapterContractTest {
    private static final UUID OWNER =
            UUID.fromString("00000000-0000-0000-0000-000000000007");
    private static final Set<String> PRIVATE = Set.of("[Private]");
    private static final Set<String> MORE_USERS = Set.of("[More Users]");
    private static final Set<String> EVERYONE = Set.of("Everyone");
    private static final BlockLockerSignAttachment.Position CHEST =
            new BlockLockerSignAttachment.Position(0, 64, 0);

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),
                new Class<?>[]{type}, handler));
    }

    private static BlockData wall(BlockFace facing) {
        return proxy(WallSign.class, (self, method, args) -> {
            if (method.getName().equals("getFacing")) return facing;
            throw new UnsupportedOperationException(method.getName());
        });
    }

    private static Sign sign(BlockData blockData, List<Component> front) {
        SignSide side = proxy(SignSide.class, (self, method, args) -> {
            if (method.getName().equals("lines")) return front;
            throw new UnsupportedOperationException(method.getName());
        });
        return proxy(Sign.class, (self, method, args) -> switch (method.getName()) {
            case "getBlockData" -> blockData;
            case "getX" -> 0;
            case "getY" -> 64;
            case "getZ" -> -1;
            case "getSide" -> {
                assertEquals(Side.FRONT, args[0], "Only the front text is authoritative");
                yield side;
            }
            default -> throw new UnsupportedOperationException(method.getName());
        });
    }

    private static List<Component> lines(String a, String b, String c, String d) {
        return List.of(Component.text(a), Component.text(b),
                Component.text(c), Component.text(d));
    }

    private static java.util.Optional<BlockLockerProtectionResolution.VerifiedSign> read(
            Sign sign, co.surumene.simplecoins.domain.BlockLockerVisibleSignResolver.PrimaryIdentity identity)
            throws IOException {
        return PaperBlockLockerSignAdapter.readPaper(sign, PRIVATE, MORE_USERS,
                EVERYONE, identity);
    }

    @Test void northWallSignPreservesPhysicalEvidenceAndVerifiedPrimary() throws Exception {
        var verified = read(sign(wall(BlockFace.NORTH),
                lines("[Private]", "Alice", "", "")),
                name -> {
                    assertEquals("Alice", name);
                    return ExternalOwnerDecision.personal(OWNER);
                }).orElseThrow();
        assertEquals(new BlockLockerSignAttachment.Position(0, 64, -1),
                verified.position());
        assertEquals(BlockLockerSignAttachment.Mount.WALL_NORTH, verified.mount());
        assertEquals(OWNER, BlockLockerProtectionResolution.single(
                CHEST, List.of(verified)).owner());
    }

    @Test void everyoneRequiresVerifiedPrimaryAndPhysicalAttachment() throws Exception {
        var verified = read(sign(wall(BlockFace.NORTH),
                lines("[Private]", "Alice", "[Everyone]", "")),
                name -> ExternalOwnerDecision.personal(OWNER)).orElseThrow();
        assertTrue(verified.everyone());
        assertEquals(ExternalOwnerDecision.Kind.FREE,
                BlockLockerProtectionResolution.single(CHEST, List.of(verified)).kind());
        assertEquals(ExternalOwnerDecision.Kind.UNKNOWN,
                BlockLockerProtectionResolution.single(
                        new BlockLockerSignAttachment.Position(10, 64, 10),
                        List.of(verified)).kind());
    }

    @Test void ordinarySignDoesNotInvokeIdentityResolver() throws Exception {
        var calls = new AtomicInteger();
        var result = read(sign(wall(BlockFace.NORTH),
                lines("Welcome", "Alice", "", "")), name -> {
                    calls.incrementAndGet();
                    throw new AssertionError("An ordinary sign is not ownership evidence");
                });
        assertTrue(result.isEmpty());
        assertEquals(0, calls.get());
    }

    @Test void absentUnsupportedAndIncompletePaperEvidenceFailsClosed() {
        var good = lines("[Private]", "Alice", "", "");
        var identity = (co.surumene.simplecoins.domain.BlockLockerVisibleSignResolver.PrimaryIdentity)
                name -> ExternalOwnerDecision.personal(OWNER);
        assertThrows(IOException.class, () -> read(null, identity));
        assertThrows(IOException.class, () -> read(sign(
                proxy(BlockData.class, (self, method, args) -> null), good), identity));
        assertThrows(IOException.class, () -> read(sign(wall(BlockFace.NORTH),
                good.subList(0, 3)), identity));
        assertThrows(IOException.class, () -> read(sign(wall(BlockFace.NORTH),
                null), identity));
    }

    @Test void failedPrimaryLookupCannotPublishPersonalOrFree() {
        assertThrows(IOException.class, () -> read(sign(wall(BlockFace.NORTH),
                lines("[Private]", "Alice", "[Everyone]", "")),
                name -> { throw new IOException("Identity source unavailable"); }));
    }
}
