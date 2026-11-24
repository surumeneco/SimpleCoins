package co.surumene.simplecoins.domain;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public final class MemoryAccountingDriver {
    private Map<String, Object> prepared;
    public void prepare(Map<String, Object> state, Map<String, Object> input) {
        if (!state.isEmpty()) throw new IllegalArgumentException("Unexpected state");
        prepared = Map.copyOf(input);
    }
    public Map<String, Object> observeState() { return Map.of(); }
    public Map<String, Object> trigger(String trigger, Map<String, Object> input) {
        if (!"確定仕訳".equals(trigger) || !input.equals(prepared))
            throw new IllegalArgumentException("Unexpected request");
        var account = new MemoryAccounting(Map.of(), BigInteger.TEN, Map.of());
        var issuance = new MemoryAccounting.Movement(MemoryAccounting.Endpoint.external(),
                MemoryAccounting.Endpoint.free(), BigInteger.ONE);
        if (account.record(1, List.of(issuance)) != MemoryAccounting.Outcome.APPLIED)
            throw new AssertionError("Fixture did not establish first operation");
        long sequence = switch ((String) input.get("入力.順序")) {
            case "記帳済" -> 1L;
            case "次" -> 2L;
            case "欠落" -> 3L;
            default -> throw new IllegalArgumentException("Unknown sequence classification");
        };
        var movement = (Boolean) input.get("入力.差額整合") ? issuance :
                new MemoryAccounting.Movement(MemoryAccounting.Endpoint.free(),
                        MemoryAccounting.Endpoint.external(), BigInteger.valueOf(12));
        var result = account.record(sequence, List.of(movement));
        BigInteger expected = result == MemoryAccounting.Outcome.APPLIED
                ? BigInteger.valueOf(12) : BigInteger.valueOf(11);
        if (!account.snapshot().free().equals(expected)) throw new AssertionError("Partial or incorrect mutation");
        return Map.of("出力.結果", switch (result) {
            case APPLIED -> "反映";
            case DUPLICATE -> "重複";
            case REJECTED -> "拒否";
        });
    }
}
