package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.IpBanAssociations;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/** Server-local last accepted player addresses. Manual overrides are separate
 * so automatic writes never replace operator edits. Only UUID and last IP
 * are retained; no addresses are printed in diagnostics or public statistics. */
public final class PaperIpBanAssociations {
    public static final String LAST_FILE="last-player-addresses.yml";
    public static final String OVERRIDE_FILE="ip-ban-overrides.yml";
    private final Path lastFile,overrideFile;
    private Map<UUID,String> last;

    public PaperIpBanAssociations(Path directory)throws IOException {
        Files.createDirectories(directory);
        lastFile=directory.resolve(LAST_FILE);overrideFile=directory.resolve(OVERRIDE_FILE);
        last=read(lastFile);
        if(Files.notExists(overrideFile))Files.writeString(overrideFile,
                "# UUID: 'numeric IP address'\n# A manual value takes priority over last-player-addresses.yml.\n# Remove the entry to restore automatic matching. Server-private data.\n{}\n",
                java.nio.file.StandardOpenOption.CREATE_NEW);
        read(overrideFile); // invalid operator data must not look like an empty map
    }

    private static Map<UUID,String> read(Path file)throws IOException {
        if(Files.notExists(file))return Map.of();
        var yaml=new YamlConfiguration();
        try {yaml.loadFromString(Files.readString(file));}
        catch(InvalidConfigurationException invalid){throw new IOException("Invalid IP association YAML: "+file.getFileName());}
        var result=new HashMap<UUID,String>();
        for(String key:yaml.getKeys(false)){
            UUID id;
            try {id=UUID.fromString(key);if(!id.toString().equalsIgnoreCase(key))throw new IllegalArgumentException();}
            catch(IllegalArgumentException invalid){throw new IOException("Expected UUID key in "+file.getFileName());}
            Object raw=yaml.get(key);
            if(!(raw instanceof String ip))throw new IOException("Expected numeric IP string in "+file.getFileName());
            if(result.putIfAbsent(id,IpBanAssociations.literal(ip))!=null)throw new IOException("Duplicate UUID in "+file.getFileName());
        }
        return Map.copyOf(result);
    }

    /** Only called for a real joined Player, not a rejected login or name lookup. */
    public void record(UUID owner,InetAddress address)throws IOException {
        Objects.requireNonNull(owner);
        if(address==null)throw new IOException("Joined player has no resolved address");
        // Scope identifiers are local interface metadata, not part of a BAN IP.
        String ip=IpBanAssociations.literal(address.getHostAddress().split("%",2)[0]);
        if(ip.equals(last.get(owner)))return;
        var next=new HashMap<>(last);next.put(owner,ip);
        var yaml=new YamlConfiguration();
        next.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->yaml.set(e.getKey().toString(),e.getValue()));
        Path temporary=Files.createTempFile(lastFile.getParent(),".last-player-addresses-",".tmp");
        try {
            Files.writeString(temporary,"# Automatically recorded on successful join. Server-private data.\n"+yaml.saveToString());
            Files.move(temporary,lastFile,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
            last=Map.copyOf(next);
        } finally {Files.deleteIfExists(temporary);}
    }

    /** Read operator overrides at each current BAN capture, not just on restart. */
    public Set<UUID> bannedPlayers(Set<String> currentBans)throws IOException {
        return IpBanAssociations.bannedPlayers(last,read(overrideFile),currentBans);
    }

}
