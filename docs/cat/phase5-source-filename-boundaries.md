---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase5.source-filename-boundaries
status: candidate
process: simplecoins.snapshot
source_refs:
  - https://github.com/surumeneco/SimpleCoins/blob/b88cee565e78376f0161965cc9fdf7ad3b196915/src/main/java/co/surumene/simplecoins/domain/WorldStorageDiscovery.java
  - https://github.com/surumeneco/SimpleCoins/blob/develop/docs/verification/phase5-storage-discovery-acceptance.md
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5: 保存済みソースの正規ファイル名境界（code-to-spec Candidate）

## Targets

| Ref | Aspect |
| --- | --- |
| simplecoins.snapshot.region-coordinate | Anvil region filenameの符号付き32bit座標と正規表記 |
| simplecoins.snapshot.playerdata-uuid | playerdata UUIDの正規表記と無関係なバックアップファイルの除外 |

## Rules

| ID | Condition | Requirement | Verification |
| --- | --- | --- | --- |
| SC-NAME-01 | 正規の最小/最大int座標を持つr.X.Z.mcaがある | region/entityのmanifestにそれぞれ1件含める | extremeSignedIntRegionCoordinatesAreAccepted |
| SC-NAME-02 | int範囲外の座標を持つ.mcaがある | IOExceptionで停止し、先に検出した有効ファイルの部分manifestを返さない | outOfRangeRegionCoordinatesFailClosed |
| SC-NAME-03 | 追加の座標区切りまたは空の座標を持つ.mcaがある | IOExceptionで停止する | malformedRegionCoordinateShapeFailsClosed |
| SC-NAME-04 | アルファベットを含む正規小文字UUID.datがある | playerdata候補としてそのUUIDを返す | canonicalLowercaseAlphabeticUuidIsAccepted |
| SC-NAME-05 | 大文字のUUID.datまたは非UUIDの.datがplayerdata直下にある | IOExceptionで停止し、未知の所有者をゼロと解釈しない | noncanonicalOrInvalidPlayerdataNamesFailClosed |
| SC-NAME-06 | バックアップ直下に不正.mcaや.dat.bakがある | 非権威のファイルとして無視し、正規ソースのみ返す | unrelatedBackupNamesDoNotBecomeAuthoritativeSources |

## Evidence and limits

- 観測対象はPR #34の既存実装（固定Git SHA）であり、新しい規範の確定ではない。コードの現在の振る舞いを仕様の正しさと同一視しない。
- 本文はcode-to-spec CandidateであってConfirmedではない。正式なProcess/PI/TCEの意味承認、CAT machine Gates、Paper実機検証は未実行。
- JUnitは保存元候補のファイル名検証のみを対象とする。MCA/NBT内容の正当性、スナップショット整合性、ロード済み資産の重複排除は対象外。
