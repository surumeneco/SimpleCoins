---
cat_version: '0.2'
kind: work
id: simplecoins.a04.bedrock-pack-byte-stability
status: draft
process: simplecoins.presentation.currency-pack
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://github.com/surumeneco/SimpleCoins/pull/305
  - https://github.com/surumeneco/SimpleCoins/issues/6
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: verification
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/6
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# A04: Bedrock ZIPとGeyserマッピングのバイト決定性（検証専用）

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/305 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/BedrockPack.java |
| https://github.com/surumeneco/SimpleCoins/issues/6 |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.presentation.currency-pack | simplecoins.a04.bedrock-pack-byte-stability | simplecoins.currency.pdc-stable;simplecoins.settlement.default-deny |

## Technologies

| Technology |
| --- |
| Java 25; JUnit 5; exact 9 denominations; BedrockPack.write; ZIP/JSON byte identity; SHA-derived content UUID contract; pinned CAT issue-work structural validator |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a04.bedrock-pack-byte-stability.md;src/test/java/co/surumene/simplecoins/domain/BedrockPackDeterminismMatrixTest.java;.github/workflows/a04-bedrock-pack-determinism.yml | — | src/test/java/co/surumene/simplecoins/domain/BedrockPackDeterminismMatrixTest.java;.github/workflows/a04-bedrock-pack-determinism.yml |

## Checks

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| bedrock-matrix | regression | junit | @repo:SimpleCoins | gradle test --tests co.surumene.simplecoins.domain.BedrockPackDeterminismMatrixTest | 16-variants;stable-repeat;content-change | BedrockPack;CoinDenomination | JUnit XML | 180 |

## Gates

| Gate | Status | Evidence |
| --- | --- | --- |
| process-closure | not-run | — |
| model-conformance | not-run | — |
| implementation-conformance | not-run | — |
| semantic-review | not-run | — |
| test-oracle-review | not-run | — |
| red-review | not-run | — |
| pr-review | not-run | — |
| ci | not-run | — |
| merge | not-run | — |
| deployment | not-run | — |
| real-use | not-run | — |

## Requirement and bounded observed behavior

01/03の確定仕様は9額面のID/PDC/モデル、名称と画像の任意変更、Nexoの有無に応じたJE/BEパック処理を要求する。旧[#305](https://github.com/surumeneco/SimpleCoins/pull/305)には、現`develop`に存在しないJUnitの**16バリエーションの繰返しバイト決定性試験**と旧branch固有のWorkflowが残っていた。旧branch全体は後続コードを巻き戻すため統合しない。このWorkは**今の`BedrockPack.write`**を対象にテストとCIだけを独立抽出する。

実際の`BedrockPack.write(archive, mapping, images, names)`を9額面すべてのPNGバイト長・内容、表示名（引用符・円記号・改行風エスケープ・日本語・絵文字）、16種類の決定的入力で呼び出す。同じ入力を同じ出力パスへ2回書いてZIP/JSONが**完全なバイト同値**となり、各ZIPにmanifestと9 PNGエントリがあることを確認する。16入力の間では実PNGバイトと表示名が変わるため、前のZIP/JSONと**異なるバイト列**になることも要求する（同一内容を再出力するとキャッシュ識別子が不必要に変化する／異なる内容で識別子が変化しない双方を防ぐ限定試験）。

`BedrockPack`が入力byte[]をPNGデコーダーで検証するか、Geyserが実JSONを受理するか、実Bedrockクライアントが画像を表示するかは本テストで検証できない。**テスト用画像byte[]は必ずしも有効なPNGではない。** 以前の`#305`はJDK25で成功したが、今回の受入は**現`develop`起点の新しいHEAD**でのJUnit XMLと既存Domain・Phase6 CIで判断する。手書きJUnitをCAT生成suiteや正式Red/Green Gateに読み替えない。

## Acceptance

- 旧PRの2独立パスを現`develop`へ無差別マージしない。今の製品`BedrockPack`に対し16入力×再出力のZIP/JSON同一バイトと、異なる入力間の非同一バイトをJUnitで確認する。
- パック出力先・モデル・PDC・配布/入室ガードは変更しない。実JE/BEクライアント、Nexo/外部サービス、実VPS受入は別検証。
- 新規専用CIで固定CAT Work構造、JUnit XML、Domain/Phase6の同一HEAD成功を確認してから`develop`へ統合し、旧[#305](https://github.com/surumeneco/SimpleCoins/pull/305)には引継ぎ先と意図的未マージをコメント・closeする。
- CATの本来の仕様が`Confirmed`ではないため、このWorkは`Draft`のまま、正式Process Closure/Model/implementation Gatesはnot-run。CI GreenをCAT承認やJE/BE表示の保証にしない。
