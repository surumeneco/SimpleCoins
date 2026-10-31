# CAT Draft: Entity UUID uniqueness across saved-asset adapters

Status: **Draft / not Confirmed**. This document records the already-adopted fail-closed accounting requirement; it is not a new normative specification or a passed machine Gate.

Source: `forGPT/XPlayServer/OriginalPlugins/SimpleCoins/02B_データ収集・決算処理.md`, `05_1.0.0_実装計画.md` (G0, Phase 5, duplicate-accounting and source-completeness checks).

## Process / PI

- Input: one consistent saved Entity chunk and an authoritative ownership decision for each value-bearing holder.
- PI-1: one physical Entity UUID in the chunk identifies at most one saved holder, regardless of whether value appears in `equipment`, `Items`, or `Inventory`.
- PI-2: an Entity containing both equipment and inventory is valid and may contribute distinct physical slots.
- PI-3: if the same UUID appears on two distinct value-bearing Entity records, abort the source before accepting either record's value. Do not publish a partial settlement.
- PI-4: ownership is never inferred from the item content; unresolved ownership fails closed.

## Test conditions / TCE

| ID | Input | Expected |
| --- | --- | --- |
| EID-01 | Two records with identical UUID; first carries a coin in equipment, second in Items | `stageChunk` fails, and `commit` fails |
| EID-02 | One record with both equipment and Items; distinct slots | Both values staged once with the same owner decision |
| EID-03 | Two distinct UUIDs with equipment and Items | Existing staging behavior preserved |
| EID-04 | One saved dropped coin and one empty item frame reuse a UUID | Combined free-Entity staging fails before counting |

## Evidence / gates

- Draft created before source-code changes.
- CAT formal process-closure, TestModel, model-conformance, vectors, Queue, implementation-conformance: **not-run** until the authorized current routing/scripts and Confirmed Artifact are available.
- Regression test must demonstrate a real Red on the feature branch before a minimal Green fix; GitHub CI and blob/branch readback are separate evidence.
- This decoder is only a Phase-5 component. No claim of coherent world snapshot, BAN/BlockLocker resolution, Paper real-server verification, or 1.0.0 release.
