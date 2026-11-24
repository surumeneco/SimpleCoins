package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.*;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.HopperInventorySearchEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.block.Sign;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.ItemStack;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Transient observations of actual block-hopper transfers. Paper temporarily
 * changes the source stack count before InventoryMoveItemEvent: the search
 * event supplies the original physical preimage, not the proposed item.
 * Never infer a debit from the proposed count or from event cancellation alone. */
public final class PaperHopperAccounting implements Listener {
    private record Position(UUID world, int x, int y, int z) {
        static Position of(Block b) { return new Position(b.getWorld().getUID(), b.getX(), b.getY(), b.getZ()); }
        Block block() throws IOException {
            World w = Bukkit.getWorld(world);
            if (w == null || !w.isChunkLoaded(x >> 4, z >> 4))
                throw new IOException("Hopper inventory chunk unavailable");
            return w.getBlockAt(x, y, z);
        }
        String location(Block b, int slot) {
            return "world/" + b.getWorld().getName() + "/" + x + "," + y + "," + z + ":" + slot;
        }
    }
    private record Image(Position position, ItemStack[] contents, AssetOwner.Result owner) {}
    private final WorldScope scope;
    private final BooleanSupplier ready;
    private final Consumer<List<MemoryAccounting.Movement>> sink;
    private final Consumer<Exception> fatal;
    private final Consumer<Set<UUID>> register;
    private final Consumer<Inventory> observeEntity;
    private final java.util.function.Predicate<Inventory> entityImage;
    private final Consumer<List<OwnerStorageIndex.Change>> ownerLocations;
    private final Map<Position, Image> before = new HashMap<>();
    private final Set<Position> touched = new HashSet<>();
    private static final int COMPRESSED = 16 * 1024 * 1024;
    private static final int INFLATED = 64 * 1024 * 1024;

    public PaperHopperAccounting(WorldScope scope, BooleanSupplier ready,
            Consumer<List<MemoryAccounting.Movement>> sink, Consumer<Exception> fatal) {
        this(scope,ready,sink,fatal,ignored->{});
    }
    public PaperHopperAccounting(WorldScope scope, BooleanSupplier ready,
            Consumer<List<MemoryAccounting.Movement>> sink, Consumer<Exception> fatal,
            Consumer<Set<UUID>> register) {
        this(scope,ready,sink,fatal,register,ignored->{},ignored->false);
    }
    public PaperHopperAccounting(WorldScope scope, BooleanSupplier ready,
            Consumer<List<MemoryAccounting.Movement>> sink, Consumer<Exception> fatal,
            Consumer<Set<UUID>> register, Consumer<Inventory> observeEntity,
            java.util.function.Predicate<Inventory> entityImage) {
        this(scope,ready,sink,fatal,register,observeEntity,entityImage,ignored->{});
    }
    public PaperHopperAccounting(WorldScope scope, BooleanSupplier ready,
            Consumer<List<MemoryAccounting.Movement>> sink, Consumer<Exception> fatal,
            Consumer<Set<UUID>> register, Consumer<Inventory> observeEntity,
            java.util.function.Predicate<Inventory> entityImage, Consumer<List<OwnerStorageIndex.Change>> ownerLocations) {
        this.ownerLocations=Objects.requireNonNull(ownerLocations);
        this.scope = Objects.requireNonNull(scope);
        this.register = Objects.requireNonNull(register);
        this.observeEntity=Objects.requireNonNull(observeEntity);
        this.entityImage=Objects.requireNonNull(entityImage);
        this.ready = Objects.requireNonNull(ready);
        this.sink = Objects.requireNonNull(sink);
        this.fatal = Objects.requireNonNull(fatal);
    }

    private static Inventory physical(Block b) throws IOException {
        BlockState state = b.getState();
        if (state instanceof Chest chest) return chest.getBlockInventory();
        if (state instanceof Container container) return container.getInventory();
        return null;
    }

    private static List<Position> positions(Inventory inventory) throws IOException {
        if (inventory == null) return List.of();
        if (inventory instanceof DoubleChestInventory doubleChest) {
            var result = new ArrayList<Position>();
            result.addAll(positions(doubleChest.getLeftSide()));
            result.addAll(positions(doubleChest.getRightSide()));
            if (result.size() != 2 || result.get(0).equals(result.get(1)))
                throw new IOException("Invalid double-chest physical halves");
            return result;
        }
        InventoryHolder holder = inventory.getHolder();
        if (holder instanceof DoubleChest doubleChest) {
            var left = doubleChest.getLeftSide();
            var right = doubleChest.getRightSide();
            if (left instanceof BlockState a && right instanceof BlockState b
                    && !Position.of(a.getBlock()).equals(Position.of(b.getBlock())))
                return List.of(Position.of(a.getBlock()), Position.of(b.getBlock()));
            throw new IOException("Missing double-chest physical halves");
        }
        if (holder instanceof BlockState state && state instanceof Container)
            return List.of(Position.of(state.getBlock()));
        return List.of(); // non-block sources require another adapter
    }

