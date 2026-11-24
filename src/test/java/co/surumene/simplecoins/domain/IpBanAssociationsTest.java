package co.surumene.simplecoins.domain;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class IpBanAssociationsTest {
    @Test void allDecisionCombinations() throws Exception { IpBanAssociationsDriver.main(new String[0]); }
    @Test void sharedIpOverrideRemovalAndPardon() throws Exception {
        UUID a=UUID.randomUUID(), b=UUID.randomUUID(), unknown=UUID.randomUUID();
        var last=Map.of(a,"203.0.113.42",b,"203.0.113.42");
        var ban=Set.of("203.0.113.42");
        assertEquals(Set.of(a,b),IpBanAssociations.bannedPlayers(last,Map.of(),ban));
        assertEquals(Set.of(b,unknown),IpBanAssociations.bannedPlayers(last,Map.of(a,"192.0.2.5",unknown,"203.0.113.42"),ban));
        assertEquals(Set.of(b),IpBanAssociations.bannedPlayers(Map.of(a,"192.0.2.5",b,"203.0.113.42"),Map.of(),ban));
        assertEquals(Set.of(),IpBanAssociations.bannedPlayers(last,Map.of(),Set.of()));
    }
    @Test void numericNormalizationWithoutHostnameResolution()throws Exception{
        UUID id=UUID.randomUUID();
        assertEquals(Set.of(id),IpBanAssociations.bannedPlayers(Map.of(id,"2001:db8::1"),Map.of(),Set.of("2001:0db8:0:0:0:0:0:1")));
        assertEquals(Set.of(id),IpBanAssociations.bannedPlayers(Map.of(id,"::ffff:203.0.113.42"),Map.of(),Set.of("203.0.113.42")));
        for(String invalid:new String[]{"localhost","203.0.113.42:25565","256.0.0.1","127.1","fe80::1%eth0",""})
            assertThrows(java.io.IOException.class,()->IpBanAssociations.literal(invalid));
    }
    @Test void deadPlayerSlotReplacementDoesNotMoveProtectedAssets(){
        UUID id=UUID.randomUUID(),other=UUID.randomUUID();var reason=AssetOwner.DeadReason.CURRENT_BAN;
        var saved=MemoryAccounting.Endpoint.dead(id,reason,"playerdata/"+id+"/inventory/0");
        var live=MemoryAccounting.Endpoint.dead(id,reason,"player/"+id+"/inventory/0");
        var box=MemoryAccounting.Endpoint.dead(id,reason,"world/world/block/0,64,0/0");
        var unrelated=MemoryAccounting.Endpoint.dead(other,reason,"playerdata/"+other+"/inventory/0");
        var amount=java.math.BigInteger.valueOf(5000);var after=java.math.BigInteger.valueOf(5100);
        var ledger=new MemoryAccounting(Map.of(),java.math.BigInteger.ZERO,Map.of(saved,amount,box,amount,unrelated,amount));
        assertEquals(Map.of(saved,amount),ledger.deadPlayerAssets(id));
        assertEquals(MemoryAccounting.Outcome.APPLIED,ledger.recordNext(InventoryAccountingDelta.between(ledger.deadPlayerAssets(id),Map.of(live,after)),Set.of()));
        assertEquals(Map.of(live,after,box,amount,unrelated,amount),ledger.snapshot().dead());
        assertTrue(ledger.snapshot().personal().isEmpty());
    }
}
