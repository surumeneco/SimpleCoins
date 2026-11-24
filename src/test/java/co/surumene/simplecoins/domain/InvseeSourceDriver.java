package co.surumene.simplecoins.domain;
import java.util.Map;
public final class InvseeSourceDriver {
    public void prepare(Map<String,Object> state,Map<String,Object> input){if(!state.isEmpty())throw new IllegalArgumentException("Stateless");}
    public Map<String,Object> observeState(){return Map.of();}
    public Map<String,Object> trigger(String trigger,Map<String,Object> input){
        if(!"収納読取".equals(trigger))throw new IllegalArgumentException(trigger);
        var source=InvseeSourceSelection.select((Boolean)input.get("入力.オンライン"),(Boolean)input.get("入力.有効編集画面"));
        return Map.of("出力.読取源",switch(source){case LIVE->"実Player";case EDITOR->"編集画面";case SAVED->"保存";});
    }
}
