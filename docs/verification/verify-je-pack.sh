#!/usr/bin/env bash
# Read-only external verification of a hosted SimpleCoins Java resource pack.
set -euo pipefail

if [[ $# -ne 1 || ! -f "$1" || -L "$1" ]]; then
  printf 'Usage: %s <generated-regular-zip>\n' "$0" >&2
  exit 2
fi
archive=$1
if ! command -v curl >/dev/null || ! command -v cmp >/dev/null || \
   ! command -v sha1sum >/dev/null || ! command -v unzip >/dev/null; then
  printf 'Required verification tools are missing.\n' >&2
  exit 2
fi

printf 'Exact hosted HTTP(S) pack URL (not logged): ' >&2
IFS= read -r -s pack_url || { printf '\nURL input unavailable.\n' >&2; exit 2; }
printf '\n' >&2
if [[ -z "$pack_url" ]]; then
  printf 'No URL supplied.\n' >&2
  exit 2
fi

tmp=$(mktemp -d)
trap 'rm -rf -- "$tmp"; unset pack_url' EXIT
if ! curl --proto '=http,https' --proto-redir '=http,https' \
    --fail --silent --show-error --location --max-time 90 \
    --output "$tmp/hosted.zip" "$pack_url" 2>"$tmp/curl.err"; then
  printf 'Hosted pack download failed (URL redacted).\n' >&2
  exit 1
fi
if ! cmp -s -- "$archive" "$tmp/hosted.zip"; then
  printf 'Hosted pack differs byte-for-byte from the generated archive.\n' >&2
  exit 1
fi
if ! unzip -tq "$tmp/hosted.zip" >/dev/null; then
  printf 'Hosted bytes are not a valid ZIP archive.\n' >&2
  exit 1
fi
printf 'Verified identical bytes and ZIP integrity. SHA-1 evidence:\n'
sha1sum -- "$archive" "$tmp/hosted.zip"
