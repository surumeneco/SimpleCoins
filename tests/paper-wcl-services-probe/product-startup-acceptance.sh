#!/usr/bin/env bash
# Isolated disposable fixture; actual product JAR and loopback client only.
set -euo pipefail
fixture=$1
client=$(realpath "$2")
cd "$fixture"
rm -f plugins/StorageProbe.jar
printf '[]\n' > banned-ips.json
sed -i 's/diagnostic-only: true/diagnostic-only: false/' plugins/SimpleCoins/config.yml
sed -i '/^server-port=/d; /^enforce-secure-profile=/d' server.properties
printf 'server-port=25598\nenforce-secure-profile=false\n' >> server.properties
uuid=3c62cc0b-2256-346d-9f5c-4faa2150e23d
printf '[{"uuid":"%s","name":"ProbeSettlement"}]\n' "$uuid" > whitelist.json
server= client_pid=
cleanup() {
  if [[ -n "$client_pid" ]]; then kill "$client_pid" 2>/dev/null || true; fi
  if [[ -n "$server" ]]; then kill "$server" 2>/dev/null || true; fi
}
trap cleanup EXIT
trap 'echo "Product acceptance failed at line $LINENO" >&2; tail -100 product-startup.log product-client.log product-restart.log 2>/dev/null || true' ERR
wait_for() {
  local log=$1 marker=$2 limit=${3:-150}
  for ((i=0;i<limit;i++)); do
    if grep -Fq "$marker" "$log" 2>/dev/null; then return 0; fi
    if ! kill -0 "$server" 2>/dev/null; then break; fi
    sleep 1
  done
  echo "Missing expected product observation: $marker" >&2
  tail -180 "$log" >&2 || true
  return 1
}
start_server() {
  local log=$1
  rm -f console.fifo
  mkfifo console.fifo
  exec 3<>console.fifo
  timeout --signal=TERM --kill-after=10s 300s java -Xms256M -Xmx1024M -jar paper.jar nogui <console.fifo >"$log" 2>&1 &
  server=$!
  wait_for "$log" 'SIMPLECOINS_PRODUCT_SETTLEMENT_PUBLISHED'
}
command_to_server() { printf '%s\n' "$*" >&3; }
stop_server() {
  command_to_server stop
  wait "$server" || { tail -180 "$1"; return 1; }
  server=
  exec 3>&-
  rm -f console.fifo
}
start_server product-startup.log
wait_for product-startup.log 'SIMPLECOINS_PRODUCT_SETTLEMENT_PREPARED_UNPUBLISHED personal=25000 free=1000 dead=30000'
command_to_server 'sc stats'
wait_for product-startup.log 'Personal P: 25000 C; free F: 1000 C; world T=P+F: 26000 C'
wait_for product-startup.log 'Dead D (excluded from T): 30000 C'
timeout 190 "$client" --wallet-scenario >product-client.log 2>&1 &
client_pid=$!
wait_for product-startup.log 'ProbeSettlement joined the game'
wait_for product-client.log 'NETWORK_CLIENT_PLAY_LOGIN'
command_to_server 'sc give ProbeSettlement 5200'
wait_for product-startup.log 'Delivered now (including earlier reservations): 5200C; pending reservations: 0'
command_to_server "sc balance $uuid"
wait_for product-startup.log "Personal balance $uuid: 5200 C"
command_to_server 'sc stats'
wait_for product-startup.log 'Personal P: 30200 C; free F: 1000 C; world T=P+F: 31200 C'
# A count-only clear must not mutate or create a debit.
command_to_server 'minecraft:clear ProbeSettlement minecraft:paper 0'
sleep 2
command_to_server "sc balance $uuid"
# Use the last response (not the earlier, identical 5200C line).
for ((i=0;i<15;i++)); do
  [[ $(grep -Fc "Personal balance $uuid: 5200 C" product-startup.log || true) -ge 2 ]] && break
  sleep 1
