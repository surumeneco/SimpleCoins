package co.surumene.simplecoins.paper;

import java.util.Map;
import java.util.Objects;

/**
 * CAT JUnit driver for the confirmed local pure decision only.
 * The oracle is generated from TCE; this driver observes the production
 * CoinExchangeRecipes decision method and must never implement its own oracle.
 * The live Paper event adapter is independently exercised by StationResultGuardLiveProbe.
 */
public final class A12StationResultDecisionDriver {
    private Map<String, Object> prepared;

    public void prepare(Map<String, Object> state, Map<String, Object> input) {
        if (!state.isEmpty()) throw new IllegalArgumentException("Unexpected state");
        prepared = Map.copyOf(Objects.requireNonNull(input, "input"));
    }

    public Map<String, Object> observeState() {
        return Map.of();
    }

    public Map<String, Object> trigger(String operation, Map<String, Object> input) {
        if (!"判定".equals(operation)) throw new IllegalArgumentException("Unexpected trigger");
        if (!Objects.equals(prepared, input))
            throw new IllegalStateException("Input changed since prepare");
        boolean target = requiredBool(input, "入力.対象結果操作");
        boolean readable = requiredBool(input, "入力.入力読取可能");
        boolean marked = requiredBool(input, "入力.貨幣マーカーあり");
        boolean cancel = CoinExchangeRecipes.stationResultShouldCancel(target, readable, marked);
        return Map.of("出力.許可", !cancel, "出力.取消", cancel);
    }

    private static boolean requiredBool(Map<String, Object> input, String key) {
        if (!(input.get(key) instanceof Boolean value))
            throw new IllegalArgumentException("CAT input must be Boolean: " + key);
        return value;
    }
}
