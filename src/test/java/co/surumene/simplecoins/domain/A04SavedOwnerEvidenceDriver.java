package co.surumene.simplecoins.domain;

import java.util.Map;
import java.util.Objects;

/** CAT binding for the ten already-verified boolean inputs; it cannot verify BlockLocker sources. */
public final class A04SavedOwnerEvidenceDriver {
    private Map<String, Object> prepared;

    public void prepare(Map<String, Object> initialState, Map<String, Object> input) {
        if (!Objects.requireNonNull(initialState, "initialState").isEmpty())
            throw new IllegalArgumentException("A04 verdict has no state");
        prepared = Map.copyOf(Objects.requireNonNull(input, "input"));
    }

    public Map<String, Object> observeState() {
        return Map.of();
    }

    public Map<String, Object> trigger(String trigger, Map<String, Object> input) {
        if (!"保存保護照合".equals(trigger))
            throw new IllegalArgumentException("Unexpected A04 operation: " + trigger);
        if (prepared == null || !prepared.equals(input))
            throw new IllegalStateException("Prepared A04 input differs from triggered input");
        var evidence = new BlockLockerSavedOwnerVerdict.Evidence(
                value(input, "入力.保存地形ソース同定済"),
                value(input, "入力.全関連看板照合済"),
                value(input, "入力.物理収納重複排除済"),
                value(input, "入力.保存源局所世代整合済"),
                value(input, "入力.保護適用を検証済"),
                value(input, "入力.非保護を検証済"),
                value(input, "入力.主所有者UUID検証済"),
                value(input, "入力.Everyone有効を検証済"),
                value(input, "入力.Everyone不在を検証済"),
                value(input, "入力.矛盾あり"));
        var result = BlockLockerSavedOwnerVerdict.decide(evidence);
        return Map.of(
                "出力.所有者候補", result == BlockLockerSavedOwnerVerdict.Verdict.OWNER_UNPUBLISHED,
                "出力.フリー候補", result == BlockLockerSavedOwnerVerdict.Verdict.FREE_UNPUBLISHED,
                "出力.決算保留", result == BlockLockerSavedOwnerVerdict.Verdict.HOLD);
    }

    private static boolean value(Map<String, Object> input, String key) {
        if (!(input.get(key) instanceof Boolean result))
            throw new IllegalArgumentException("Missing or non-boolean A04 evidence: " + key);
        return result;
    }
}
