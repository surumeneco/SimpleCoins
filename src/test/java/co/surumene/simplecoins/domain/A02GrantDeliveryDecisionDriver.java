package co.surumene.simplecoins.domain;

import java.util.Map;
import java.util.Objects;

/**
 * CAT A02 PI-only binding. It cannot authenticate disk contents, writer
 * quiescence, ledger identity or the durability of any acknowledgment.
 */
public final class A02GrantDeliveryDecisionDriver {
    private Map<String, Object> prepared;

    public void prepare(Map<String, Object> initialState, Map<String, Object> input) {
        if (!Objects.requireNonNull(initialState, "initialState").isEmpty())
            throw new IllegalArgumentException("A02 verdict has no persistent Process state");
        prepared = Map.copyOf(Objects.requireNonNull(input, "input"));
    }

    public Map<String, Object> observeState() { return Map.of(); }

    public Map<String, Object> trigger(String trigger, Map<String, Object> input) {
        if (prepared == null || !prepared.equals(input))
            throw new IllegalStateException("A02 input changed after preparation");
        var evidence = new GrantDeliveryDecision.Evidence(
                value(input, "入力.予約あり"),
                value(input, "入力.保存済み現物受領対"),
                value(input, "入力.予約全額受領済"),
                value(input, "入力.保存世代証拠有効"),
                value(input, "入力.他収納保全"),
                value(input, "入力.未ack"),
                value(input, "入力.矛盾あり"));
        var verdict = switch (trigger) {
            case "配布検査" -> GrantDeliveryDecision.prepare(evidence);
            case "保存後照合" -> GrantDeliveryDecision.verify(evidence);
            case "クラッシュ復旧" -> GrantDeliveryDecision.recover(evidence);
            default -> throw new IllegalArgumentException("Unknown A02 trigger: " + trigger);
        };
        return Map.of(
                "出力.ack許可", verdict == GrantDeliveryDecision.Verdict.ALLOW_ACK,
                "出力.予約維持", verdict == GrantDeliveryDecision.Verdict.KEEP_PENDING
                        || verdict == GrantDeliveryDecision.Verdict.KEEP_PENDING_AND_BLOCK,
                "出力.異常遮断", verdict == GrantDeliveryDecision.Verdict.KEEP_PENDING_AND_BLOCK
                        || verdict == GrantDeliveryDecision.Verdict.BLOCK_WITHOUT_PENDING);
    }

    private static boolean value(Map<String, Object> input, String key) {
        if (!(input.get(key) instanceof Boolean result))
            throw new IllegalArgumentException("Missing or non-boolean A02 evidence: " + key);
        return result;
    }
}
