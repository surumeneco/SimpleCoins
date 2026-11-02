---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase7.bedrock-output-collision
status: candidate
process: simplecoins.bedrock-pack
source_refs:
  - https://github.com/surumeneco/SimpleCoins/blob/01a06f9f0fb78c8f941f2aa2e1c3df5e8951d153/src/main/java/co/surumene/simplecoins/domain/BedrockPack.java
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 7: Bedrock ZIPとmapping出力先の同一パス衝突（code-to-spec Candidate）

## Targets

| Ref | Aspect |
| --- | --- |
| simplecoins.bedrock-pack.outputs | ZIPとGeyser JSONの独立した出力先 |

## Rules

| ID | Condition | Requirement | Verification |
| --- | --- | --- | --- |
| SC-OUT-01 | ZIPとJSONの出力先が同じPath | 一方が他方を上書きする成功結果を許さず、出力前に失敗する | identicalPackAndMappingPathMustBeRejected |
| SC-OUT-02 | 正規化すると同じ出力先になる別表記 | 正規化前の文字列差で衝突を見逃さない | normalizedAliasOfOutputPathMustBeRejected |
| SC-OUT-03 | 異なる出力先が与えられる | ZIPとJSONがそれぞれの場所に生成される | distinctOutputPathsProduceZipAndMapping |

## Evidence and limits

- 現行`BedrockPack.write`は二つの一時ファイルを順に移動する。引数が同一パスならJSONがZIPを上書きする可能性がある。この契約は未承認のCandidateであり、実測テストにより確認する。
- 別PR #86の二ファイル原子的公開・障害時ロールバックとは独立した入力パス衝突の検証。CAT正式Gate、Paper/Geyser/JE-BE実機は未実施。
