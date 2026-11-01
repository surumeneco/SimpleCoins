# Phase 5: Canonical wallet UUID identity (CAT Draft)

Status: Draft, not Confirmed. Sources: SimpleCoins 01 wallet UUID contract, 02B nested wallet valuation, 05_1.0.0_実装計画.md (A02/A03, G0).

## Process
Every SimpleCoins wallet is minted with a canonical lowercase UUID string. A saved bundle carrying `simplecoins:wallet_id` and a balance is authoritative currency only if the wallet ID is exactly the canonical UUID spelling; Java UUID.fromString accepts abbreviated/case aliases and must not silently normalize them. The runtime wallet recognizer and saved NBT valuer must agree. Malformed tagged wallets fail closed in settlement, rather than becoming free zero or valid money.

## Test model
- Red: a saved wallet with balance 100 C and wallet_id `1-1-1-1-1` or `not-a-uuid` must raise IOException.
- Existing Green: canonical wallet UUIDs retain their balance; non-wallet bundles with nested coins remain valid.
- Runtime recognizer must reject noncanonical wallet IDs rather than allowing wallet menu operations.

Formal CAT machine gates: not-run; Chat plugin/Skill unavailable. Draft is not Confirmed.
