---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.owner.result-invariants
status: draft
process: simplecoins.owner
source_refs:
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1sn-FXbU7nFDr6yc6YxPAe2V6U6mV2CZD/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# DomainSpec: Asset owner classification result invariants (CAT Draft)

## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.owner | FREE/PERSONAL/DEADの内部整合性と所有者UUID |
| simplecoins.ban | DEADの分類理由（現在BAN/プレイヤーデータ欠損） |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| owner.result.invalid | AssetOwner.Resultにkind/UUID/DeadReasonの矛盾または欠損があるとき | public record constructorはIllegalArgumentExceptionで拒否し、矛盾する分類を外部へ公開しない | AO-01, AO-02, AO-03 |
| owner.result.valid | FREE/PERSONAL/DEADがそれぞれ正当なUUIDとDeadReasonの組を持つとき | 結果は構築可能であり既存のP/F/D分類を変更しない | AO-04 |

## Process / PI

An `AssetOwner.Result` is internally consistent: FREE has no player and no dead reason; PERSONAL has a player but no dead reason; DEAD has a player and a non-null `DeadReason`. The public record constructor must reject contradictory combinations rather than expose a classification that cannot be produced by the canonical `AssetOwner.resolve` path. This is a pure domain invariant, not proof of a complete BAN-source adapter or authoritative settlement snapshot.

## 受入ベクトル（既存テスト条件）

- AO-01: PERSONAL with CURRENT_BAN reason raises `IllegalArgumentException`.
- AO-02: DEAD without a reason raises `IllegalArgumentException`.
- AO-03: a null classification kind raises `IllegalArgumentException`.
- AO-04: valid FREE, PERSONAL and DEAD combinations remain constructible.

## Gateと適用境界

Status: **Draft, not Confirmed**. Formal CAT semantic machine Gates are **not-run**. This conversion preserves AO-01〜04 and does not add a new production requirement, external integration claim, or permission to publish settlement totals.
