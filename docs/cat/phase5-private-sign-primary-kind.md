---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.blocklocker.private-primary
status: draft
process: simplecoins.blocklocker
source_refs:
  - https://drive.google.com/file/d/1sn-FXbU7nFDr6yc6YxPAe2V6U6mV2CZD/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# DomainSpec: BlockLocker PRIVATE sign primary identity

## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.blocklocker | PRIVATE signのprimary identityと集約後所有権の区別 |
| simplecoins.settlement | 未確定所有権からの公開禁止 |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| private.primary.domain | PRIVATE signのprimary identityを検証するとき | primaryは検証済みPERSONAL(owner UUID)またはUNKNOWNのみ。FREEはprimaryの有効値ではない | SC-BLOCKLOCKER-PRIMARY-02 |
| private.primary.lookup | identity lookup adapterがPRIVATE signにFREEを返したとき | 集約前にUNKNOWNへ正規化し、FREEを証明したと扱わない | SC-BLOCKLOCKER-PRIMARY-01 |
| private.everyone.authority | 物理的にattachedなEveryone markerが存在するとき | independently verified PERSONAL primaryがある場合にのみ集約結果をFREEにできる | SC-BLOCKLOCKER-PRIMARY-01, SC-BLOCKLOCKER-PRIMARY-03 |
| private.personal | independently verified PERSONAL primaryがあり、Everyone markerがないとき | 集約結果はPERSONALのまま維持する | SC-BLOCKLOCKER-PRIMARY-03 |
| private.unknown | primary lookupが欠損・利用不能・不明のとき | 集約結果はUNKNOWNまたはfail-closed。ゼロ資産・FREE・公開可能な決算とみなさない | SC-BLOCKLOCKER-PRIMARY-04 |

## 境界と状態

これは既存TASK-20261101-004の要求に対するCAT **Draft**。実装済みの局所guardは、BlockLocker 1.14.1の実機API、保存済みとliveの全看板の一貫性、UUID解決権限、Paper snapshotの完全性を証明しない。TCE/PIの正式な規範確認・機械Gateはnot-runであり、このDomainSpecをそれらの代替にしない。既存のRed/GreenとCIはソースの実行証拠であり、規範的Confirmed昇格ではない。
