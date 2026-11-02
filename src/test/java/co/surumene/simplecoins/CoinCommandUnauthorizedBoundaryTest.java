package co.surumene.simplecoins;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

/**
 * Code-to-spec characterization of the existing unauthorized command boundary.
 * This does not exercise Paper permissions, grants, or successful issuance.
 */
class CoinCommandUnauthorizedBoundaryTest {
    @Test
    void unauthorizedCommandRejectsBeforeParsingOrResolvingAnyTarget() {
        List<String> messages = new ArrayList<>();
        AtomicInteger permissionChecks = new AtomicInteger();
        CommandSender sender = deniedSender(messages, permissionChecks);
        CoinCommand command = new CoinCommand(null, null, null);
        String[][] requests = {
            {},
            {"give"},
            {"give", "offline-player", "100"},
            {"GIVE", "@a", "50", "3"},
            {"other", "player", "100"},
            {"give", "private-player-uuid", "100", "1", "extra"}
        };

        for (String[] args : requests) {
            assertTrue(command.onCommand(sender, null, "sc", args));
        }
        assertEquals(requests.length, permissionChecks.get());
        assertEquals(java.util.Collections.nCopies(requests.length, "No permission"), messages);
    }

    @Test
    void unauthorizedTabCompletionNeverEnumeratesPlayersOrDenominations() {
        List<String> messages = new ArrayList<>();
        AtomicInteger permissionChecks = new AtomicInteger();
        CommandSender sender = deniedSender(messages, permissionChecks);
        CoinCommand command = new CoinCommand(null, null, null);
        String[][] requests = {
            {""},
            {"g"},
            {"give", ""},
            {"give", "@a"},
            {"give", "offline-player", ""},
            {"give", "offline-player", "50", ""}
        };

        for (String[] args : requests) {
            assertEquals(List.of(), command.onTabComplete(sender, null, "sc", args));
        }
        assertEquals(requests.length, permissionChecks.get());
        assertEquals(List.of(), messages);
    }

    private static CommandSender deniedSender(List<String> messages,
                                               AtomicInteger permissionChecks) {
        return (CommandSender) Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(),
                new Class<?>[] {CommandSender.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "hasPermission" -> {
                        assertEquals("simplecoins.admin", args[0]);
                        permissionChecks.incrementAndGet();
                        yield false;
                    }
                    case "sendMessage" -> {
                        messages.add((String) args[0]);
                        yield null;
                    }
                    case "toString" -> "DeniedCommandSender";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new AssertionError(
                            "Unexpected call before permission boundary: " + method.getName());
                });
    }
}
