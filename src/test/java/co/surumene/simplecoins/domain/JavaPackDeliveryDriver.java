package co.surumene.simplecoins.domain;

import java.util.Map;

public final class JavaPackDeliveryDriver {
    private Map<String, Object> prepared;
    public void prepare(Map<String, Object> state, Map<String, Object> input) {
        if (!state.isEmpty()) throw new IllegalArgumentException("Unexpected state");
        prepared = Map.copyOf(input);
    }
    public Map<String, Object> observeState() { return Map.of(); }
    public Map<String, Object> trigger(String trigger, Map<String, Object> input) {
        if (!"配布判定".equals(trigger) || !input.equals(prepared))
            throw new IllegalArgumentException("Invalid prepared request");
        return Map.of("出力.要求許可", JavaPackDelivery.shouldRequest(
                (Boolean) input.get("入力.Nexo委任"), (Boolean) input.get("入力.Java接続"),
                (Boolean) input.get("入力.配布情報準備済")));
    }
}
