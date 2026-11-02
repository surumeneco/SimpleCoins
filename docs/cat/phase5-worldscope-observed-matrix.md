---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase5.worldscope.observed-matrix
status: candidate
process: simplecoins.world-scope
source_refs:
  - https://github.com/surumeneco/SimpleCoins/blob/20f5a26a0e6326a215170d55ef0839a7aebcf8c0/src/main/java/co/surumene/simplecoins/domain/WorldScope.java
refs: []
semantic_contract: markdown-v2
language: ja
---

# WorldScope code-to-spec Candidate

## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.world-scope | 既存包含・除外判定 |

## 規則
| ID | 条件 | 要求候補 | 検証 |
| --- | --- | --- | --- |
| WS-01 | inのみ | true | WorldScopeTest.allExplicitMembershipCombinationsFollowObservedTruthTable |
| WS-02 | outのみ | false | 同上 |
| WS-03 | inとoutが一致 | defaultIncluded | 同上 |
| WS-04 | world=null | true | 同上 |
| WS-05 | Setを変更 | snapshotは不変 | WorldScopeTest.bothConfigurationSetsRemainSnapshotsAfterCallerMutatesThem |

未決定: 外部仕様への昇格。既存実装の観測でありConfirmedではない。正規CAT GateとPaper実機検証は未実行。
