package co.surumene.simplecoins.domain;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

final class TraderLiveTest {
    @Test void allConditions() throws Exception {
        var driver=new TraderLiveDriver();
        for(boolean ready:new boolean[]{false,true}) for(boolean trader:new boolean[]{false,true})
        for(boolean cancelled:new boolean[]{false,true}) for(boolean success:new boolean[]{false,true}) {
            var actual=driver.trigger("イベント",Map.of("入力.準備済",ready,"入力.行商人",trader,
                    "入力.キャンセル",cancelled,"入力.抽選成功",success));
            assertEquals(Map.of("出力.発行",ready&&trader&&!cancelled&&success),actual);
        }
    }
}
