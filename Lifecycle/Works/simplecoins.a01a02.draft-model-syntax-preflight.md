---
cat_version: '0.2'
kind: work
id: simplecoins.a01a02.draft-model-syntax-preflight
status: draft
process: simplecoins.settlement.writer-epoch-publication
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1Py55wN7KsfMqIWfD9P2IwSDwXSzUBqeR/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: verification
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# A01/G0・A02/G3 — 未承認Draftの純粋なMarkdown/分岐モデル検査

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/CAT-TDD/commit/a6ff19bd0cfdb7a8b007f4df62e25f154acbe643 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.settlement.writer-epoch-publication;simplecoins.grants.paper-save-commit | simplecoins.a01a02.draft-model-syntax-preflight | simplecoins.blocklocker.saved-owner;simplecoins.external-storage-provider-integration |

## Technologies
| Technology |
| --- |
| Python 3; immutable released CAT compiler; GitHub Actions |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01a02.draft-model-syntax-preflight.md;docs/cat/drafts/a02-grant-delivery-process.md;.github/workflows/cat-a01-a02-draft-model-preflight.yml | — | .github/workflows/cat-a01-a02-draft-model-preflight.yml |

## Checks
| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |

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

## 技術上の実作業と境界

A01およびA02のProcess/PI/TCEは**現状Draft**であり、ユーザーの正当な規範承認と正式CAT Gateが未成立。独自の保証の意味を変更したり自動Confirmed化せず、最新の公開済みCATのMarkdownモデル読取と有限Boolean規則の技術的不備だけを調査する。

- A02 `docs/cat/drafts/a02-grant-delivery-process.md` の観測能力表は**三列表頭に二列の区切り**が存在していた。仕様内容は変えず`| --- | --- | --- |`に修正する。
- `CAT-TDD main@a6ff19bd0cfdb7a8b007f4df62e25f154acbe643`の真正`cat_compile_v2.py model`へ、A01とA02それぞれ**`--allow-draft --allow-partial`**を明示してソースArtifactを入力。無断で規範Confirmed・正式Model Conformanceの結果に置換しない。
- A01公開判定と失効通知の合計5規則、A02配布検査・保存後照合・クラッシュ復旧の合計24規則について、確認可能な**入力Boolean抽象空間内**で各起点の重複数0、未被覆数0、到達不能規則0、出典未解決0と機械確認する。生成したモデル本文に`status: draft`相当が残ることを必須とする。
- これらは「真である証拠」の独立収集を行っておらず、現Paperの全writer・永続ACKを保証しない。実資産観測、Global閉包、原子公開、発行仕訳、G3同一保存世代、CAT Process/Model/Implementation正式Gate、正式JUnit Green、入室許可は全て対象外。規範的に判断が必要な内容は正本Draftのまま保留し、承認者が決めるまでは非公開を維持する。
- CIでモデルとcoverage結果のSHA256をartifactとして保存する。本番製品Java、`main`、VPS、入室Guard、G0公開、G3 ACKは変更しない。

## Stop condition

正規コンパイラでA01/A02の未承認モデルを再生成し、Syntax/Boolean分割が一致するCIを取得すること。DraftをConfirmしない。実装分岐の列挙が整っても入力証拠の妥当性が不明なら、正式な実装に進まない。
