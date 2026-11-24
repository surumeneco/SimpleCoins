package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.AdminQuery;
import co.surumene.simplecoins.domain.SettlementRuntime;
import co.surumene.simplecoins.domain.SettlementAccumulator;
import co.surumene.simplecoins.domain.WorldScope;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/** Reads only the published generation, never the pending accumulator. */
public final class AdminAssetCommand implements CommandExecutor {
    private static final int PAGE_SIZE = 10;
    private final SettlementRuntime runtime;
    private final WorldScope scope;
    private final String unit;
    public AdminAssetCommand(SettlementRuntime runtime, WorldScope scope, String unit) {
        this.runtime = Objects.requireNonNull(runtime);
        this.scope = Objects.requireNonNull(scope);
        this.unit = Objects.requireNonNull(unit);
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("simplecoins.admin")) {
            sender.sendMessage("You do not have permission.");
            return true;
        }
        if (args.length == 0) return false;
        AdminQuery.Kind kind;
        int page = 1;
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "stats" -> { if (args.length != 1) return false; kind = AdminQuery.Kind.STATISTICS; }
            case "balance" -> { if (args.length != 2) return false; kind = AdminQuery.Kind.PLAYER; }
            case "dead" -> {
                if (args.length > 2) return false;
                kind = AdminQuery.Kind.DEAD;
                if (args.length == 2) {
                    try { page = Integer.parseInt(args[1]); }
                    catch (NumberFormatException invalid) { sender.sendMessage("Page must be a positive integer."); return true; }
                    if (page < 1) { sender.sendMessage("Page must be a positive integer."); return true; }
                }
            }
            default -> { return false; }
        }
        var published = runtime.current();
        UUID player = kind == AdminQuery.Kind.PLAYER && published.isPresent() ? resolve(args[1]) : null;
        boolean registered = player != null && published.isPresent() && published.get().personalBalances().containsKey(player);
        var outcome = AdminQuery.evaluate(true, published.isPresent(), kind, registered);
        switch (outcome) {
            case UNAVAILABLE -> sender.sendMessage("Accounting is not available: no verified current settlement.");
            case UNREGISTERED -> sender.sendMessage("No registered personal balance for this player.");
            case DENIED -> sender.sendMessage("You do not have permission.");
            case DISPLAY -> {
                var snapshot = published.orElseThrow();
                sender.sendMessage("Accounting as of " + Instant.now() + " (UTC); world default=" + scope.defaultIncluded()
                        + "; enabled=" + scope.included() + "; disabled=" + scope.excluded()
                        + "; player/ender storage always included.");
                switch (kind) {
                    case STATISTICS -> stats(sender, snapshot);
                    case PLAYER -> sender.sendMessage("Personal balance " + player + ": " + money(snapshot.personalBalances().get(player)));
                    case DEAD -> dead(sender, snapshot, page);
                }
            }
        }
        return true;
    }
    private UUID resolve(String input) {
        try {
            UUID uuid = UUID.fromString(input);
            if (uuid.toString().equalsIgnoreCase(input)) return uuid;
        } catch (IllegalArgumentException ignored) { }
        UUID found = null;
        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            if (!input.equalsIgnoreCase(player.getName())) continue;
            if (found != null && !found.equals(player.getUniqueId())) return null;
            found = player.getUniqueId();
        }
        return found;
    }
    private void stats(CommandSender sender, SettlementAccumulator.Result snapshot) {
        var s = snapshot.statistics();
        sender.sendMessage("Registered players: " + s.registeredPlayers());
        sender.sendMessage("Personal P: " + money(s.personal()) + "; free F: " + money(s.free())
                + "; world T=P+F: " + money(s.worldTotal()));
        sender.sendMessage("Dead D (excluded from T): " + money(s.dead()));
        sender.sendMessage("Shared average: " + money(s.sharedAverage()) + "; personal average: " + money(s.personalAverage()));
        sender.sendMessage("Max: " + money(s.max()) + "; min: " + money(s.min()));
        sender.sendMessage("Max/world: " + percent(s.maxWorldPercent()) + "; half-share subjects: " + value(s.halfShareSubjects())
                + "; half-share/player count: " + percent(s.halfSharePercent()));
        sender.sendMessage("Max/min minus shared average: " + money(s.maxMinusShared()) + " / " + money(s.minMinusShared()));
        sender.sendMessage("Max/min minus personal average: " + money(s.maxMinusPersonal()) + " / " + money(s.minMinusPersonal()));
    }
    private void dead(CommandSender sender, SettlementAccumulator.Result snapshot, int page) {
        var locations = snapshot.deadLocations().stream().sorted(Comparator
                .comparing(SettlementAccumulator.DeadLocation::owner)
                .thenComparing(SettlementAccumulator.DeadLocation::location)
                .thenComparing(SettlementAccumulator.DeadLocation::reason)).toList();
        sender.sendMessage("Dead D: " + money(snapshot.dead()));
        if (locations.isEmpty()) { sender.sendMessage("No dead asset locations."); return; }
        int pages = 1 + (locations.size() - 1) / PAGE_SIZE;
        if (page > pages) { sender.sendMessage("Page out of range (1-" + pages + ")."); return; }
        sender.sendMessage("Dead asset locations (" + page + "/" + pages + "):");
        int start = (page - 1) * PAGE_SIZE;
        int count = Math.min(PAGE_SIZE, locations.size() - start);
        for (int i = start; i < start + count; i++) {
            var row = locations.get(i);
            sender.sendMessage(row.owner() + " | " + row.reason() + " | " + row.location() + " | " + money(row.value()));
        }
    }
    private String money(Number value) { return value == null ? "N/A" : value(value) + " " + unit; }
    private static String percent(BigDecimal value) { return value == null ? "N/A" : value(value) + "%"; }
    private static String value(Number value) {
        return value == null ? "N/A" : value instanceof BigDecimal decimal ? decimal.stripTrailingZeros().toPlainString() : value.toString();
    }
}
