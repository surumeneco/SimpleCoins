---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase7.bedrock-pair-publication
status: draft
process: simplecoins.bedrock-pack
source_refs:
  - https://github.com/surumeneco/SimpleCoins/blob/01a06f9f0fb78c8f941f2aa2e1c3df5e8951d153/src/main/java/co/surumene/simplecoins/domain/BedrockPack.java
  - https://github.com/surumeneco/SimpleCoins/blob/develop/docs/verification/phase7-je-be-pack-handoff.md
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 7: Bedrock packとGeyser mappingの二ファイル公開整合性（Draft）

## Targets

| Ref | Aspect |
| --- | --- |
| simplecoins.bedrock-pack.pair-publication | Pack ZIPとmapping JSONの生成/公開時の部分更新リスク |
| simplecoins.bedrock-pack.failure-visibility | 二つ目の出力に障害が起きた場合の一貫性とエラー伝播 |

## Rules

| ID | Condition | Requirement | Verification |
| --- | --- | --- | --- |
| SC-BPAIR-01 | 有効なpackが既にあり、新pack生成後にmappingの公開が失敗 | 候補要求: 旧packを保持し、片側だけを新世代にしない | mappingPublicationFailureMustNotReplacePreviousPack |
| SC-BPAIR-02 | pack側の公開が失敗 | 既存mappingを保持し、失敗を通知する | packPublicationFailureMustNotReplacePreviousMapping |
| SC-BPAIR-03 | 二出力が共に正常 | 新しいpackとmappingを生成し、対応する九額面を含む | successfulPairPublicationContainsAllDenominations |

## Evidence and open decisions

- 現行`BedrockPack.write`は一時ZIPと一時JSONを生成後、`Files.move(temporary, pack, ATOMIC_MOVE, REPLACE_EXISTING)`と`Files.move(temporaryMapping, mapping, ...)`を**別々に**実行する。各moveの原子性は二ファイル全体の原子性を意味しない。
- SC-BPAIR-01は故障注入で再現する**未承認の安全要求候補**。世代ディレクトリ・ポインタ切替等の解決設計を本Draftで確定しない。
- 失敗した時点でGeyser管理領域に実配布済みかは未検証。Paper/Geyser/Bedrock実機試験、CAT Process/PI/TCEのConfirmedと正規Gateは未実行。
