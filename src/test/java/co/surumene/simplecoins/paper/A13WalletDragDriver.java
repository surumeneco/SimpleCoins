package co.surumene.simplecoins.paper;

import java.util.Map;

/** CAT driver: calls the actual policy, not the TCE oracle. */
public final class A13WalletDragDriver {
    public void prepare(Map<String,Object> state, Map<String,Object> input) {
        if (!state.isEmpty()) throw new IllegalArgumentException("Unexpected state");
    }

    public Map<String,Object> observeState() { return Map.of(); }

    public Map<String,Object> trigger(String operation, Map<String,Object> input) {
        if (!"ドラッグ判定".equals(operation))
            throw new IllegalArgumentException("Unexpected trigger: " + operation);
        return Map.of("出力.取消", WalletDragPolicy.shouldCancel(
            field(input,"入力.先行キャンセル"),
            field(input,"入力.財布セッション有効"),
            field(input,"入力.ドラッグ元が財布"),
            field(input,"入力.下段だけ変更"),
            field(input,"入力.固定財布枠に接触")));
    }

    private static boolean field(Map<String,Object> values, String id) {
        Object value=values.get(id);
        if (!(value instanceof Boolean actual))
            throw new IllegalArgumentException("Missing/invalid CAT input: " + id);
        return actual;
    }
}
