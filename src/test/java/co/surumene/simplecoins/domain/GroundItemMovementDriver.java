package co.surumene.simplecoins.domain;

import java.util.Map;

public final class GroundItemMovementDriver {
    public void prepare(Map<String,Object> state, Map<String,Object> input) {}
    public Map<String,Object> observeState() { return Map.of(); }
    public Map<String,Object> trigger(String trigger, Map<String,Object> input) throws Exception {
        Class<?> type=Class.forName("co.surumene.simplecoins.domain.GroundItemMovement");
        String kind=switch((String)input.get("入力.種別")) {
            case "生成" -> "SPAWN";
            case "消失" -> "DESPAWN";
            case "プレイヤー投棄" -> "PLAYER_DROP";
            case "プレイヤー回収" -> "PLAYER_PICKUP";
            default -> throw new IllegalArgumentException("Unknown event");
        };
        Object event=type.getMethod("valueOf",String.class).invoke(null,kind);
        Object delta=type.getMethod("delta",boolean.class,boolean.class).invoke(event,
                (Boolean)input.get("入力.対象ワールド"),(Boolean)input.get("入力.確定"));
        return Map.of("出力.フリー",word((Integer)delta.getClass().getMethod("free").invoke(delta)),
                "出力.個人",word((Integer)delta.getClass().getMethod("personal").invoke(delta)));
    }
    private static String word(int sign) { return sign==0?"不変":sign>0?"増":"減"; }
}
