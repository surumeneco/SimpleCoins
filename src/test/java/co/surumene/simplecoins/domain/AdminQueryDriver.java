package co.surumene.simplecoins.domain;

import java.util.Map;

public final class AdminQueryDriver {
    private Map<String,Object> prepared;
    public void prepare(Map<String,Object> state, Map<String,Object> input) {
        if (!state.isEmpty()) throw new IllegalArgumentException("Unexpected state");
        prepared = Map.copyOf(input);
    }
    public Map<String,Object> observeState() { return Map.of(); }
    public Map<String,Object> trigger(String trigger, Map<String,Object> input) {
        if (!"管理照会".equals(trigger) || !input.equals(prepared)) throw new IllegalArgumentException("Unexpected request");
        var kind = switch ((String) input.get("入力.照会種別")) {
            case "統計" -> AdminQuery.Kind.STATISTICS;
            case "個人" -> AdminQuery.Kind.PLAYER;
            case "死蔵" -> AdminQuery.Kind.DEAD;
            default -> throw new IllegalArgumentException("Unknown query");
        };
        var outcome = AdminQuery.evaluate((Boolean)input.get("入力.管理権限"), (Boolean)input.get("入力.確定集計"), kind, (Boolean)input.get("入力.登録済"));
        return Map.of("出力.結果", switch(outcome) {
            case DENIED -> "拒否";
            case UNAVAILABLE -> "未確定";
            case UNREGISTERED -> "未登録";
            case DISPLAY -> "表示";
        });
    }
}
