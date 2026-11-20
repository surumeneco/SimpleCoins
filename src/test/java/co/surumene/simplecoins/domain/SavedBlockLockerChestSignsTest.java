package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SavedBlockLockerChestSignsTest {
    private static final int Y = 65;
    private static final UUID OWNER = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final Set<String> PRIVATE=Set.of("[Private]");
    private static final Set<String> MORE=Set.of("[More Users]");
    private static final Set<String> EVERYONE=Set.of("Everyone");

    private static SavedBlockLockerChestSigns.Position p(int x,int z) {
        return new SavedBlockLockerChestSigns.Position(x,Y,z);
    }
    private static Map<String,Object> block(String name, String... attrs) {
        if(attrs.length == 0) return Map.of("Name","minecraft:"+name);
        Map<String,String> props=new LinkedHashMap<>();
        for(int i=0;i<attrs.length;i+=2) props.put(attrs[i],attrs[i+1]);
        return Map.of("Name","minecraft:"+name,"Properties",props);
    }
    private static Map<String,Object> sign(int x,int z,String header,String first,Map<String,Object> pdc) {
        return Map.of("id","minecraft:sign","x",x,"y",Y,"z",z,
            "front_text",Map.of("messages",List.of(header,first,"","")),
            "back_text",Map.of("messages",List.of("","","","")),
            "PublicBukkitValues",pdc);
    }
    private static Map<String,Object> ownerPdc() {
        return Map.of("blocklocker:header","PRIVATE",
            "blocklocker:profile_1",Map.of("blocklocker:n","Alice",
                "blocklocker:u",new long[]{OWNER.getMostSignificantBits(),OWNER.getLeastSignificantBits()}));
    }
    private static Map<String,Object> chunk(int cx,int cz,
            Map<SavedBlockLockerChestSigns.Position,Map<String,Object>> blocks,
            List<Map<String,Object>> signs) {
        List<Map<String,Object>> palette=new ArrayList<>();
        palette.add(block("air"));
        long[] packed=new long[256];
        for (var entry:blocks.entrySet()) {
            Map<String,Object> state=entry.getValue();
            int i=palette.indexOf(state);
            if(i<0){palette.add(state);i=palette.size()-1;}
            assertTrue(i<16,"Test fixture only supports 4-bit palette");
            var pos=entry.getKey();
            int index=((pos.y()&15)<<8)|((pos.z()&15)<<4)|(pos.x()&15);
            packed[index/16] |= ((long)i) << ((index%16)*4);
        }
        Map<String,Object> states=Map.of("palette",palette,"data",packed);
        return Map.of("xPos",cx,"zPos",cz,"sections",
            List.of(Map.of("Y",4,"block_states",states)),"block_entities",signs);
    }
    private static SavedBlockLockerChestSigns.Observation observe(
            Map<SavedBlockLockerChestSigns.ChunkPosition,Map<String,Object>> chunks,
            SavedBlockLockerChestSigns.Position chest) throws IOException {
        return SavedBlockLockerChestSigns.observe(chunks,chest,PRIVATE,MORE,EVERYONE,raw -> raw);
    }

    @Test void facingWallSignAttachesButWrongDirectionAndOrdinarySignDoNot() throws Exception {
        Map<SavedBlockLockerChestSigns.Position,Map<String,Object>> states=new LinkedHashMap<>();
        states.put(p(10,10),block("chest","facing","north","type","single"));
        states.put(p(10,11),block("oak_wall_sign","facing","south"));
        states.put(p(11,10),block("oak_wall_sign","facing","south"));
        states.put(p(9,10),block("oak_wall_sign","facing","west"));
        var signs=List.of(sign(10,11,"[Private]","Alice",ownerPdc()),
            sign(11,10,"[Private]","Other",Map.of()),
            sign(9,10,"Hello","Anybody",Map.of()));
        var chunks=Map.of(new SavedBlockLockerChestSigns.ChunkPosition(0,0),
            chunk(0,0,states,signs));
        var result=observe(chunks,p(10,10));
        assertEquals(List.of(p(10,10)),result.storageMembers());
        assertEquals(1,result.attachedSigns().size());
        var applied=result.attachedSigns().get(0);
        assertEquals(p(10,11),applied.position());
        assertEquals(OWNER,applied.profiles().primaryOwner().orElseThrow());
        assertFalse(applied.profiles().everyoneEffective());
    }

    @Test void crossChunkDoubleChestObservesSignsOnBothHalves() throws Exception {
        var a=new LinkedHashMap<SavedBlockLockerChestSigns.Position,Map<String,Object>>();
        var b=new LinkedHashMap<SavedBlockLockerChestSigns.Position,Map<String,Object>>();
        a.put(p(15,4),block("chest","facing","north","type","left"));
        b.put(p(16,4),block("chest","facing","north","type","right"));
        a.put(p(15,5),block("oak_wall_sign","facing","south"));
        b.put(p(16,5),block("oak_wall_sign","facing","south"));
        var chunks=Map.of(
            new SavedBlockLockerChestSigns.ChunkPosition(0,0),
            chunk(0,0,a,List.of(sign(15,5,"[Private]","Alice",ownerPdc()))),
            new SavedBlockLockerChestSigns.ChunkPosition(1,0),
            chunk(1,0,b,List.of(sign(16,5,"[More Users]","[Everyone]",Map.of()))));
        var resolved=observe(chunks,p(16,4));
        assertEquals(Set.of(p(15,4),p(16,4)),Set.copyOf(resolved.storageMembers()));
        assertEquals(Set.of(p(15,5),p(16,5)),
            Set.of(resolved.attachedSigns().get(0).position(),resolved.attachedSigns().get(1).position()));
        assertEquals(1,resolved.attachedSigns().stream()
            .filter(x->x.profiles().everyoneEffective()).count());
        assertEquals(1,resolved.attachedSigns().stream()
            .filter(x->x.profiles().primaryOwner().isPresent()).count());
    }

    @Test void observedEmptyMeansNoAttachedSignsOnlyForSuppliedNeighborhood() throws Exception {
        var states=Map.of(p(10,10),block("chest","facing","north","type","single"));
        var result=observe(Map.of(new SavedBlockLockerChestSigns.ChunkPosition(0,0),
            chunk(0,0,states,List.of())),p(10,10));
        assertTrue(result.attachedSigns().isEmpty());
    }

    @Test void missingAdjacentChunkIsNotEvidenceOfNoProtection() {
        var states=Map.of(p(0,10),block("chest","facing","north","type","single"));
        var chunks=Map.of(new SavedBlockLockerChestSigns.ChunkPosition(0,0),
            chunk(0,0,states,List.of()));
        assertThrows(IOException.class,()->observe(chunks,p(0,10)));
    }

    @Test void realSignWithMissingBlockEntityIsUnknown() {
        var states=Map.of(p(10,10),block("chest","facing","north","type","single"),
            p(10,11),block("oak_wall_sign","facing","south"));
        var chunks=Map.of(new SavedBlockLockerChestSigns.ChunkPosition(0,0),
            chunk(0,0,states,List.of()));
        assertThrows(IOException.class,()->observe(chunks,p(10,10)));
    }

    @Test void brokenDoubleChestMustNotFallbackToSingle() {
        var states=Map.of(p(10,10),block("chest","facing","north","type","left"),
            p(11,10),block("chest","facing","north","type","single"));
        var chunks=Map.of(new SavedBlockLockerChestSigns.ChunkPosition(0,0),
            chunk(0,0,states,List.of()));
        assertThrows(IOException.class,()->observe(chunks,p(10,10)));
    }
}
