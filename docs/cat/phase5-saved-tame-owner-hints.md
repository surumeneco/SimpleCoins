---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase5.saved-tame-owner-hints
status: draft
process: simplecoins.saved-entity-evidence
source_refs:
  - https://drive.google.com/file/d/1sn-FXbU7nFDr6yc6YxPAe2V6U6mV2CZD/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5: saved tameable owner hints (CAT Draft)

## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.saved-entities | 保存済みEntitiesリストのvanilla tameable UUID/Owner NBT hint |
| simplecoins.external-owner | WWC/WWW/WCLの指揮者・所有者証拠との後段照合 |
| simplecoins.settlement | 未確認のhintから資産の帰属・FREE・BAN状態を確定しない |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| tame.owner | 対象tameableが有効なUUIDとOwnerを持つ | Entity UUIDをkey、Owner UUIDをvalueとするhintを返す | TAME-01 |
| tame.absent | tameableにOwner tagがない | mapに載せない。所有者不在・FREEの証明には使わない | TAME-02 |
| tame.nonowner | dropped item等の非tameableがOwner tagを持つ | 配達先・射手等を経済的所有者として採用しない | TAME-03 |
| tame.malformed | 対象tameableのUUID/Ownerが4-int配列でない | IOExceptionで走査を中断し、部分結果を確定しない | TAME-04 |
| tame.duplicate | 同じEntity UUIDが対象リスト内に重複する | Owner有無や値に関係なくIOExceptionで拒否する | TAME-05 |
| tame.source | Entitiesリスト自体が欠損・型不正、または要素のidが不正 | IOExceptionで拒否する | TAME-06 |
| tame.immutable | 抽出結果を呼出元が変更しようとする | 返却されたmapは不変snapshot | TAME-07 |
| tame.uuid | UUIDの4つのsigned intを復元する | 上位・下位64bitを欠落なく保持する | TAME-08 |

## Process / PI
SavedEntityOwner.extract is a **read-only hint decoder**, not an authoritative owner resolver. It inspects recognized vanilla tameable IDs in the saved chunk Entities list. An absent Owner tag means no hint, **not** FREE ownership. The caller must still validate current loaded entity state, entity/world UUID, registered account, bans, WWC/WWW commander precedence and the source snapshot before settlement. Never infer ownership for dropped items, projectiles or other non-tameables merely because they carry an Owner field.

## Test model / TCE
- TAME-01: verified vanilla tameable with four-int UUID and Owner produces a UUID-keyed hint.
- TAME-02: tameable without Owner produces no map entry.
- TAME-03: dropped item Owner is ignored, not classified as tame ownership.
- TAME-04: invalid UUID and Owner shapes fail closed.
- TAME-05: duplicate tameable UUID fails even if only one entry carries Owner.
- TAME-06: absent/invalid Entities and malformed element identifiers fail closed.
- TAME-07: output map is immutable and independent of mutable input maps.
- TAME-08: signed int components round-trip full 128-bit UUID.
- TAME-09: recognized vanilla species set is bounded; non-tameable entities are not assigned economic owners.

## State and gate
Draft only, **not Confirmed**. The existing implementation and test source are extracted without semantic alteration from PR #8 for isolated verification; the original PR is not edited. Formal CAT Process/PI/TCE confirmation, process-closure, TestModel, vectors, Queue, model-conformance, implementation-conformance and live Paper 26.2 tests are not-run until explicitly verified. Passing this decoder's unit tests does not satisfy external ownership or settlement acceptance.
