#!/usr/bin/env bash
# Only disposable localhost CI. Never use these authentication settings in production.
set -euo pipefail
root=$(pwd)
fixture=$(realpath "$1")
client="$root/tests/paper-wcl-services-probe/bedrock-join.cjs"
cd "$fixture"
# Final product acceptance must not run the test-only storage probe.
rm -f plugins/StorageProbe.jar
printf '[]\n' > banned-ips.json
sed -i 's/diagnostic-only: true/diagnostic-only: false/' plugins/SimpleCoins/config.yml
sed -i '/^server-port=/d; /^enforce-secure-profile=/d' server.properties
printf 'server-port=25598\nenforce-secure-profile=false\n' >> server.properties
uuid=$(python3 -c 'import hashlib,uuid; print(uuid.UUID(bytes=hashlib.md5(b"OfflinePlayer:ProbeBedrock").digest(),version=3))')
printf '[{"uuid":"%s","name":"ProbeBedrock"}]\n' "$uuid" > whitelist.json
# Authentic provider, checksum pinned to the same verified Geyser build as #690.
mkdir -p geyser-bootstrap/plugins
cp paper.jar geyser-bootstrap/paper.jar
curl -fLsS --retry 3 'https://download.geysermc.org/v2/projects/geyser/versions/2.11.1/builds/1210/downloads/spigot' -o geyser-bootstrap/plugins/Geyser-Spigot.jar
echo '996c1f1997c2db2f9baf802a6a42b374ffadb8d1527638ba5ad765b06b085ecb  geyser-bootstrap/plugins/Geyser-Spigot.jar' | sha256sum -c -
printf 'eula=true\n' > geyser-bootstrap/eula.txt
printf 'online-mode=false\nwhite-list=true\nenforce-whitelist=true\nserver-ip=127.0.0.1\nserver-port=25597\n' > geyser-bootstrap/server.properties
server= client_pid=
cleanup(){ if [[ -n "$client_pid" ]]; then kill "$client_pid" 2>/dev/null || true; fi; if [[ -n "$server" ]]; then kill "$server" 2>/dev/null || true; fi; }
trap cleanup EXIT
wait_for(){
  local log=$1 marker=$2 limit=$3
  for ((i=0;i<limit;i++)); do
    if grep -Fq "$marker" "$log" 2>/dev/null; then return 0; fi
    if ! kill -0 "$server" 2>/dev/null; then break; fi
    sleep 1
  done
  echo "Missing $marker" >&2
  tail -120 "$log" >&2 || true
  return 1
}
mkfifo geyser-bootstrap/console.in
exec 3<>geyser-bootstrap/console.in
(cd geyser-bootstrap && exec timeout --signal=TERM --kill-after=5s 120s java -Xms256M -Xmx1024M -jar paper.jar nogui <console.in >../product-geyser-bootstrap.log 2>&1) &
server=$!
wait_for product-geyser-bootstrap.log 'Done (' 100
printf 'stop\n' >&3
wait "$server"; server=; exec 3>&-
cp geyser-bootstrap/plugins/Geyser-Spigot.jar plugins/
mkdir -p plugins/Geyser-Spigot
cp geyser-bootstrap/plugins/Geyser-Spigot/config.yml plugins/Geyser-Spigot/
python3 - <<'PY'
from pathlib import Path
import re
p=Path('plugins/Geyser-Spigot/config.yml')
lines=p.read_text().splitlines()
start=lines.index('bedrock:')
end=next((i for i in range(start+1,len(lines)) if re.match(r'^[A-Za-z][\w-]*:',lines[i])),len(lines))
lines=lines[:start+1]+['  address: 127.0.0.1']+[l for l in lines[start+1:end] if not l.strip().lstrip('#').strip().startswith('address:')]+lines[end:]
s='\n'.join(lines)+'\n'
for key,val in [('enable-custom-content','true'),('validate-bedrock-login','false'),('auth-type','offline')]:
    s,n=re.subn(r'(?m)^([ \t]*)'+key+r':[ \t]*[^\n#]+(?:#.*)?$',lambda m:m[1]+key+': '+val,s)
    assert n==1,(key,n)
p.write_text(s)
PY
mkfifo product-bedrock-console.in
exec 3<>product-bedrock-console.in
timeout --signal=TERM --kill-after=10s 250s java -Xms256M -Xmx1024M -jar paper.jar nogui <product-bedrock-console.in >product-bedrock.log 2>&1 &
server=$!
wait_for product-bedrock.log 'SIMPLECOINS_PRODUCT_SETTLEMENT_PUBLISHED' 150
wait_for product-bedrock.log 'Started Geyser on 127.0.0.1:19132' 30
wait_for product-bedrock.log 'Registered 10 custom items' 30
ss -H -lun 'sport = :19132' | tee product-bedrock-socket.log
awk 'NF < 5 || ($4 != "127.0.0.1:19132" && $4 != "[::ffff:127.0.0.1]:19132") { bad=1 } END { exit (NR==0 || bad) }' product-bedrock-socket.log
expected=$(python3 -c 'import json,zipfile; print(json.loads(zipfile.ZipFile("plugins/SimpleCoins/geyser/SimpleCoins.mcpack").read("manifest.json"))["header"]["uuid"])')
timeout 100 node "$client" "$expected" >product-bedrock-client.log 2>&1 &
client_pid=$!
wait_for product-bedrock.log 'ProbeBedrock joined the game' 60
wait_for product-bedrock-client.log 'NETWORK_BEDROCK_PRODUCT_SPAWN' 30
printf 'sc give ProbeBedrock 16666\n' >&3
wait_for product-bedrock.log 'Delivered now (including earlier reservations): 16666C; pending reservations: 0' 20
wait_for product-bedrock-client.log 'NETWORK_BEDROCK_NINE_REAL_INVENTORY_COINS_PASSED' 30
wait "$client_pid"; client_pid=
cmp bedrock-received.mcpack plugins/SimpleCoins/geyser/SimpleCoins.mcpack
printf 'sc balance %s\n' "$uuid" >&3
wait_for product-bedrock.log "Personal balance $uuid: 16666 C" 15
wait_for product-bedrock.log 'ProbeBedrock left the game' 20
printf 'stop\n' >&3
wait "$server"; server=; exec 3>&-
grep -F 'ProbeBedrock[/127.0.0.1:' product-bedrock.log
grep -E "^['\"]?$uuid['\"]?: ['\"]?127\.0\.0\.1['\"]?$" plugins/SimpleCoins/last-player-addresses.yml
echo 'SIMPLECOINS_PRODUCT_BEDROCK_ADMISSION_IP_AND_COINS_PASSED'