    private static ItemStack[] copyContents(Inventory inventory) {
        ItemStack[] items = inventory.getContents();
        for (int i = 0; i < items.length; i++)
            if (items[i] != null) items[i] = items[i].clone();
        return items;
    }

    private AssetOwner.Result resolve(Block block) throws IOException {
        UUID owner=null;
        boolean everyone=false;
        var settings=PaperBlockLockerSettings.capture();
        if (settings.containers().contains(block.getType().getKey().toString())) {
            var protection=BlockLockerLiveEvidence.observe(block);
            owner=protection.owner();
            everyone=protection.everyoneProtected();
        }
        var bans=PaperProfileBanSource.captureCompleteStandard(Set.of("paper-profile","paper-ip"),Map.of());
        return AssetOwner.resolve(owner,everyone,owner!=null&&bans.currentlyBanned(owner),
                owner!=null&&PaperPlayerGrantDelivery.playerdata(owner)!=null);
    }

    private void capture(Position p) throws IOException {
        if (!before.containsKey(p)) {
            var block=p.block();
            var inventory=physical(block);
            if(inventory==null) throw new IOException("Expected physical block inventory");
            before.put(p,new Image(p,copyContents(inventory),resolve(block)));
        }
    }
    private static final BlockFace[] FACES={BlockFace.NORTH,BlockFace.SOUTH,BlockFace.EAST,
            BlockFace.WEST,BlockFace.UP,BlockFace.DOWN};
    private void touch(Block block) throws IOException {
        if (!block.getWorld().isChunkLoaded(block.getX()>>4,block.getZ()>>4)
                || !scope.includes(block.getWorld().getName())) return;
        if (!(block.getState() instanceof Container)) return;
        var position=Position.of(block);
        capture(position);
        touched.add(position);
        if (block.getState() instanceof Chest chest) {
            for(var member:positions(chest.getInventory())) {
                capture(member);
                touched.add(member);
            }
        }
    }
    private void neighborhood(Block block) throws IOException {
        touch(block);
        for(var face:FACES) {
            int x=block.getX()+face.getModX(), y=block.getY()+face.getModY(), z=block.getZ()+face.getModZ();
            if(y>=block.getWorld().getMinHeight()&&y<block.getWorld().getMaxHeight()
                    &&block.getWorld().isChunkLoaded(x>>4,z>>4))
                touch(block.getWorld().getBlockAt(x,y,z));
        }
    }
    @FunctionalInterface private interface Observation { void read() throws IOException; }
    private void observe(Observation observation) {
        if (!ready.getAsBoolean())return;
        try { observation.read(); }
        catch(IOException|RuntimeException failure){fail(failure);}
    }
    @EventHandler(priority=EventPriority.LOWEST)
    public void sign(SignChangeEvent event) { observe(()->neighborhood(event.getBlock())); }
    @EventHandler(priority=EventPriority.LOWEST)
    public void interact(PlayerInteractEvent event) {
        if(event.getClickedBlock()!=null)observe(()->neighborhood(event.getClickedBlock()));
    }
    @EventHandler(priority=EventPriority.LOWEST)
    public void breakBlock(BlockBreakEvent event) { observe(()->neighborhood(event.getBlock())); }
    @EventHandler(priority=EventPriority.LOWEST)
    public void place(BlockPlaceEvent event) {
        observe(()->{
            Block block=event.getBlockPlaced();
            if(block.getState() instanceof Container && scope.includes(block.getWorld().getName())) {
                var position=Position.of(block);
                if(!before.containsKey(position)) {
                    if(event.getBlockReplacedState() instanceof Container)
                        throw new IOException("Container replacement lacks original observation");
                    before.put(position,new Image(position,new ItemStack[0],AssetOwner.resolve(null,false,false,false)));
                }
                touched.add(position);
            }
            neighborhood(block);
        });
    }
    @EventHandler(priority=EventPriority.LOWEST)
    public void explode(BlockExplodeEvent event) {
        observe(()->{for(Block block:event.blockList())neighborhood(block);});
    }
    @EventHandler(priority=EventPriority.LOWEST)
    public void explode(EntityExplodeEvent event) {
        observe(()->{for(Block block:event.blockList())neighborhood(block);});
    }

