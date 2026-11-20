package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.util.*;
import org.junit.jupiter.api.Test;

class SavedBlockLockerAttachedProtectionTest {
    private static final int Y=65;
    private static final UUID OWNER=UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final Set<String> PRIVATE=Set.of("[Private]"), MORE=Set.of("[More Users]"), EVERYONE=Set.of("Everyone");

    private static final class Fixture {
        final Map<Integer,long[]> packed=new HashMap<>();
        final Map<Integer,List<Map<String,Object>>> entities=new HashMap<>();
        final List<Map<String,Object>> palette=List.of(
            Map.of("Name","minecraft:air"),
            Map.of("Name","minecraft:chest","Properties",Map.of("facing","north","type","single")),
            Map.of("Name","minecraft:chest","Properties",Map.of("facing","north","type","left")),
            Map.of("Name","minecraft:chest","Properties",Map.of("facing","north","type","right")),
            Map.of("Name","minecraft:oak_wall_sign","Properties",Map.of("facing","south")),
            Map.of("Name","minecraft:oak_wall_sign","Properties",Map.of("facing","north")));
        void block(int x,int z,int index) {
            int cx=Math.floorDiv(x,16),offset=(1<<8)|(Math.floorMod(z,16)<<4)|Math.floorMod(x,16);
            long[] words=packed.computeIfAbsent(cx,unused->new long[256]);
            words[offset/16]|=((long)index)<<((offset%16)*4);
        }
        void chest(int x,int type) {block(x,4,type);}
        void sign(int x,String owner,boolean frontEveryone,boolean backEveryone) {
            signFacing(x,owner,frontEveryone,backEveryone,4,true);
        }
        void signFacing(int x,String owner,boolean frontEveryone,boolean backEveryone,int state,boolean blockEntity) {
            block(x,5,state);
            if(!blockEntity)return;
            var data=new HashMap<String,Object>();
            data.put("id","minecraft:sign");
            data.put("x",x);data.put("y",Y);data.put("z",5);
            data.put("front_text",Map.of("messages",List.of(
                "[Private]",owner,frontEveryone?"[Everyone]":"","")));
            data.put("back_text",Map.of("messages",List.of(
                backEveryone?"[More Users]":"",backEveryone?"[Everyone]":"","","")));
            entities.computeIfAbsent(Math.floorDiv(x,16),unused->new ArrayList<>()).add(data);
        }
        Map<SavedBlockLockerAttachedProtection.ChunkKey, Map<String,Object>> build() {
            var chunks=new HashMap<SavedBlockLockerAttachedProtection.ChunkKey,Map<String,Object>>();
            for(int cx=0;cx<=1;cx++) {
                var states=Map.<String,Object>of("palette",palette,"data",packed.getOrDefault(cx,new long[256]));
                var section=Map.<String,Object>of("Y",4,"block_states",states);
                chunks.put(new SavedBlockLockerAttachedProtection.ChunkKey(cx,0),
                    Map.of("xPos",cx,"zPos",0,"sections",List.of(section),
                        "block_entities",entities.getOrDefault(cx,List.of())));
            }
            return chunks;
        }
    }
    private static SavedBlockLockerAttachedProtection.Observation observe(Fixture fixture,int x)
            throws IOException {
        return SavedBlockLockerAttachedProtection.observe(
            fixture.build(),x,Y,4,PRIVATE,MORE,EVERYONE,raw->raw);
    }

    @Test void classifyOnlyPhysicallyAttachedEffectiveProfiles() throws Exception {
        Fixture f=new Fixture();
        f.chest(3,1);f.chest(6,1);f.chest(9,1);f.chest(12,1);
        f.sign(6,"ProbeOwner#"+OWNER,false,false);
        f.sign(9,"UnknownNoUUID",true,false);
        f.sign(12,"UnknownNoUUID",false,false);
        var plain=observe(f,3);
        assertFalse(plain.hasProtectionSign());
        assertTrue(plain.appliedSigns().isEmpty());
        assertEquals(1,plain.parts().size());
        var owner=observe(f,6);
        assertTrue(owner.hasProtectionSign());
        assertEquals(OWNER,owner.primaryOwner().orElseThrow());
        assertFalse(owner.everyoneEffective());
        assertEquals(1,owner.appliedSigns().size());
        var free=observe(f,9);
        assertTrue(free.everyoneEffective());
        assertTrue(free.primaryOwner().isEmpty());
        var hold=observe(f,12);
        assertTrue(hold.hasProtectionSign());
        assertFalse(hold.everyoneEffective());
        assertTrue(hold.primaryOwner().isEmpty());
    }

    @Test void backEveryoneOnOneHalfAffectsEntireVerifiedCrossChunkChest() throws Exception {
        Fixture f=new Fixture();
        f.chest(15,2);f.chest(16,3);
        f.sign(15,"UnknownNoUUID",false,true);
        var left=observe(f,15);
        var right=observe(f,16);
        assertEquals(2,left.parts().size());
        assertEquals(left.parts(),right.parts());
        assertEquals(left.appliedSigns(),right.appliedSigns());
        assertTrue(left.everyoneEffective());
        assertTrue(right.everyoneEffective());
    }

    @Test void missingPartnerChunkAndMissingAttachedSignNbtHold() {
        Fixture f=new Fixture();
        f.chest(15,2);f.chest(16,3);f.sign(15,"ProbeOwner#"+OWNER,false,false);
        var incomplete=f.build();
        incomplete.remove(new SavedBlockLockerAttachedProtection.ChunkKey(1,0));
        assertThrows(IOException.class,()->SavedBlockLockerAttachedProtection.observe(
            incomplete,15,Y,4,PRIVATE,MORE,EVERYONE,raw->raw));

        Fixture broken=new Fixture();broken.chest(6,1);
        broken.signFacing(6,"ProbeOwner#"+OWNER,false,false,4,false);
        assertThrows(IOException.class,()->observe(broken,6));
    }

    @Test void wrongFacingSignDoesNotBecomeAttachedProtectionAndBrokenPairHolds() throws Exception {
        Fixture f=new Fixture();f.chest(6,1);
        f.signFacing(6,"ProbeOwner#"+OWNER,false,false,5,true);
        assertFalse(observe(f,6).hasProtectionSign(),
            "A north-facing wall sign one block south of a chest does not attach");

        Fixture pair=new Fixture();pair.chest(15,2);pair.chest(16,1);
        assertThrows(IOException.class,()->observe(pair,15));
    }
}
