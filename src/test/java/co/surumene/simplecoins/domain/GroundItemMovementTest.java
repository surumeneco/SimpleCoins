package co.surumene.simplecoins.domain;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

final class GroundItemMovementTest {
    @Test void allMovementRoutes() throws Exception {
        var driver=new GroundItemMovementDriver();
        for(String kind:new String[]{"生成","消失","プレイヤー投棄","プレイヤー回収"})
        for(boolean included:new boolean[]{false,true}) for(boolean confirmed:new boolean[]{false,true}) {
            String free="不変", personal="不変";
            if(confirmed) {
                if(included) free=switch(kind) {case "生成"->"増";case "消失","プレイヤー回収"->"減";default->"不変";};
                personal=switch(kind) {case "プレイヤー投棄"->"減";case "プレイヤー回収"->"増";default->"不変";};
            }
            assertEquals(Map.of("出力.フリー",free,"出力.個人",personal),driver.trigger("イベント",Map.of(
                    "入力.種別",kind,"入力.対象ワールド",included,"入力.確定",confirmed)));
        }
    }
}