    /** Shared preimage for inventory GUI and ground-to-block pickup. */
    public void observeInventory(Inventory inventory) {
        if (!ready.getAsBoolean()) return;
        try {
            for (Position position : positions(inventory)) {
                capture(position);
                touched.add(position);
            }
        } catch (IOException | RuntimeException failure) { fail(failure); }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void search(HopperInventorySearchEvent event) {
        if (!ready.getAsBoolean()) return;
        try {
            if (!Bukkit.isPrimaryThread()) throw new IOException("Hopper search off primary");
            if (event.getInventory() == null) return;
            var candidates = positions(event.getInventory());
            if (candidates.isEmpty()) observeEntity.accept(event.getInventory());
            // Minecart hoppers also search for a source, but their block is
            // the rail/location, not the initiating inventory.
            if (event.getBlock().getState() instanceof org.bukkit.block.Hopper)
                capture(Position.of(event.getBlock()));
            for (Position p : candidates) capture(p);
        } catch (IOException | RuntimeException failure) { fail(failure); }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void observeDestination(InventoryMoveItemEvent event) {
        observe(()->{
            // A receiving entity still has its original contents at the
            // move event. Do not reconstruct a debited source at this stage.
            if (event.getDestination().getHolder() instanceof org.bukkit.entity.Entity
                    && PaperLiveItemValue.stack(event.getItem(), COMPRESSED, INFLATED).signum()!=0)
                observeEntity.accept(event.getDestination());
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void move(InventoryMoveItemEvent event) {
        if (!ready.getAsBoolean()) return;
        try {
            if (!Bukkit.isPrimaryThread()) throw new IOException("Hopper move off primary");
            // Calling getItem also prevents Paper from eliding subsequent move events.
            if (PaperLiveItemValue.stack(event.getItem(), COMPRESSED, INFLATED).signum() == 0) return;
            var source = positions(event.getSource());
            var destination = positions(event.getDestination());
            if ((source.isEmpty()&&!entityImage.test(event.getSource()))
                    ||(destination.isEmpty()&&!entityImage.test(event.getDestination())))
                throw new IOException("Monetary hopper move has unsupported inventory source");
            for (Position p : source)
                if (!before.containsKey(p)) throw new IOException("Monetary hopper source lacks pre-search image");
            for (Position p : destination)
                if (!before.containsKey(p)) throw new IOException("Monetary hopper destination lacks pre-search image");
            touched.addAll(source);
            touched.addAll(destination);
        } catch (IOException | RuntimeException failure) { fail(failure); }
    }

    private Map<MemoryAccounting.Endpoint, BigInteger> balances(boolean initial,
            Set<UUID> registrations, List<OwnerStorageIndex.Change> locations) throws IOException {
        Map<MemoryAccounting.Endpoint, BigInteger> result = new HashMap<>();
        Set<UUID> wallets = new HashSet<>();
        for (Position p : touched) {
            Block b = p.block();
            if (!scope.includes(b.getWorld().getName())) continue;
            var image=before.get(p);
            if(image==null)throw new IOException("Block before-image missing");
            Inventory inventory=initial?null:physical(b);
            var holder=OwnerStorageIndex.Holder.block(b.getWorld().getName(),p.x(),p.y(),p.z());
            if(!initial&&inventory==null){locations.add(OwnerStorageIndex.Change.remove(holder));continue;} // ground events own the drops
            ItemStack[] items=initial?image.contents():inventory.getContents();
            var resolved=initial?image.owner():resolve(b);
            if(!initial) {
                if(resolved.kind()==AssetOwner.Kind.PERSONAL) registrations.add(resolved.player());
                if(resolved.player()==null) locations.add(OwnerStorageIndex.Change.remove(holder));
                else locations.add(OwnerStorageIndex.Change.replace(resolved.player(),new OwnerStorageIndex.Location(holder,b.getWorld().getName(),
                        SavedLiveSourcePartition.liveChunkSourceId(new SavedLiveSourcePartition.Chunk(b.getWorld().getName(),p.x()>>4,p.z()>>4)))));
            }
            for (int slot = 0; slot < items.length; slot++) {
                BigInteger value = PaperLiveItemValue.stack(items[slot], COMPRESSED, INFLATED, wallets);
                if (value.signum() == 0) continue;
                var endpoint = switch (resolved.kind()) {
                    case FREE -> MemoryAccounting.Endpoint.free();
                    case PERSONAL -> MemoryAccounting.Endpoint.personal(resolved.player());
                    case DEAD -> MemoryAccounting.Endpoint.dead(resolved.player(), resolved.reason(), p.location(b, slot));
                };
                result.merge(endpoint, value, BigInteger::add);
            }
        }
        return result;
    }

    /** Called before the next world tick; only inventories touched by actual
     * monetary move events are valued, and no snapshots survive the flush. */
    public void flush() {
        try {
            if (!ready.getAsBoolean() || touched.isEmpty()) return;
            var registrations=new HashSet<UUID>();
            var locations=new ArrayList<OwnerStorageIndex.Change>();
            var changes = InventoryAccountingDelta.between(balances(true, registrations, locations),
                    balances(false, registrations, locations));
            if (!changes.isEmpty()) sink.accept(changes);
            if (!registrations.isEmpty()) register.accept(Set.copyOf(registrations));
            if (!locations.isEmpty()) ownerLocations.accept(List.copyOf(locations));
        } catch (IOException | RuntimeException failure) { fail(failure); }
        finally { before.clear(); touched.clear(); }
    }

    private void fail(Exception failure) {
        before.clear();
        touched.clear();
        fatal.accept(failure);
    }
}
