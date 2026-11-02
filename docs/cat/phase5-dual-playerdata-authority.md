---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase5.dual-playerdata-authority
status: candidate
process: simplecoins.snapshot
source_refs:
  - https://github.com/surumeneco/SimpleCoins/blob/b88cee565e78376f0161965cc9fdf7ad3b196915/src/main/java/co/surumene/simplecoins/domain/WorldStorageDiscovery.java
  - https://github.com/surumeneco/SimpleCoins/blob/develop/docs/verification/phase5-storage-discovery-acceptance.md
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5: 同一ワールド内の二系統playerdata候補の権威衝突（code-to-spec Candidate）

## Targets

| Ref | Aspect |
| --- | --- |
| simplecoins.snapshot.playerdata-authority | `playerdata/` と `players/data/` の同一UUIDの候補を一意に扱う既存実装 |
| simplecoins.snapshot.playerdata-exclusion | WorldScopeで除外したワールドでもplayerdataの権威衝突を無視しない |

## Rules

| ID | Condition | Requirement | Verification |
| --- | --- | --- | --- |
| SC-DUAL-01 | 同一ワールドの `playerdata/<uuid>.dat` と `players/data/<uuid>.dat` が同じUUIDを持つ | `discover` はIOExceptionで停止し、片方を恣意的に採用しない | sameWorldLegacyAndModernCopiesForOneUuidFailClosed |
| SC-DUAL-02 | 同一ワールドの二系統に異なるUUIDが存在する | 双方の正規ファイルをmanifestに含む | independentUuidsAcrossBothLayoutsRemainDiscoverable |
| SC-DUAL-03 | WorldScopeから除外されたワールドで同一UUIDが二系統に存在する | playerdata独立性を維持しIOExceptionで停止する | excludedWorldDoesNotMaskDualLayoutConflict |
| SC-DUAL-04 | 先行する有効なworld regionがある後に同一UUIDの二系統衝突を検出 | 部分manifestを返さずIOExceptionで停止する | precedingRegionDoesNotPermitPartialManifestPublication |

## Evidence and limits

- SD-03/SD-04（`docs/verification/phase5-storage-discovery-acceptance.md`）の既存受入境界と、PR #34の`players.putIfAbsent(uuid, file)`の観測を対応づける。新しい仕様決定ではない。
- 本文はcode-to-spec CandidateでありConfirmedではない。正規CAT Process/PI/TCE machine Gates、Paper/Multiverse実機試験、保存済み/ロード済みの時点整合は未実行。
- JUnitはこの既存挙動の回帰証拠だけを提供する。PR #34の実装がdevelopに入るまでは本テストを単独でdevelopにマージしない。
