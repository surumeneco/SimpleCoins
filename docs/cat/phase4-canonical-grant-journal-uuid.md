# Phase 4/5: Canonical issuance journal UUID identity (CAT Draft)

Status: Draft, not Confirmed. Sources: SimpleCoins 01/05 1.0.0 requirements, durable issuance journal, G0 fail-closed identity.

## Process
The checksummed issuance journal is authoritative monetary state. A record must use the exact lowercase canonical 36-character UUID string for both grant ID and recipient player ID. Java UUID.fromString accepts abbreviated aliases and mixed case; decoding must not normalize such aliases silently. Reject a valid-checksum journal record with noncanonical identity rather than reassigning its economic meaning. Existing canonical v1 and v2 records remain readable.

## Test model
- Red: a checksummed v2 grant with abbreviated UUID `1-1-1-1-1` must fail replay.
- Regression: canonical grant IDs and player UUIDs still replay across restarts; invalid journal never appears as an empty ledger.

Formal CAT machine gates: not-run (Chat plugin/Skill unavailable). Draft is not Confirmed.
