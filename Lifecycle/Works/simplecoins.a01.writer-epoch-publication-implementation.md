---
cat_version: '0.2'
kind: work
id: simplecoins.a01.writer-epoch-publication-implementation
status: draft
process: simplecoins.settlement.writer-epoch-publication
source_refs:
  - https://github.com/surumeneco/SimpleCoins/issues/423#issuecomment-7082513010
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
decision_ref: user-chat:2026-11-23:delegated-A01-A02-CAT-semantic-approval
refs: []
entry: confirmed-spec
mode: implementation
flow: spec-implementation
work_kind: implementation
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: confirmed
---

# A01 — Confirmed PI/TCEを未公開の製品判定として実装

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423#issuecomment-7082513010 |
| https://github.com/surumeneco/SimpleCoins/issues/423 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.settlement.writer-epoch-publication | simplecoins.a01.writer-epoch-publication-implementation | simplecoins.settlement;simplecoins.grants.paper-save-commit |

## Technologies
| Technology |
| --- |
| Java 25; JUnit 5; Paper 26.2 |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.writer-epoch-publication-implementation.md;docs/cat/confirmed/a01-writer-epoch-publication-process.md;docs/cat/confirmed/a01-writer-epoch-publication-pi.md;docs/cat/confirmed/a01-writer-epoch-publication-tce.md;src/main/java/co/surumene/simplecoins/domain/SettlementWriterEpochDecision.java;src/test/java/co/surumene/simplecoins/domain/A01WriterEpochDecisionDriver.java;tests/cat/a01-writer-epoch-junit-binding.json;.github/workflows/cat-a01-a02-contract-acceptance.yml | src/main/java/co/surumene/simplecoins/domain/SettlementWriterEpochDecision.java | src/test/java/co/surumene/simplecoins/domain/A01WriterEpochDecisionDriver.java;tests/cat/a01-writer-epoch-junit-binding.json;.github/workflows/cat-a01-a02-contract-acceptance.yml |

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

## Authorized meaning and bounded implementation
利用者による承認判断委任（user-chat:2026-11-23:delegated-A01-A02-CAT-semantic-approval）に基づきDrive02A/02B/03/04/05へ照合したConfirmed A01 Process/PI/TCEを正本とする。
最初の実装単位は既に真正な証拠であると上位が立証した抽象Boolean PI入力の分類だけとする。製品の全writer入場拒否/既入場排出/世代継続、物理保存/fsync、通常Journal引継、原子公開、永続ACK、Admissionはこの局所分類では立証されない。
入力のBooleanを任意の呼出元が組み立てるだけで実際のpublish/ackへ接続してはならない。具体的な技術手段の導入形態は別の証拠単位で確定し、証拠不足時は未公開・未ACK・入室拒否を維持する。A02のequipment空/欠損の正規化候補はこの承認に含まない。

## Compilation
| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-process.md | @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-pi.md | @repo:SimpleCoins/docs/cat/confirmed/a01-writer-epoch-publication-tce.md | — | — | — | @work/.cat-flow/cache/a01-writer-epoch-model.json | @work/.cat-flow/cache/a01-writer-epoch-vectors.json | @work/.cat-flow/cache/a01-writer-epoch-queue.json | @repo:SimpleCoins/tests/cat/a01-writer-epoch-junit-binding.json | @work/.cat-flow/generated/A01WriterEpochGeneratedTest.java | @work/.cat-flow/cache/a01-writer-epoch-obligations.json | — | false |
