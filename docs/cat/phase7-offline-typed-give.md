# Phase 7: offline typed-coin give (CAT Draft)

Status: **Draft**, not Confirmed. Leaf `TASK-20261101-006`.

## Process / PI

An administrator with `simplecoins.admin` may reserve either an untyped amount or a specific denomination/count for an already known offline player. Both paths must use the same exact-identity lookup: online UUIDs or a uniquely matched previously seen offline player. Unknown or ambiguous names fail closed; selectors are online-only and must not fall back to an offline account. A typed grant is journaled before delivery; if the player is offline, it remains pending and is delivered on a later join. Wallet item issuance still requires an online player. Duplicate selector results must not reserve the same grant twice.

## TCE

- OFFLINE-GIVE-01: a unique previously seen offline account receives a typed denomination reservation without online inventory mutation.
- OFFLINE-GIVE-02: online typed grant behavior remains unchanged, with one reservation per distinct UUID.
- OFFLINE-GIVE-03: unknown/ambiguous offline accounts and empty selectors are rejected without reservation.
- OFFLINE-GIVE-04: wallet item issuance remains online-only.
- OFFLINE-GIVE-05: command budget counts unique recipients and uses the same durable ledger path.

Formal CAT machine Gates and Paper live/offline persistence tests: **not-run**.