done
[[ $(grep -Fc "Personal balance $uuid: 5200 C" product-startup.log || true) -ge 2 ]]
# A real ordinary vanilla clear must remove issued monetary value, not just items.
command_to_server 'minecraft:clear ProbeSettlement minecraft:paper'
# Give the console command time to finish and ordinary accounting to flush.
sleep 2
command_to_server "sc balance $uuid"
wait_for product-startup.log "Personal balance $uuid: 0 C" 20
# Exercise a different entry point and target context on the actual server.
command_to_server 'sc give ProbeSettlement 100'
wait_for product-startup.log 'Delivered now (including earlier reservations): 100C; pending reservations: 0' 15
command_to_server "sc balance $uuid"
wait_for product-startup.log "Personal balance $uuid: 100 C" 15
command_to_server 'minecraft:execute as ProbeSettlement run minecraft:clear @s minecraft:paper'
sleep 2
command_to_server "sc balance $uuid"
for ((i=0;i<15;i++)); do
  [[ $(grep -Fc "Personal balance $uuid: 0 C" product-startup.log || true) -ge 2 ]] && break
  sleep 1
done
[[ $(grep -Fc "Personal balance $uuid: 0 C" product-startup.log || true) -ge 2 ]]
# Real client GUI transfer, not direct Bukkit event invocation.
command_to_server 'sc give ProbeSettlement wallet 1'
wait_for product-startup.log 'Gave 1 empty wallet(s) to ProbeSettlement' 15
command_to_server 'sc give ProbeSettlement coin_100 2'
wait_for product-startup.log 'Delivered now (including earlier reservations): 200C; pending reservations: 0' 15
command_to_server 'execute if items entity ProbeSettlement hotbar.0 minecraft:bundle[minecraft:custom_data~{PublicBukkitValues:{"simplecoins:wallet_balance":0L}}] run say PRODUCT_WALLET_EMPTY_VERIFIED'
wait_for product-startup.log 'PRODUCT_WALLET_EMPTY_VERIFIED' 15
command_to_server 'execute if items entity ProbeSettlement hotbar.1 minecraft:paper[minecraft:item_model="simplecoins:coin_100"] run say PRODUCT_WALLET_INPUT_VERIFIED'
wait_for product-startup.log 'PRODUCT_WALLET_INPUT_VERIFIED' 15
touch product-wallet-ready
wait_for product-client.log 'NETWORK_WALLET_DEPOSIT_PACKET_OBSERVED' 25
command_to_server 'execute if items entity ProbeSettlement hotbar.0 minecraft:bundle[minecraft:custom_data~{PublicBukkitValues:{"simplecoins:wallet_balance":200L}}] run say PRODUCT_WALLET_DEPOSIT_200_VERIFIED'
wait_for product-startup.log 'PRODUCT_WALLET_DEPOSIT_200_VERIFIED' 15
command_to_server "sc balance $uuid"
wait_for product-startup.log "Personal balance $uuid: 200 C" 15
touch product-wallet-withdraw
wait_for product-client.log 'NETWORK_WALLET_WITHDRAW_PACKET_OBSERVED' 20
command_to_server 'execute if items entity ProbeSettlement hotbar.0 minecraft:bundle[minecraft:custom_data~{PublicBukkitValues:{"simplecoins:wallet_balance":0L}}] run say PRODUCT_WALLET_WITHDRAW_200_VERIFIED'
wait_for product-startup.log 'PRODUCT_WALLET_WITHDRAW_200_VERIFIED' 15
command_to_server 'execute if items entity ProbeSettlement hotbar.1 minecraft:paper[minecraft:item_model="simplecoins:coin_50"] run say PRODUCT_WALLET_FIFTY_COINS_VERIFIED'
wait_for product-startup.log 'PRODUCT_WALLET_FIFTY_COINS_VERIFIED' 15
command_to_server "sc balance $uuid"
for ((i=0;i<15;i++)); do
  [[ $(grep -Fc "Personal balance $uuid: 200 C" product-startup.log || true) -ge 2 ]] && break
  sleep 1
done
[[ $(grep -Fc "Personal balance $uuid: 200 C" product-startup.log || true) -ge 2 ]]
touch product-wallet-redeposit
wait_for product-client.log 'NETWORK_WALLET_REDEPOSIT_AND_CLOSE_PASSED' 20
command_to_server 'execute if items entity ProbeSettlement hotbar.0 minecraft:bundle[minecraft:custom_data~{PublicBukkitValues:{"simplecoins:wallet_balance":200L}}] run say PRODUCT_WALLET_REDEPOSIT_VERIFIED'
wait_for product-startup.log 'PRODUCT_WALLET_REDEPOSIT_VERIFIED' 15
command_to_server "sc balance $uuid"
for ((i=0;i<15;i++)); do
  [[ $(grep -Fc "Personal balance $uuid: 200 C" product-startup.log || true) -ge 3 ]] && break
  sleep 1
