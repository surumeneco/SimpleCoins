package co.surumene.simplecoins.domain;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Driver for the exact, local CAT IP attribution observation. */
public final class IpBanAssociationsDriver {
    private Map<String,Object> prepared;
    public void prepare(Map<String,Object> state, Map<String,Object> input) {
        if (!state.isEmpty()) throw new IllegalArgumentException("Unexpected state");
        prepared=Map.copyOf(input);
    }
    public Map<String,Object> observeState(){return Map.of();}
    public Map<String,Object> trigger(String trigger,Map<String,Object> input) throws Exception {
        if(!"照合".equals(trigger)||!input.equals(prepared))throw new IllegalArgumentException();
        UUID id=UUID.fromString("7bf1ac01-4b51-4d68-8c6e-982fae4c0001");
        boolean auto=(Boolean)input.get("入力.自動一致"), override=(Boolean)input.get("入力.補正あり"), manual=(Boolean)input.get("入力.補正一致");
        var last=Map.of(id,auto?"203.0.113.42":"192.0.2.42");
        Map<UUID,String> overrides=override?Map.of(id,manual?"203.0.113.42":"192.0.2.42"):Map.of();
        return Map.of("出力.IPBAN",IpBanAssociations.bannedPlayers(last,overrides,Set.of("203.0.113.42")).contains(id));
    }
    public static void main(String[] args)throws Exception {
        var d=new IpBanAssociationsDriver();int cases=0;
        for(boolean a:new boolean[]{false,true})for(boolean o:new boolean[]{false,true})for(boolean m:new boolean[]{false,true}){
            var input=Map.<String,Object>of("入力.自動一致",a,"入力.補正あり",o,"入力.補正一致",m);
            d.prepare(Map.of(),input);
            if(!d.trigger("照合",input).equals(Map.of("出力.IPBAN",o?m:a)))throw new AssertionError(input);
            cases++;
        }
        System.out.println("IP BAN association decision matrix passed: "+cases);
    }
}
