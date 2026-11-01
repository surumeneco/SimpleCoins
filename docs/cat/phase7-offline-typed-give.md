# Phase 7: offline typed-coin give (CAT Draft)

Status: **Draft**, not Confirmed. Leaf `TASK-20261101-006`.

## Process / PI

An administrator with `simplecoins.admin` may reserve either an untyped amount or a specific denomination/count for an already known offline player. Both paths must use the same exact-identity lookup: online UUIDs or a uniquely matched previously seen offline player. Unknown or ambiguous names fail closed; selectors are online-only and must not fall back to an offline account. A typed grant is journaled before delivery; if the player is offline, it remains pending and is delivered on a later join. A UUID-addressed player who is currently online must instead be scheduled immediately. Wallet item issuance still requires an online player. Duplicate selector results must not reserve the same grant twice. When reading a persisted player grant receipt, a missing PDC key denotes sequence zero, but an existing key with an incompatible type or negative value is corrupt and must fail closed rather than silently default to zero.

## TCE

- OFFLINE-GIVE-01: a unique previously seen offline account receives a typed denomination reservation without online inventory mutation.
- OFFLINE-GIVE-02: online typed grant behavior remains unchanged, with one reservation per distinct UUID.
- OFFLINE-GIVE-03: unknown/ambiguous offline accounts and empty selectors are rejected without reservation.
- OFFLINE-GIVE-04: wallet item issuance remains online-only.
- OFFLINE-GIVE-05: command budget counts unique recipients and uses the same durable ledger path.
- OFFLINE-GIVE-06 (Red): an existing receipt key with a non-LONG PDC value or negative LONG is rejected before any grant delivery; only absent keys default to zero.
- OFFLINE-GIVE-07 (Red): a fatal runtime failure after a reservation record is durable but before the journal head is updated must poison the current ledger instance; the record is recovered exactly once on restart rather than silently overwritten by a later reservation.
- OFFLINE-GIVE-08 (Red): a fatal failure after a delivered/acknowledged record is durable but before the in-memory ledger is updated must poison that instance; restart recovers the delivered record without reissuing assets.
- OFFLINE-GIVE-09 (Red): an issuance journal root or ancestor that is a symbolic link must not be accepted as an authoritative durable-money store, even if its target is a readable directory.

Formal CAT machine Gates and Paper live/offline persistence tests: **not-run**.
