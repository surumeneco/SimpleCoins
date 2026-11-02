---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase7.bedrock-localization-identity
status: candidate
process: simplecoins.pack-distribution
source_refs:
  - https://github.com/surumeneco/SimpleCoins/blob/01a06f9f0fb78c8f941f2aa2e1c3df5e8951d153/src/main/java/co/surumene/simplecoins/domain/BedrockPack.java
  - https://github.com/surumeneco/SimpleCoins/blob/01a06f9f0fb78c8f941f2aa2e1c3df5e8951d153/src/test/java/co/surumene/simplecoins/domain/BedrockPackTest.java
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 7: Bedrockパックのローカライズと内容アドレスID（code-to-spec Candidate）

## Targets

| Ref | Aspect |
| --- | --- |
| simplecoins.bedrock.pack-fingerprint | 画像と表示名の内容変更をクライアントキャッシュIDに反映する既存実装 |
| simplecoins.bedrock.input-validation | 必須額面の欠落時に既存配布物を置換しない |

## Rules

| ID | Condition | Requirement | Verification |
| --- | --- | --- | --- |
| BE-LOC-01 | 画像・表示名が同一で入力Mapの列挙順だけが異なる | ZIPとmapping JSONのバイト列を一致させる | inputMapIterationOrderDoesNotChangePackOrMapping |
| BE-LOC-02 | PNGが同じで一額面の表示名だけが変わる | manifestの内容IDを変更し、mappingに新表示名を反映する | changedLocalizedNameInvalidatesPackIdentity |
| BE-LOC-03 | 必須額面の表示名が欠落している | IllegalArgumentExceptionで停止し、既存のpackとmappingを維持する | missingLocalizedNameCannotReplacePublishedPair |
| BE-LOC-04 | 表示名に制御文字・引用符・バックスラッシュがある | JSONの文字列境界をエスケープし、制御文字を生で書かない | localizedDisplayNamesAreJsonEscaped |

## Evidence and limits

- PR #2の既存BedrockPack実装からのcode-to-spec Candidateであり、新しい規範決定やConfirmedへの昇格ではない。
- 単一ファイルごとの決定性と入力検証を対象とする。ZIP/mappingの二ファイル原子的公開、Geyser/Bedrockクライアント受理、Paper実機配布を保証しない（別Draft PR #86が公開境界を扱う）。
- CAT Process/PI/TCEの意味承認、machine Gates、JE/BE実機確認は未実施。上流PR #2と独立にdevelopへ統合しない。
