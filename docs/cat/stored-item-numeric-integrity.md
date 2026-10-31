# CAT Draft: reject non-NBT numeric representations in stored items

Status: **Draft / not Confirmed**. Source: SimpleCoins 02B and 05 Phase 5/G0 fail-closed saved-NBT decoding requirements. This is an implementation-level integrity check, not a change to game rules.

## Process / PI

- Read one stored ItemStack from the bounded NBT decoder.
- PI-1: a present `count` must be a finite integer NBT scalar (Byte, Short, Integer, or Long), within the existing supported stack range.
- PI-2: a malformed numeric representation must fail the source; coercing arbitrary `Number.longValue()` can overflow and undercount currency.
- PI-3: an omitted modern `count` still means one; valid NBT numeric counts preserve current behavior.

## TCE

| ID | Input | Expected |
| --- | --- | --- |
| NUM-01 | coin with `count = BigInteger(2^64 + 1)` | IOException, not a one-coin value |
| NUM-02 | coin with `count = 1` as Integer | one coin value |
| NUM-03 | omitted `count` | one coin value |

Formal CAT process-closure, TestModel, vectors, Queue and implementation-conformance are **not-run**; no Confirmed promotion is implied. Red/Green must be independently verified in feature-branch CI.
