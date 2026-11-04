---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase5.excluded-budget-boundary
status: candidate
process: simplecoins.snapshot
source_refs:
  - https://github.com/surumeneco/SimpleCoins/blob/b88cee565e78376f0161965cc9fdf7ad3b196915/src/main/java/co/surumene/simplecoins/domain/WorldStorageDiscovery.java
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5: 除外ワールドと無関係ファイルの走査予算（code-to-spec Candidate）

## Targets

| Ref | Aspect |
| --- | --- |
| simplecoins.snapshot.discovery-budget | `maximumEntries` は有効な資産だけでなく訪問した全ファイル・ディレクトリに適用する |
| simplecoins.snapshot.excluded-world-budget | WorldScopeで除外されたワールドも走査予算から免除しない |

## Rules

| ID | Condition | Requirement | Verification |
| --- | --- | --- | --- |
| SC-BUDGET-01 | 除外ワールドの無関係ファイルが走査上限を超過する | 部分的・空のmanifestを返さずIOExceptionで停止する | excludedWorldUnrelatedFilesStillExhaustGlobalVisitBudget |
| SC-BUDGET-02 | root・directory・fileの3エントリだけが存在する | 上限3では成功し、上限2ではIOExceptionで停止する | visitBudgetCountsDirectoriesAndFilesAtExactBoundary |

## Evidence and limits

- PR #34の`Files.walkFileTree`、`charge`、`scope.includes`の現行動作を固定するcode-to-spec Candidate。新しい仕様決定やConfirmedへの昇格ではない。
- JUnitはファイル走査の境界に限定する。CAT正規Gate、Paper実機、ワールドスナップショットの時点整合は未実行。
