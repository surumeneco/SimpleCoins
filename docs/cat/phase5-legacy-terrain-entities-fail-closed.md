---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.snapshot.legacy-terrain-entities
status: draft
process: simplecoins.snapshot
source_refs:
  - https://drive.google.com/file/d/173sQAGgr2JuL36ixCPtJ23daGfoOYNYQ/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# DomainSpec: legacy terrain内Entity資産のfail-closed境界

## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.snapshot | terrain Kind.REGIONのroot/Level Entities |
| simplecoins.entity | 専用entity-regionのUUID一意性と資産抽出 |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| legacy.entity.root | terrain Kind.REGIONのroot Entitiesに非空Listがあるとき | IOExceptionで拒否しvisitor呼出し0件 | SC-LEGACY-ENT-01 |
| legacy.entity.level | terrain Kind.REGIONのLevel.Entitiesに非空Listがあるとき | IOExceptionで拒否しvisitor呼出し0件 | SC-LEGACY-ENT-02 |
| legacy.entity.malformed | terrainのEntitiesが非Listのとき | IOExceptionで拒否しvisitor呼出し0件 | SC-LEGACY-ENT-03 |
| legacy.entity.empty | terrainのroot/Level両Entitiesが空Listまたは欠損のとき | 通常の走査を許可する | SC-LEGACY-ENT-04 |
| legacy.entity.dedicated | 専用entity-region fileのEntitiesを扱うとき | terrainのlegacy guardを適用せずSavedEntityUuidIntegrityを維持する | SC-LEGACY-ENT-05 |
| legacy.entity.scope | legacy terrain内のEntity資産の完全なowner/asset adapterが未整備のとき | 未読取資産をゼロとせず、全件決算を公開しない | SC-LEGACY-ENT-01, SC-LEGACY-ENT-02 |

## 境界と状態

既存SC-LEGACY-ENT-01〜05を意味変更せず構造化したTASK-20261101-003のCAT Draft。非空legacy Entitiesはvisitorに渡す前に拒否する。完全なlegacy decoder、全権威source、loaded/disk整合、決算公開は別の受入条件。正式CAT Process/PI/TCE・機械Gateはnot-run。Confirmed・マージ・デプロイを意味しない。
