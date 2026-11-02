package co.surumene.simplecoins;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

/**
 * Code-to-spec characterization of authorized syntax-only branches.
 * This is not evidence that actual give, player lookup or Paper integration works.
 */
class CoinCommandAuthorizedSyntaxBoundaryTest {
    @Test
    void authorizedMalformedCommandsReturnUsageWithoutResolvingPlayersOrGrants() {
        List<String> messages = new ArrayList<>();
        CommandSender sender = authorizedSender(messages);
        CoinCommand command = new CoinCommand(null, null, null);

        assertTrue(command.onCommand(sender, null, "coin", new String[] {}));
        assertTrue(command.onCommand(sender, null, "coin", new String[] {"give"}));
        assertTrue(command.onCommand(sender, null, "coin",
                new String[] {"GIVE", "@a"}));
        assertTrue(command.onCommand(sender, null, "coin",
                new String[] {"other", "@a", "100"}));

        assertEquals(4, messages.size());
        for (String message : messages) {
            assertTrue(message.startsWith("Usage: /coin give "),
                    "Malformed authorized commands must not start issuance: " + message);
        }
    }

    @Test
    void authorizedFirstArgumentCompletionIsCaseInsensitiveAndDependencyFree() {
        List<String> messages = new ArrayList<>();
        CommandSender sender = authorizedSender(messages);
        CoinCommand command = new CoinCommand(null, null, null);

        assertEquals(List.of("give"),
                command.onTabComplete(sender, null, "coin", new String[] {""}));
        assertEquals(List.of("give"),
                command.onTabComplete(sender, null, "coin", new String[] {"g"}));
        assertEquals(List.of("give"),
                command.onTabComplete(sender, null, "coin", new String[] {"GI"}));
        assertEquals(List.of("give"),
                command.onTabComplete(sender, null, "coin", new String[] {"give"}));
        assertEquals(List.of(),
                command.onTabComplete(sender, null, "coin", new String[] {"wallet"}));
        assertEquals(List.of(), messages);
    }

    @Test
    void pluginDescriptorBindsAdminCommandToOpOnlyPermission() throws Exception {
        String descriptor;
        try (var stream = CoinCommand.class.getClassLoader()
                .getResourceAsStream("plugin.yml")) {
            assertNotNull(stream, "Built plugin descriptor must be on the test classpath");
            descriptor = new String(stream.readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8);
        }
        int commandsStart = descriptor.indexOf("\ncommands:\n");
        int permissionsStart = descriptor.indexOf("\npermissions:\n");
        assertTrue(commandsStart >= 0 && permissionsStart > commandsStart,
                "Descriptor must have distinct commands and permissions sections");
        String commands = descriptor.substring(commandsStart, permissionsStart);
        String permissions = descriptor.substring(permissionsStart);
        assertTrue(commands.contains("\n  simplecoins:\n"));
        assertTrue(commands.contains("\n    permission: simplecoins.admin\n"));
        assertTrue(permissions.contains("\n  simplecoins.admin:\n"));
        assertTrue(permissions.contains("\n    default: op\n"),
                "Admin permission must not be granted by default");
    }

    private static CommandSender authorizedSender(List<String> messages) {
        return (CommandSender) Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(),
                new Class<?>[] {CommandSender.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "hasPermission" -> {
                        assertEquals("simplecoins.admin", args[0]);
                        yield true;
                    }
                    case "sendMessage" -> {
                        messages.add((String) args[0]);
                        yield null;
                    }
                    case "toString" -> "AuthorizedCommandSender";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new AssertionError(
                            "Unexpected dependency call in syntax-only branch: "
                                    + method.getName());
                });
    }
}
