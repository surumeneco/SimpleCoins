package co.surumene.simplecoins.domain;

import java.util.Map;
import java.util.Random;
import java.util.Optional;
import java.util.random.RandomGenerator;

public final class TraderLiveDriver {
    public void prepare(Map<String,Object> state, Map<String,Object> input) {}
    public Map<String,Object> observeState() { return Map.of(); }
    public Map<String,Object> trigger(String trigger, Map<String,Object> input) throws Exception {
        boolean ready=(Boolean)input.get("入力.準備済"), trader=(Boolean)input.get("入力.行商人"),
                cancelled=(Boolean)input.get("入力.キャンセル"), roll=(Boolean)input.get("入力.抽選成功");
        var policy = new WanderingTraderDropPolicy(true, roll?1:0, 1, 1, Map.of(CoinDenomination.ONE,1));
        var result=(Optional<?>) WanderingTraderDropPolicy.class.getMethod("drawForDeath",
                boolean.class,boolean.class,boolean.class,RandomGenerator.class)
                .invoke(policy,ready,trader,cancelled,new Random(1));
        return Map.of("出力.発行",result.isPresent());
    }
}
