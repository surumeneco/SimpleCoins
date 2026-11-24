package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.*;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/** Actual administrative give, known UUID reservation and join handoff. */
public final class PaperAdminCommand implements TabExecutor, Listener {
    private final JavaPlugin plugin;
    private final SettlementRuntime runtime;
    private final GrantLedger ledger;
    private final PaperPlayerGrantDelivery delivery;
    private final WalletItemCodec wallets;
    private final AdminAssetCommand queries;
    private final Map<CoinDenomination,String> names;
    private final String unit;

    public PaperAdminCommand(JavaPlugin plugin, SettlementRuntime runtime, GrantLedger ledger,
            PaperPlayerGrantDelivery delivery, WalletItemCodec wallets,
            WorldScope scope, Map<CoinDenomination,String> names, String unit) {
        this.plugin=Objects.requireNonNull(plugin); this.runtime=Objects.requireNonNull(runtime);
        this.ledger=Objects.requireNonNull(ledger); this.delivery=Objects.requireNonNull(delivery);
        this.wallets=Objects.requireNonNull(wallets); this.names=Map.copyOf(names); this.unit=unit;
        this.queries=new AdminAssetCommand(runtime,scope,unit);
    }

    public void setQueryFreshness(java.util.function.BooleanSupplier fresh){queries.setFreshness(fresh);}

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length==0 || !args[0].equalsIgnoreCase("give"))
            return queries.onCommand(sender,command,label,args);
        var admission=AdminGiveRequest.evaluate(sender.hasPermission("simplecoins.give"),
                runtime.memoryAccountingReady(),true,true);
        if (admission==AdminGiveRequest.Outcome.DENIED) {
            sender.sendMessage("You do not have permission."); return true;
        }
        if (admission==AdminGiveRequest.Outcome.UNAVAILABLE) {
            sender.sendMessage("Currency issuance unavailable until settlement is ready."); return true;
        }
        if (args.length<3) return false;
        final List<OfflinePlayer> targets;
        final GrantDistributionPlan plan;
        final CoinDenomination exact;
        final long count;
        final boolean wallet;
        try {
            targets=targets(sender,args[1]);
            boolean amountOnly=args.length==3;
            count=Long.parseLong(args[args.length-1]);
            if (count<=0) throw new IllegalArgumentException("Amount or count must be positive");
            String item=amountOnly?"":String.join(" ",Arrays.copyOfRange(args,2,args.length-1));
            wallet=!amountOnly && item.equalsIgnoreCase("wallet");
            exact=amountOnly||wallet?null:denomination(item);
            if (!amountOnly && targets.stream().anyMatch(t->!t.isOnline()))
                throw new IllegalArgumentException("Item and count form requires online targets");
            for (OfflinePlayer target:targets) {
                if (delivery.isBlocked(target.getUniqueId()))
                    throw new IllegalArgumentException("Delivery held for "+target.getName());
                if (target.isOnline() && !eligible(target.getPlayer()))
                    throw new IllegalArgumentException("Target is not eligible for live issuance");
            }
            plan=amountOnly?GrantDistributionPlan.forAmount(count,targets.size()):
                    wallet?GrantDistributionPlan.forEmptyWallets(count,targets.size()):
                    GrantDistributionPlan.forExactCoins(exact,count,targets.size());
            if (wallet) for (OfflinePlayer target:targets) {
                long free=Arrays.stream(target.getPlayer().getInventory().getStorageContents())
                        .filter(itemStack->itemStack==null||itemStack.getType().isAir()).count();
                if (free<plan.emptyWalletCountPerRecipient())
                    throw new IllegalArgumentException("Not enough empty wallet slots for "+target.getName());
            }
            if (AdminGiveRequest.evaluate(true,true,!targets.isEmpty(),true)
                    !=AdminGiveRequest.Outcome.ACCEPT) throw new IllegalArgumentException("Invalid request");
        } catch (IllegalArgumentException failure) {
            sender.sendMessage("Invalid give: "+failure.getMessage()); return true;
        }
        String issuer=sender instanceof Player p?p.getUniqueId()+" "+p.getName():sender.getName();
        for (OfflinePlayer target:targets) {
            try {
                if (!runtime.memoryAccountingReady()) throw new IllegalStateException("Accounting unavailable");
                UUID id=target.getUniqueId();
                if (wallet) {
                    giveWallets(Objects.requireNonNull(target.getPlayer()),plan.emptyWalletCountPerRecipient());
                    sender.sendMessage("Gave "+count+" empty wallet(s) to "+target.getName());
                } else {
                    GrantLedger.Grant grant=exact==null?ledger.reserve(id,count,issuer):
                            ledger.reserveCoins(id,exact,count,issuer);
                    sender.sendMessage("Reserved "+grant.amount()+unit+" for "+target.getName()
                            +" (grant "+grant.id()+").");
                    if (target.isOnline()) {
                        var result=delivery.deliver(Objects.requireNonNull(target.getPlayer()));
                        sender.sendMessage("Delivered now (including earlier reservations): "
                                +result.deliveredNow()+unit+"; pending reservations: "+result.pendingCount());
                    }
                }
            } catch (IOException | RuntimeException failure) {
                plugin.getLogger().log(Level.SEVERE,"SimpleCoins give failed for "+target.getUniqueId(),failure);
                sender.sendMessage("Stopped issuance at "+target.getName()+": "+failure.getMessage()
                        +". Inspect existing reservations before retrying.");
                break;
            }
        }
        return true;
    }

    private void giveWallets(Player player,long count) throws IOException {
        // Empty wallets are physical zero-value assets, not monetary reservations.
        // Verify that this actual player has persisted data before registering 0.
        if (PaperPlayerGrantDelivery.playerdata(player.getUniqueId())==null) player.saveData();
        if (PaperPlayerGrantDelivery.playerdata(player.getUniqueId())==null)
            throw new IOException("Paper playerdata unavailable");
        ItemStack[] next=player.getInventory().getStorageContents();
        long remaining=count;
        for (int i=0;i<next.length&&remaining>0;i++) if(next[i]==null||next[i].getType().isAir()) {
            next[i]=wallets.create(); remaining--;
        }
        if(remaining!=0)throw new IllegalStateException("Wallet capacity changed");
        player.getInventory().setStorageContents(next);
        runtime.recordConfirmedMemory(List.of(),Set.of(player.getUniqueId()));
    }

    private CoinDenomination denomination(String item) {
        CoinDenomination selected=null;
        for(var coin:CoinDenomination.values()) {
            if (!item.equalsIgnoreCase("coin_"+coin.value()) && !item.equalsIgnoreCase(names.get(coin))) continue;
            if(selected!=null&&selected!=coin)throw new IllegalArgumentException("Ambiguous coin name");
            selected=coin;
        }
        if(selected==null)throw new IllegalArgumentException("Unknown item: "+item);
        return selected;
    }

    private static boolean eligible(Player player) {
        if(player==null||player.isBanned())return false;
        var address=player.getAddress();
        return address==null||address.getAddress()==null||!Bukkit.getIPBans().contains(address.getAddress().getHostAddress());
    }

    private static List<OfflinePlayer> targets(CommandSender sender,String input) {
        Map<UUID,OfflinePlayer> matches=new LinkedHashMap<>();
        if(input.startsWith("@")) {
            for(var entity:Bukkit.selectEntities(sender,input)) {
                if(!(entity instanceof Player player)||!player.isOnline())
                    throw new IllegalArgumentException("Selector must resolve to online players");
                matches.put(player.getUniqueId(),player);
            }
        } else {
            UUID requested=null;
            try { requested=UUID.fromString(input); if(!requested.toString().equalsIgnoreCase(input))requested=null; }
            catch(IllegalArgumentException ignored) { }
            for(Player player:Bukkit.getOnlinePlayers())
                if(input.equalsIgnoreCase(player.getName())||player.getUniqueId().equals(requested))
                    matches.put(player.getUniqueId(),player);
            for(OfflinePlayer player:Bukkit.getOfflinePlayers())
                if((input.equalsIgnoreCase(player.getName())||player.getUniqueId().equals(requested))
                        &&(player.isOnline()||player.hasPlayedBefore()))
                    matches.putIfAbsent(player.getUniqueId(),player);
            if(matches.size()!=1)throw new IllegalArgumentException("Unknown or ambiguous player");
        }
        if(matches.isEmpty())throw new IllegalArgumentException("No selected players");
        return List.copyOf(matches.values());
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player=event.getPlayer();
        Bukkit.getScheduler().runTask(plugin,()->{
            if(!player.isOnline()||!eligible(player)||!runtime.memoryAccountingReady())return;
            try {
                var result=delivery.deliver(player);
                if(result.deliveredNow()>0)player.sendMessage("Received "+result.deliveredNow()+unit+" from reserved grants.");
                if(result.pendingCount()>0)player.sendMessage("Remaining reservations: "+result.pendingCount()+". Free space and rejoin.");
            } catch(IOException|RuntimeException failure) {
                plugin.getLogger().log(Level.SEVERE,"Grant delivery held for "+player.getUniqueId(),failure);
                player.sendMessage("Automatic currency delivery is held; contact an administrator.");
            }
        });
    }

    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args) {
        List<String> candidates=new ArrayList<>();
        if(args.length==1) {
            if(sender.hasPermission("simplecoins.admin")) candidates.addAll(List.of("stats","balance","dead"));
            if(sender.hasPermission("simplecoins.give"))candidates.add("give");
        } else if(args[0].equalsIgnoreCase("give")&&sender.hasPermission("simplecoins.give")) {
            if(args.length==2) {
                candidates.addAll(List.of("@p","@a","@s","@r"));
                for(Player player:Bukkit.getOnlinePlayers())candidates.add(player.getName());
                for(OfflinePlayer player:Bukkit.getOfflinePlayers())if(player.getName()!=null)candidates.add(player.getName());
            } else if(args.length==3) {
                candidates.add("wallet");
                for(var coin:CoinDenomination.values())candidates.add("coin_"+coin.value());
            }
        }
        String prefix=args.length==0?"":args[args.length-1].toLowerCase(Locale.ROOT);
        return candidates.stream().distinct().filter(s->s.toLowerCase(Locale.ROOT).startsWith(prefix)).sorted().toList();
    }
}
