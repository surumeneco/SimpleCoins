#!/usr/bin/env bash
# SimpleCoins fail-closed service launch preflight. Does not edit server files.
set -euo pipefail
if [[ $# -lt 5 || $4 != '--' ]]; then
  echo 'Usage: paper_start_guard.sh SERVER_DIR EXPECTED_SIMPLECOINS_SHA256 EXPECTED_GUARD_SHA256 -- PAPER_LAUNCH_COMMAND [ARGS...]' >&2
  exit 64
fi
server_dir=$1
expected_digest=$2
expected_guard_digest=$3
shift 4
if [[ ! $expected_digest =~ ^[0-9a-f]{64}$ || ! $expected_guard_digest =~ ^[0-9a-f]{64}$ ]]; then
  echo 'START_GUARD_DENIED: expected SHA-256 must be 64 lowercase hex digits' >&2
  exit 65
fi
if [[ ! -d $server_dir || -L $server_dir ]]; then
  echo 'START_GUARD_DENIED: server directory is missing or is a symbolic link' >&2
  exit 66
fi
server_dir=$(cd -- "$server_dir" && pwd -P)
jar="$server_dir/plugins/SimpleCoins.jar"
guard="$server_dir/plugins/SimpleCoinsGuard.jar"
properties="$server_dir/server.properties"
whitelist="$server_dir/whitelist.json"
ops="$server_dir/ops.json"
for input in "$jar" "$guard" "$properties" "$whitelist" "$ops"; do
  if [[ ! -f $input || -L $input ]]; then
    echo "START_GUARD_DENIED: missing or symlinked required input: $input" >&2
    exit 66
  fi
done
actual_digest=$(sha256sum -- "$jar" | cut -d ' ' -f1)
actual_guard_digest=$(sha256sum -- "$guard" | cut -d ' ' -f1)
if [[ $actual_digest != "$expected_digest" || $actual_guard_digest != "$expected_guard_digest" ]]; then
  echo 'START_GUARD_DENIED: SimpleCoins or SimpleCoinsGuard JAR digest mismatch' >&2
  exit 65
fi
# Validate an actual loadable plugin descriptor (not a random matching ZIP),
# strict property uniqueness, and an empty allowlist before starting Paper.
if ! python3 - "$jar" "$guard" "$properties" "$whitelist" "$ops" <<'PY'
import json
import pathlib
import sys
import zipfile

jar, guard, properties, whitelist, ops = (pathlib.Path(s) for s in sys.argv[1:])
try:
    for path, name, main, classname in (
        (jar, 'SimpleCoins', 'co.surumene.simplecoins.SimpleCoinsPlugin',
         'co/surumene/simplecoins/SimpleCoinsPlugin.class'),
        (guard, 'SimpleCoinsGuard', 'co.surumene.simplecoins.guard.SimpleCoinsAdmissionGuard',
         'co/surumene/simplecoins/guard/SimpleCoinsAdmissionGuard.class'),
    ):
        with zipfile.ZipFile(path) as z:
            members = z.namelist()
            if members.count('plugin.yml') != 1 or members.count(classname) != 1:
                raise ValueError(f'{name}: missing or duplicate plugin entrypoint')
            descriptor = z.read('plugin.yml').decode('utf-8')
        # Only exact, non-indented top-level fields are accepted. Duplicate keys fail.
        fields = {}
        for line in descriptor.splitlines():
            if not line or line[0].isspace() or line.lstrip().startswith('#') or ':' not in line:
                continue
            key, value = line.split(':', 1)
            fields.setdefault(key.strip(), []).append(value.strip().strip("'\""))
        if fields.get('name') != [name] or fields.get('main') != [main]:
            raise ValueError(f'{name}: unexpected plugin name or entrypoint')
        if name == 'SimpleCoins' and '\ndepend:\n  - SimpleCoinsGuard\n' not in '\n' + descriptor:
            raise ValueError('SimpleCoins must depend on SimpleCoinsGuard')
    setting = {}
    for line in properties.read_text(encoding='utf-8').splitlines():
        line = line.strip()
        if not line or line.startswith(('#', '!')):
            continue
        if '=' not in line:
            raise ValueError('malformed server.properties line')
        key, val = line.split('=', 1)
        setting.setdefault(key.strip(), []).append(val.strip())
    for name in ('white-list', 'enforce-whitelist'):
        if setting.get(name) != ['true']:
            raise ValueError(f'{name}=true must occur exactly once')
    if json.loads(whitelist.read_text(encoding='utf-8')) != []:
        raise ValueError('whitelist.json must be an empty JSON array')
    # Vanilla/Paper operators bypass a Java Edition whitelist even when
    # whitelist.json is empty. No OP can be present before settlement.
    if json.loads(ops.read_text(encoding='utf-8')) != []:
        raise ValueError('ops.json must be an empty JSON array (OP whitelist bypass)')
except (ValueError, OSError, UnicodeError, zipfile.BadZipFile, json.JSONDecodeError) as exc:
    print(f'START_GUARD_DENIED: {exc}', file=sys.stderr)
    sys.exit(1)
PY
then
  exit 67
fi
# Refuse a concurrent guarded launch of the same working directory.
exec {guard_lock}>"$server_dir/.simplecoins-start-guard.lock"
if ! flock -n "$guard_lock"; then
  echo 'START_GUARD_DENIED: another guarded Paper launch is active' >&2
  exit 68
fi
printf '%s\n' 'START_GUARD_PASSED: pinned SimpleCoins and Guard JARs + empty whitelist and OP list verified' >&2
cd -- "$server_dir"
exec "$@"
