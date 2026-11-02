package co.surumene.simplecoins;

import static org.junit.jupiter.api.Assertions.*;

import co.surumene.simplecoins.domain.BlockLockerProtectionResolution;
import co.surumene.simplecoins.domain.BlockLockerSignAttachment;
import co.surumene.simplecoins.domain.BlockLockerVisibleSignResolver;
import co.surumene.simplecoins.domain.ExternalOwnerDecision;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.text.Component;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallSign;
import org.junit.jupiter.api.Test;

/** Source-bound code-to-spec Candidate LIVE-SIGN-01..06; not live Paper acceptance. */
class PaperBlockLockerSignAdapterBoundaryTest {
    private static final UUID OWNER =
            UUID.fromString("00000000-0000-0000-0000-000000000077");
    private static final Set<String> PRIVATE = Set.of("[Private]");
    private static final Set<String> MORE = Set.of("[More Users]");
    private static final Set<String> EVERYONE = Set.of("Everyone");
    private static final BlockLockerSignAttachment.Position CHEST =
            new BlockLockerSignAttachment.Position(0, 64, 0);

    @Test
    void wallAndStandingSignGeometryRetainVerifiedOwner() throws Exception {
        WallSign wallData = proxy(WallSign.class, (method, args) -> {
            if (method.equals("getFacing")) return BlockFace.NORTH;
            throw new AssertionError("Unexpected wall-state call: " + method);
        });
        org.bukkit.block.data.type.Sign standingData =
                proxy(org.bukkit.block.data.type.Sign.class, (method, args) -> {
                    throw new AssertionError("Unexpected standing-state call: " + method);
                });
        var wall = read(sign(wallData, 0, 64, -1, text("[Private]", "Alice", "", ""), false));
        var standing = read(sign(standingData, 0, 65, 0,
                text("[Private]", "Alice", "", ""), false));
        assertEquals(OWNER, BlockLockerProtectionResolution.single(CHEST, List.of(wall)).owner());
        assertEquals(OWNER, BlockLockerProtectionResolution.single(CHEST, List.of(standing)).owner());
        assertEquals(ExternalOwnerDecision.Kind.PERSONAL, wall.primary().kind());
        assertEquals(ExternalOwnerDecision.Kind.PERSONAL, standing.primary().kind());
    }

    @Test
    void malformedFrontOrUnsupportedStateFailsClosed() {
        WallSign wallData = proxy(WallSign.class, (method, args) -> {
            if (method.equals("getFacing")) return BlockFace.NORTH;
            throw new AssertionError(method);
        });
        BlockData unsupported = proxy(BlockData.class, (method, args) -> {
            throw new AssertionError(method);
        });
        assertThrows(IOException.class, () -> read(sign(wallData, 0, 64, -1,
                text("[Private]", "Alice", ""), false)));
        assertThrows(IOException.class, () -> read(sign(unsupported, 0, 64, -1,
                text("[Private]", "Alice", "", ""), false)));
        assertThrows(IOException.class, () -> read(sign(wallData, 0, 64, -1,
                text("[Private]", "Alice", "", ""), true)));
    }

    @Test
    void unrelatedSignDoesNotResolvePrimaryIdentity() throws Exception {
        WallSign wallData = proxy(WallSign.class, (method, args) -> {
            if (method.equals("getFacing")) return BlockFace.NORTH;
            throw new AssertionError(method);
        });
        AtomicInteger calls = new AtomicInteger();
        var result = PaperBlockLockerSignAdapter.readPaper(
                sign(wallData, 0, 64, -1, text("Hello", "Alice", "", ""), false),
                PRIVATE, MORE, EVERYONE, name -> {
                    calls.incrementAndGet();
                    throw new AssertionError("Non-BlockLocker sign must not query identity");
                });
        assertTrue(result.isEmpty());
        assertEquals(0, calls.get());
    }

    @Test
    void liveEveryoneNeedsResolvedPrimaryBeforePublicOwnership() throws Exception {
        WallSign wallData = proxy(WallSign.class, (method, args) -> {
            if (method.equals("getFacing")) return BlockFace.NORTH;
            throw new AssertionError(method);
        });
        Sign publicSign = sign(wallData, 0, 64, -1,
                text("[Private]", "Alice", "[Everyone]", ""), false);
        var known = PaperBlockLockerSignAdapter.readPaper(publicSign,
                PRIVATE, MORE, EVERYONE, name -> ExternalOwnerDecision.personal(OWNER))
                .orElseThrow();
        var unknown = PaperBlockLockerSignAdapter.readPaper(publicSign,
                PRIVATE, MORE, EVERYONE, name -> ExternalOwnerDecision.unknown())
                .orElseThrow();
        assertEquals(ExternalOwnerDecision.Kind.FREE,
                BlockLockerProtectionResolution.single(CHEST, List.of(known)).kind());
        assertEquals(ExternalOwnerDecision.Kind.UNKNOWN,
                BlockLockerProtectionResolution.single(CHEST, List.of(unknown)).kind());
    }

    private static BlockLockerProtectionResolution.VerifiedSign read(Sign sign)
            throws IOException {
        return PaperBlockLockerSignAdapter.readPaper(sign, PRIVATE, MORE, EVERYONE,
                name -> {
                    assertEquals("Alice", name);
                    return ExternalOwnerDecision.personal(OWNER);
                }).orElseThrow();
    }

    private static List<Component> text(String... lines) {
        return java.util.Arrays.stream(lines).map(Component::text).map(c -> (Component) c).toList();
    }

    private static Sign sign(BlockData state, int x, int y, int z,
                             List<Component> front, boolean throwOnRead) {
        SignSide side = proxy(SignSide.class, (method, args) -> {
            if (method.equals("lines")) return front;
            throw new AssertionError("Unexpected sign-side call: " + method);
        });
        return proxy(Sign.class, (method, args) -> switch (method) {
            case "getBlockData" -> state;
            case "getX" -> x;
            case "getY" -> y;
            case "getZ" -> z;
            case "getSide" -> {
                assertEquals(Side.FRONT, args[0]);
                if (throwOnRead) throw new IllegalStateException("Sign snapshot unavailable");
                yield side;
            }
            default -> throw new AssertionError("Unexpected Paper sign call: " + method);
        });
    }

    @FunctionalInterface
    private interface Call {
        Object apply(String method, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Call call) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(),
                new Class<?>[] {type}, (self, method, args) ->
                        call.apply(method.getName(), args));
    }
}
