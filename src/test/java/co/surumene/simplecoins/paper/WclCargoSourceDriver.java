package co.surumene.simplecoins.paper;

import java.util.Map;

/** Binds the generated PI-only case to the actual production source selection. */
public final class WclCargoSourceDriver {
    public void prepare(Map<String,Object> state,Map<String,Object> input){
        if(!state.isEmpty())throw new IllegalArgumentException("Stateless source selection");
    }
    public Map<String,Object> observeState(){return Map.of();}
    public Map<String,Object> trigger(String trigger,Map<String,Object> input){
        if(!"収納読取".equals(trigger))throw new IllegalArgumentException(trigger);
        Boolean open=switch((String)input.get("入力.GUI状態")){
            case "開" -> Boolean.TRUE;
            case "閉" -> Boolean.FALSE;
            case "不明" -> null;
            default -> throw new IllegalArgumentException("Unknown GUI state");
        };
        var source=PaperWonderfulLiveCandidates.cargoSource(open,(Boolean)input.get("入力.正本確認"));
        return Map.of("出力.読取源",switch(source){
            case GUI -> "GUI優先";
            case WCL -> "WCL";
            case HOLD -> "保留";
        });
    }
}
