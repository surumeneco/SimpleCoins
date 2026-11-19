package co.surumene.simplecoins.domain;

import java.util.Map;
import java.util.Objects;

/**
 * CAT technology binding for the Confirmed A05 PI-only decision.
 *
 * Input booleans are supplied by the generated vectors; this driver does NOT
 * perform or attest the Process's WCL observations, action effects, or G0.
 */
public final class A05WclEvidenceDriver {
    private Map<String, Object> prepared;

    public void prepare(Map<String, Object> initialState, Map<String, Object> input) {
        if (!Objects.requireNonNull(initialState, "initialState").isEmpty()) {
            throw new IllegalArgumentException("A05 local verdict has no Process state");
        }
        prepared = Map.copyOf(Objects.requireNonNull(input, "input"));
    }

    public Map<String, Object> observeState() {
        return Map.of();
    }

    public Map<String, Object> trigger(String trigger, Map<String, Object> input) {
        if (!"収納照合".equals(trigger)) {
            throw new IllegalArgumentException("Unknown A05 trigger: " + trigger);
        }
        if (prepared == null || !prepared.equals(input)) {
            throw new IllegalStateException("CAT prepared input differs from triggered input");
        }

        var evidence = new WclStorageEvidenceDecision.Evidence(
                value(input, "入力.必要提供者検証済"),
                value(input, "入力.対象個体確認済"),
                value(input, "入力.対象外検証済"),
                value(input, "入力.読取成功"),
                value(input, "入力.個体と提供者の同一性一致"),
                value(input, "入力.帰属根拠確認済"),
                value(input, "入力.同一世代かつ重複排除済"),
                value(input, "入力.矛盾あり"));
        var verdict = WclStorageEvidenceDecision.decide(evidence);
        return Map.of(
                "出力.候補採用", verdict == WclStorageEvidenceDecision.Verdict.ADOPT_UNPUBLISHED,
                "出力.対象外継続", verdict == WclStorageEvidenceDecision.Verdict.CONTINUE_VERIFIED_NON_TARGET,
                "出力.決算保留", verdict == WclStorageEvidenceDecision.Verdict.HOLD);
    }

    private static boolean value(Map<String, Object> input, String key) {
        if (!(input.get(key) instanceof Boolean result)) {
            throw new IllegalArgumentException("Missing or non-boolean A05 input: " + key);
        }
        return result;
    }
}
