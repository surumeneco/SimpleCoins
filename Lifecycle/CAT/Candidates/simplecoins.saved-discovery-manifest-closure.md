---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.saved-discovery-manifest-closure
status: candidate
process: simplecoins.settlement.saved-source-coverage
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/issues/3
  - https://github.com/surumeneco/SimpleCoins/pull/321
refs: []
semantic_contract: markdown-v2
language: ja
---

# 保存資産のManifest発見集合を走査完了時に再検証するCandidate

## 対象

| Ref | Aspect |
| --- | --- |
| discovery | 実ワールドフォルダ群・WorldScope・有界探索予算から生成されたManifest |
| authoritative-inputs | 集計対象region/entitiesのMCAとワールド非依存のplayerdata |
| stage-result | 元Manifestの全ファイルを走査した直後の未公開決算候補 |

## 観測

`WorldSnapshotTraversal`は既知ソースのfileKey/サイズ/mtime/SHA-256を走査前後に検証する。しかし、元Manifestに存在しない新しいMCAやUUID.datが処理途中に現れた場合、既知ソースのfingerprintは変わらないため検知できない。発見集合そのものの不変性がないと、未読取資産がないと判断できない。

## 規則

| ID | Condition | Requirement | Verification |
| --- | --- | --- | --- |
| closure-01 | Discover was performed from known world roots/scope/budget | Preserve those immutable discovery inputs with the Manifest | provenance retained |
| closure-02 | No authoritative source was added or removed | Re-discovery of all roots yields exactly the original world file and player UUID/path sets | unchanged set succeeds |
| closure-03 | A new included-region MCA or entity-region MCA appears during callbacks | Reject the candidate instead of ignoring it | late MCA injection |
| closure-04 | New valid offline playerdata appears during callbacks | Reject regardless of WorldScope exclusions | late playerdata injection |
| closure-05 | A new source is unreadable/symbolic or discovery exceeds the original budget | Fail closed, do not regard the file as zero assets | discovery safety |
| closure-06 | A caller built a Manifest manually with no trusted discovery roots | Do not invent proof of directory closure; retain the existing read-only candidate limit | manually constructed fixture remains explicitly unproven |
| closure-07 | Discovery set is unchanged | Does not prove intermediate create/remove ABA, Paper memory synchronization, external owner/BAN, journal integrity or admission readiness | no publication claim |

## Boundary

Re-discovery is bounded by the original discovery budget. Only manifests produced by `WorldStorageDiscovery.discover` can carry immutable source-closure provenance. This check supplements, rather than replaces, the existing content hash validation. It does not open the admission gate. The source roots are not inferred from `worldFiles`, since empty source directories must also be monitored.

この文書はCAT Candidateであり、AIによるConfirmed昇格・公式Gate通過を意味しない。
