# Phase 5: Canonical saved player inventory slot domain (CAT Draft)

Status: Draft, not Confirmed. Sources: SimpleCoins 02B (playerdata authoritative storage), 05_1.0.0_実装計画.md, G0.

## Process
For the vanilla saved `Inventory` list, physical slot keys must denote a real player inventory slot: 0..35 (main inventory/hotbar), 100..103 (legacy armor), or -106 (legacy offhand). Do not accept arbitrary byte values such as -1 or 99 as valid currency-bearing storage. `EnderItems` remains 0..26; modern `equipment` is accounted separately and duplicate physical representations must still fail closed.

## Test model
- Red: saved coin with `Slot:99` in `Inventory` must fail instead of counting 100 C.
- Regression: slots 35, 100, and -106 remain valid; malformed slot values do not become zero balances.

Formal CAT machine gates: not-run (Chat plugin/Skill unavailable); Draft is not Confirmed.

## Numeric identity integrity (Draft)
A saved slot or selected-hotbar index must be represented by an integral NBT numeric tag (Byte, Short, Integer, or Long). Arbitrary `Number` implementations such as a huge BigInteger must not be narrowed with `longValue()` or `intValue()`, because overflow can alias a valid slot and bypass duplicate checks. The same rule applies to `SelectedItemSlot` when reconciling modern `equipment.mainhand`.
