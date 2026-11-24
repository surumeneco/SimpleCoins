package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/** Select indexed physical holders from only their saved source files.
 * Absence of an indexed holder is not evidence that its assets disappeared.
 * Source authority (unloaded state) and freshness are checked by the caller. */
public final class OwnerSavedStorage {
    private OwnerSavedStorage() {}
    public record Selected(int chunkX,int chunkZ,Map<String,Object> nbt) {}

    public static Map<OwnerStorageIndex.Holder,Selected> read(Path source,
            Set<OwnerStorageIndex.Holder> requested, int compressed, int inflated) throws IOException {
        if(requested.isEmpty()) return Map.of();
        var result = new HashMap<OwnerStorageIndex.Holder,Selected>();
        var kind = requested.iterator().next().kind();
        if(requested.stream().anyMatch(h -> h.kind()!=kind))throw new IOException("Mixed holder kinds in one saved source");
        var wanted = new HashMap<String,OwnerStorageIndex.Holder>();
        for(var holder:requested) {
            if(wanted.put(holder.identity(),holder)!=null)throw new IOException("Duplicate requested holder identity");
        }
        AnvilRegionReader.scan(source,compressed,inflated,chunk -> {
            String list=kind==OwnerStorageIndex.Kind.BLOCK?"block_entities":"Entities";
            if(!(chunk.nbt().get(list) instanceof List<?> compounds))throw new IOException("Invalid saved holder source: "+source);
            for(Object raw:compounds) {
                if(!(raw instanceof Map<?,?> compound))throw new IOException("Malformed saved holder");
                String id;
                if(kind==OwnerStorageIndex.Kind.BLOCK) {
                    if(!(compound.get("x") instanceof Integer x)||!(compound.get("y") instanceof Integer y)||!(compound.get("z") instanceof Integer z))throw new IOException("Invalid saved block coordinate");
                    id=x+","+y+","+z;
                } else {
                    if(!(compound.get("UUID") instanceof int[] u)||u.length!=4)throw new IOException("Invalid saved entity UUID");
                    id=new UUID(((long)u[0]<<32)|Integer.toUnsignedLong(u[1]),((long)u[2]<<32)|Integer.toUnsignedLong(u[3])).toString();
                }
                var holder=wanted.get(id);
                if(holder==null)continue;
                var selected=new HashMap<>(chunk.nbt());
                selected.put(list,List.of(compound));
                // Do not retain unrelated chunk terrain or entity NBT.
                selected.keySet().retainAll(Set.of(list,"xPos","zPos","DataVersion","Position"));
                if(result.putIfAbsent(holder,new Selected(chunk.x(),chunk.z(),Map.copyOf(selected)))!=null)throw new IOException("Ambiguous saved holder: "+holder);
            }
        });
        if(!result.keySet().equals(requested))throw new IOException("Indexed holder missing from saved source: "+source);
        return Map.copyOf(result);
    }
}
