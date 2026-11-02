---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.wwc.saved-follow-target
status: draft
process: simplecoins.owner
source_refs:
  - https://drive.google.com/file/d/1sn-FXbU7nFDr6yc6YxPAe2V6U6mV2CZD/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# DomainSpec: 保存済みWWC鶏のfollow-target証拠

## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.owner | WWC commander UUIDのpositive-only証拠 |
| simplecoins.snapshot | 保存済みEntityとloaded memoryの同一時点性 |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| wwc.saved.positive | minecraft:chickenのBukkitValuesに正規marker・v2〜v4・canonical UUID follow_targetがあるとき | 既知のcommander UUIDをpositive-only evidenceとして返す | WWC-SAVED-01 |
| wwc.saved.missing | BukkitValues・marker・follow_targetが欠損、またはversionが未対応のとき | UNKNOWNとしFREEやゼロ資産と推論しない | WWC-SAVED-02 |
| wwc.saved.malformed | Entity種別が違うか、PDCが不正型、targetが不正型・非canonical UUIDのとき | UNKNOWNとする | WWC-SAVED-03 |
| wwc.saved.version | 正しいv2/v3/v4のmarkerとtargetがあるとき | 各versionで正規UUIDを認識し、通常の鶏をFREEと断定しない | WWC-SAVED-04 |
| wwc.saved.readonly | 保存済みEntityから証拠を読むとき | 入力source mapやBukkit状態を変更しない | WWC-SAVED-05 |
| wwc.saved.nbt | named-root NBT chunkにtyped BukkitValuesがあるとき | NbtReader経由でも同じpositive-only commander evidenceを得る | WWC-SAVED-06 |

## 境界と状態

既存WWC-SAVED-01〜06を意味変更せず構造化したTASK-20261101-004のCAT Draft。PDC namespaceはwhatawonderfulchicken、markerはwonderful_chicken。呼出側はEntity UUID、WWC契約、loaded/disk照合、所有権優先順位、BAN、snapshotの完全性を別途検証する。Paper/WWC状態は変更しない。正式CAT Process/PI/TCE・機械GateとPaper 26.2実機検証はnot-run。Confirmed・マージ・決算公開を意味しない。
