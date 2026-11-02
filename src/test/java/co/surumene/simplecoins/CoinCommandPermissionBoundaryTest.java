package co.surumene.simplecoins;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

/**
 * Code-to-spec verification of the existing admin permission short circuit.
 * This does not prove the positive give path or a live Paper permission setup.
 */
class CoinCommandPermissionBoundaryTest {
    @Test
    void deniedCommandNeverNeedsPlayerResolutionOrGrantDependencies() {
        AtomicInteger rejectedMessages = new AtomicInteger();
        CommandSender denied = deniedSender(rejectedMessages);
        CoinCommand command = new CoinCommand(null, null, null);

        assertTrue(command.onCommand(denied, null, "coin",
                new String[] {"give", "@a", "100"}));
        assertTrue(command.onCommand(denied, null, "coin",
                new String[] {"give", "@a", "wallet", "2"}));
        assertTrue(command.onCommand(denied, null, "coin",
                new String[] {}));
        assertEquals(3, rejectedMessages.get(),
                "Each rejected invocation should report denial before any give work");
    }

    @Test
    void deniedTabCompletionNeverEnumeratesPlayersOrItems() {
        AtomicInteger rejectedMessages = new AtomicInteger();
        CommandSender denied = deniedSender(rejectedMessages);
        CoinCommand command = new CoinCommand(null, null, null);

        for (String[] args : List.of(
                new String[] {""},
                new String[] {"give", ""},
                new String[] {"give", "@a", ""},
                new String[] {"give", "@a", "wallet", ""})) {
            assertEquals(List.of(), command.onTabComplete(denied, null, "coin", args));
        }
        assertEquals(0, rejectedMessages.get(),
                "Tab completion must not emit messages or query external dependencies");
    }

    private static CommandSender deniedSender(AtomicInteger messages) {
        return (CommandSender) Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(),
                new Class<?>[] {CommandSender.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "hasPermission" -> false;
                    case "sendMessage" -> {
                        messages.incrementAndGet();
                        yield null;
                    }
                    case "toString" -> "DeniedCommandSender";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new AssertionError(
                            "Unexpected dependency call before permission check: "
                                    + method.getName());
                });
    }
}
