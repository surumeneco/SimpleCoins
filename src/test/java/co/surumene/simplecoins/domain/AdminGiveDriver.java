package co.surumene.simplecoins.domain;
import java.util.Map;
public final class AdminGiveDriver {
    public void prepare(Map<String,Object> pre, Map<String,Object> input) { }
    public Map<String,Object> observeState() { return Map.of(); }
    public Map<String,Object> trigger(String trigger, Map<String,Object> input) {
        if (!"発行受付".equals(trigger)) throw new IllegalArgumentException(trigger);
        var result = AdminGiveRequest.evaluate((Boolean) input.get("入力.発行権限"),
                (Boolean) input.get("入力.決算確定"), (Boolean) input.get("入力.対象確定"),
                (Boolean) input.get("入力.指定有効"));
        return Map.of("出力.結果", switch (result) {
            case DENIED -> "拒否"; case UNAVAILABLE -> "未確定";
            case INVALID -> "不正"; case ACCEPT -> "受付可";
        });
    }
}
