package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

class SavedTerrainBlockPaletteTest {
    private static Map<String,Object> chunk(int chunkX, int sectionY, List<Map<String,Object>> palette, long[] data) {
        Map<String,Object> states = new HashMap<>();
        states.put("palette",palette);
        if(data != null)states.put("data",data);
        return Map.of("xPos",chunkX,"zPos",0,"sections",
            List.of(Map.of("Y",sectionY,"block_states",states)));
    }

    @Test void readSinglePaletteBlockWithoutData() throws Exception {
        var nbt=chunk(0,4,List.of(Map.of("Name","minecraft:stone")),null);
        var s=SavedTerrainBlockPalette.read(nbt,6,65,4);
        assertEquals("minecraft:stone",s.name());
        assertEquals(Map.of(),s.properties());
    }

    @Test void readPackedChestAtChunkEdgeAndRequireCorrectChunk() throws Exception {
        long[] words=new long[256]; // 4 bits per index, 16 positions per word
        int pos=(1<<8)|(4<<4)|15;
        words[pos/16] = 1L << ((pos%16)*4);
        var palette=List.of(
            Map.<String,Object>of("Name","minecraft:stone"),
            Map.<String,Object>of("Name","minecraft:chest","Properties",Map.of("facing","north","type","left")));
        var nbt=chunk(0,4,palette,words);
        var chest=SavedTerrainBlockPalette.read(nbt,15,65,4);
        assertEquals("minecraft:chest",chest.name());
        assertEquals("left",chest.properties().get("type"));
        assertThrows(IOException.class,()->SavedTerrainBlockPalette.read(nbt,16,65,4));

        long[] right=new long[256]; // x=16 is local x=0
        right[((1<<8)|(4<<4))/16]=1L<<((((1<<8)|(4<<4))%16)*4);
        var opposite=chunk(1,4,List.of(Map.<String,Object>of("Name","minecraft:stone"),
            Map.<String,Object>of("Name","minecraft:chest","Properties",Map.of("facing","north","type","right"))),right);
        assertEquals("right",SavedTerrainBlockPalette.read(opposite,16,65,4).properties().get("type"));
    }

    @Test void rejectMissingPaletteOrInvalidPackedIndexWithoutInventingAir() throws Exception {
        assertThrows(IOException.class,()->SavedTerrainBlockPalette.read(Map.of("xPos",0,"zPos",0,
            "sections",List.of(Map.of("Y",4))),6,65,4));
        var pal=List.of(Map.<String,Object>of("Name","minecraft:air"),
            Map.<String,Object>of("Name","minecraft:stone"));
        long[] bad=new long[256];
        bad[0]=3L; // local 0,0,0, index 3 but only 2 palette entries
        var nbt=chunk(0,0,pal,bad);
        assertThrows(IOException.class,()->SavedTerrainBlockPalette.read(nbt,0,0,0));
        assertThrows(IOException.class,()->SavedTerrainBlockPalette.read(chunk(0,4,pal,new long[1]),6,65,4));
        assertThrows(IOException.class,()->SavedTerrainBlockPalette.read(chunk(0,4,pal,null),6,65,4));
    }

    @Test void rejectMissingOrWrongSectionAndWrongCoordinateTypes() throws Exception {
        var nbt=chunk(0,4,List.of(Map.of("Name","minecraft:stone")),null);
        assertThrows(IOException.class,()->SavedTerrainBlockPalette.read(nbt,6,-1,4));
        assertThrows(IOException.class,()->SavedTerrainBlockPalette.read(nbt,6,65,16));
        assertThrows(IOException.class,()->SavedTerrainBlockPalette.read(Map.of("xPos","0","zPos",0,"sections",
            nbt.get("sections")),6,65,4));
    }
}
