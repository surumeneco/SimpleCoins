# Phase 4: Bounded durable monetary journal reads (CAT Draft)

Status: Draft, not Confirmed. Sources: SimpleCoins 1.0.0 issuance journal and G0 fail-closed persistence requirements.

## Process
A journal file is a checksummed authoritative monetary record. `Files.size` before `Files.readAllBytes` is not a complete input budget when a file can change during reading. Open with no-follow semantics, bound the actual stream to the configured payload limit plus checksum size, and reject any size mismatch. An oversized, truncated, changed, or symbolic journal must not be replayed as a valid grant. Canonical stable records remain readable.

## Test model
- A bounded stream of exactly N bytes succeeds.
- N+1 bytes raises IOException without unbounded allocation.
- Existing journal checksum, crash recovery, and symlink rejection remain green.
- The limit does not establish a global filesystem snapshot; external single-writer coordination remains necessary.

Formal CAT machine gates: not-run; dedicated Chat plugin/Skill unavailable. Draft is not Confirmed.
