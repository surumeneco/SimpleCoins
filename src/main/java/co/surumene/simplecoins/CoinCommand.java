package co.surumene.simplecoins;

import co.surumene.simplecoins.domain.CoinDenomination;
import co.surumene.simplecoins.domain.CoinMoney;
import co.surumene.simplecoins.domain.IssuanceBudget;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class CoinCommand implements CommandExecutor, TabCompleter {
    private final SimpleCoinsPlugin plugin;
    private final CoinItems items;
    public CoinCommand(SimpleCoinsPlugin plugin, CoinItems items) {
        this.plugin = plugin; this.items = items;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("simplecoins.admin")) { sender.sendMessage("No permission"); return true; }
        if (args.length < 3 || !args[0].equalsIgnoreCase("give")) {
            sender.sendMessage("Usage: /" + label + " give <online player|selector> <amount|denomination> [count]");
            return true;
        }
        List<Player> targets = resolve(sender, args[1]);
        if (targets.isEmpty()) { sender.sendMessage("No online target matched; offline grants are not yet supported."); return true; }
        try {
            if (args.length == 3) {
                long amount = Long.parseLong(args[2]);
                Map<CoinDenomination, Long> breakdown = CoinMoney.minimumCoins(amount);
                long numberOfStacks = 0;
                for (long n : breakdown.values()) numberOfStacks = Math.addExact(numberOfStacks, IssuanceBudget.coinStacks(n));
                IssuanceBudget.requireBudget(numberOfStacks, targets.size());
                for (Player target : targets) for (var entry : breakdown.entrySet()) giveCoins(target, entry.getKey(), entry.getValue());
                sender.sendMessage("Issued " + amount + items.currency() + " to " + targets.size() + " online player(s).");
            } else if (args.length == 4) {
                long count = Long.parseLong(args[3]);
                if (count <= 0 || count > 65536) throw new IllegalArgumentException("Count must be 1..65536 per command");
                if (args[2].equalsIgnoreCase("wallet")) {
                    IssuanceBudget.requireBudget(count, targets.size());
                    for (Player target : targets) for (long i=0; i<count; i++) deliver(target, items.newWallet());
                } else {
                    CoinDenomination denomination = parseDenomination(args[2]);
                    IssuanceBudget.requireBudget(IssuanceBudget.coinStacks(count), targets.size());
                    for (Player target : targets) giveCoins(target, denomination, count);
                }
                sender.sendMessage("Items issued to " + targets.size() + " online player(s).");
            } else {
                sender.sendMessage("Unexpected arguments");
            }
        } catch (IllegalArgumentException | ArithmeticException failure) {
            sender.sendMessage("Invalid issue request: " + failure.getMessage());
        }
        return true;
    }

    private CoinDenomination parseDenomination(String input) {
        try { return CoinDenomination.fromValue(Integer.parseInt(input)); }
        catch (NumberFormatException ignored) {
            for (CoinDenomination d : CoinDenomination.values())
                if (items.name(d).equalsIgnoreCase(input)) return d;
            throw new IllegalArgumentException("Unknown denomination " + input);
        }
    }

    private List<Player> resolve(CommandSender sender, String input) {
        if (input.startsWith("@")) {
            try {
                return Bukkit.selectEntities(sender, input).stream().filter(e -> e instanceof Player)
                        .map(e -> (Player) e).distinct().toList();
            } catch (IllegalArgumentException failure) { return List.of(); }
        }
        Player player = Bukkit.getPlayerExact(input);
        return player == null ? List.of() : List.of(player);
    }

    private void giveCoins(Player target, CoinDenomination denomination, long count) {
        while (count > 0) {
            int batch = (int) Math.min(64, count);
            deliver(target, items.coin(denomination, batch));
            count -= batch;
        }
    }

    private void deliver(Player player, ItemStack item) {
        for (ItemStack leftover : player.getInventory().addItem(item).values())
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission("simplecoins.admin")) return List.of();
        if (args.length == 1) return "give".startsWith(args[0].toLowerCase(Locale.ROOT)) ? List.of("give") : List.of();
        if (args.length == 2) {
            List<String> options = new ArrayList<>(List.of("@p", "@a", "@s", "@r"));
            for (Player p : Bukkit.getOnlinePlayers()) options.add(p.getName());
            return options.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 3) {
            List<String> options = new ArrayList<>();
            options.add("wallet");
            for (CoinDenomination d : CoinDenomination.values()) options.add(String.valueOf(d.value()));
            return options.stream().filter(s -> s.startsWith(args[2])).toList();
        }
        return List.of();
    }
}
