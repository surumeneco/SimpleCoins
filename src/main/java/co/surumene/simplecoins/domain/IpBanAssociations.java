package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Current IP-ban membership from observed last addresses and explicit overrides.
 * Never resolve a hostname or infer an address from player names/history. */
public final class IpBanAssociations {
    private IpBanAssociations() {}

    public static String literal(String address) throws IOException {
        if (address == null || address.isEmpty()) throw new IOException("Missing numeric IP address");
        if (address.indexOf(':') >= 0) {
            if (!address.matches("[0-9A-Fa-f:.]+")) throw new IOException("Expected a numeric IPv6 address");
            try { return InetAddress.getByName(address).getHostAddress(); }
            catch (UnknownHostException invalid) { throw new IOException("Invalid numeric IPv6 address"); }
        }
        String[] parts=address.split("\\.",-1);
        if(parts.length!=4)throw new IOException("Expected a numeric IPv4 address");
        int[] octets=new int[4];
        for(int i=0;i<4;i++){
            if(!parts[i].matches("[0-9]{1,3}"))throw new IOException("Invalid numeric IPv4 address");
            octets[i]=Integer.parseInt(parts[i]);
            if(octets[i]>255)throw new IOException("Invalid numeric IPv4 address");
        }
        return octets[0]+"."+octets[1]+"."+octets[2]+"."+octets[3];
    }

    public static Set<UUID> bannedPlayers(Map<UUID,String> lastAddresses,
            Map<UUID,String> overrides,Set<String> currentBans)throws IOException {
        Objects.requireNonNull(lastAddresses);Objects.requireNonNull(overrides);Objects.requireNonNull(currentBans);
        var banned=new HashSet<String>();
        for(String ip:currentBans)banned.add(literal(ip));
        var effective=new HashMap<UUID,String>(lastAddresses);
        effective.putAll(overrides);
        var result=new HashSet<UUID>();
        for(var entry:effective.entrySet()){
            if(entry.getKey()==null)throw new IOException("Missing IP association UUID");
            if(banned.contains(literal(entry.getValue())))result.add(entry.getKey());
        }
        return Set.copyOf(result);
    }
}
