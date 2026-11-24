package co.surumene.simplecoins.domain;

import java.util.Map;
import java.util.Objects;

/**
 * CAT A01 PI-only binding. The generated Boolean vectors are not authentic
 * writer/asset/Journal observations and must never reach a publish API.
 */
public final class A01WriterEpochDecisionDriver {
    private Map<String, Object> prepared;

    public void prepare(Map<String, Object> initialState, Map<String, Object> input) {
        if (!Objects.requireNonNull(initialState, "initialState").isEmpty())
            throw new IllegalArgumentException("A01 verdict has no persistent Process state");
        prepared = Map.copyOf(Objects.requireNonNull(input, "input"));
    }

    public Map<String, Object> observeState() { return Map.of(); }

    public Map<String, Object> trigger(String trigger, Map<String, Object> input) {
        if (prepared == null || !prepared.equals(input))
            throw new IllegalStateException("A01 input changed after preparation");
        var evidence = new SettlementWriterEpochDecision.Evidence(
                value(input, "入力.全対象読取完了"),
                value(input, "入力.帰属重複照合済"),
                value(input, "入力.競合解消済"),
                value(input, "入力.通常仕訳引継可能"),
                value(input, "入力.参加禁止かつ無人"),
                value(input, "入力.重大な不整合あり"));
        var verdict = switch (trigger) {
            case "公開判定" -> SettlementWriterEpochDecision.evaluate(evidence);
            case "失効通知" -> SettlementWriterEpochDecision.revoke(evidence);
            default -> throw new IllegalArgumentException("Unknown A01 trigger: " + trigger);
        };
        return Map.of(
                "出力.原子公開試行許可",
                    verdict == SettlementWriterEpochDecision.Verdict.TRY_ATOMIC_PUBLISH,
                "出力.未公開維持", verdict == SettlementWriterEpochDecision.Verdict.HOLD_UNPUBLISHED
                        || verdict == SettlementWriterEpochDecision.Verdict.REVOKE_PUBLISHED,
                "出力.公開済を失効", verdict == SettlementWriterEpochDecision.Verdict.REVOKE_PUBLISHED);
    }

    private static boolean value(Map<String, Object> input, String key) {
        if (!(input.get(key) instanceof Boolean result))
            throw new IllegalArgumentException("Missing or non-boolean A01 evidence: " + key);
        return result;
    }
}