done
[[ $(grep -Fc "Personal balance $uuid: 200 C" product-startup.log || true) -ge 3 ]]
# Exercise actual item entity/ownership transitions after closing the wallet GUI.
# A high, isolated platform keeps the fixture away from pre-existing items.
command_to_server 'fill -6 179 -6 6 179 6 minecraft:stone'
command_to_server 'tp ProbeSettlement 0.5 180 0.5 0 70'
sleep 2
touch product-ground-drop
wait_for product-client.log 'NETWORK_WALLET_DROP_SENT' 15
# Count new stats responses; earlier wallet/restart log lines cannot satisfy this.
expect_stats() {
  local marker=$1 old
  old=$(grep -Fc "$marker" product-startup.log || true)
  command_to_server 'sc stats'
  for ((i=0;i<15;i++)); do
    [[ $(grep -Fc "$marker" product-startup.log || true) -gt $old ]] && return 0
    sleep 1
  done
  echo "Missing new stats response: $marker" >&2
  return 1
}
# Dropped items have a thrower pickup delay. Move the player out of reach,
# then make the ordinary collision/pickup path occur after the delay.
sleep 1
command_to_server 'tp ProbeSettlement 5.5 180 5.5'
sleep 2
expect_stats 'Personal P: 25000 C; free F: 1200 C; world T=P+F: 26200 C'
command_to_server 'execute positioned 0.5 180 0.5 if entity @e[type=minecraft:item,distance=..6] run say PRODUCT_GROUND_ITEM_PRESENT'
wait_for product-startup.log 'PRODUCT_GROUND_ITEM_PRESENT' 10
command_to_server 'execute positioned 0.5 180 0.5 run tp ProbeSettlement @e[type=minecraft:item,distance=..6,sort=nearest,limit=1]'
sleep 3
expect_stats 'Personal P: 25200 C; free F: 1000 C; world T=P+F: 26200 C'
command_to_server 'execute if items entity ProbeSettlement hotbar.0 minecraft:bundle[minecraft:custom_data~{PublicBukkitValues:{"simplecoins:wallet_balance":200L}}] run say PRODUCT_GROUND_WALLET_RECOVERED'
wait_for product-startup.log 'PRODUCT_GROUND_WALLET_RECOVERED' 10
# Death is real vanilla kill, not a synthetic PlayerDeathEvent.
command_to_server 'tp ProbeSettlement 0.5 180 0.5'
command_to_server 'minecraft:kill ProbeSettlement'
wait_for product-client.log 'NETWORK_PLAYER_DEATH_OBSERVED' 15
sleep 2
expect_stats 'Personal P: 25000 C; free F: 1200 C; world T=P+F: 26200 C'
touch product-ground-respawn
wait_for product-client.log 'NETWORK_RESPAWN_SENT' 15
sleep 3
command_to_server 'execute positioned 0.5 180 0.5 run tp ProbeSettlement @e[type=minecraft:item,distance=..6,sort=nearest,limit=1]'
sleep 3
expect_stats 'Personal P: 25200 C; free F: 1000 C; world T=P+F: 26200 C'
command_to_server 'execute if items entity ProbeSettlement hotbar.0 minecraft:bundle[minecraft:custom_data~{PublicBukkitValues:{"simplecoins:wallet_balance":200L}}] run say PRODUCT_DEATH_WALLET_RECOVERED'
wait_for product-startup.log 'PRODUCT_DEATH_WALLET_RECOVERED' 10
echo 'SIMPLECOINS_PRODUCT_DROP_PICKUP_DEATH_RECOVERY_PASSED'
command_to_server 'kick ProbeSettlement product acceptance complete'
wait "$client_pid" || { cat product-client.log; exit 1; }
client_pid=
wait_for product-startup.log 'ProbeSettlement left the game'
stop_server product-startup.log
start_server product-restart.log
command_to_server 'sc stats'
wait_for product-restart.log 'Personal P: 25200 C; free F: 1000 C; world T=P+F: 26200 C'
wait_for product-restart.log 'Dead D (excluded from T): 30000 C'
command_to_server "sc balance $uuid"
wait_for product-restart.log "Personal balance $uuid: 200 C"
stop_server product-restart.log
printf 'SIMPLECOINS_PRODUCT_STARTUP_CLIENT_ISSUE_CLEAR_RESTART_PASSED\nSIMPLECOINS_PRODUCT_WALLET_NETWORK_RESTART_PASSED\n'
