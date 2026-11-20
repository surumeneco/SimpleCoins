---
cat_version: '0.2'
kind: work
id: simplecoins.a01.writer-epoch-publication-contract
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
work_kind: specification
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# A01/G0 — 全writer世代から原子公開するための規範境界

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/pull/471 |
| https://github.com/surumeneco/SimpleCoins/pull/510 |
| https://github.com/surumeneco/SimpleCoins/pull/559 |
| https://github.com/surumeneco/SimpleCoins/pull/560 |
| https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.settlement.writer-epoch-publication | simplecoins.settlement.writer-epoch-publication;simplecoins.settlement.writer-epoch-publication.pi.authorize;simplecoins.settlement.writer-epoch-publication.tce.authorize | simplecoins.world-source-discovery;simplecoins.external-storage-provider-integration;simplecoins.grants.paper-save-commit |

## Technologies
| Technology |
| --- |
| Java25; Paper26.2; fixed CAT-TDD; real Paper/Paper-native and 24-plugin writer evidence; JUnit after approval |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.writer-epoch-publication-contract.md;docs/cat/drafts/a01-writer-epoch-publication-*.md | — | — |

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

## 非重複の技術作業と停止条件

既存PaperSavedSourceDiscovery.observedはManifest/world-setの局所証拠だけで、PaperCapturedHybridSettlementは未公開LIVE/SAVED候補、SettlementSnapshotPublisherは呼出元の全writer静止を証明しない。SimpleCoinsPluginはAdmissionBarrierをBLOCKEDのまま保持する。これらを改修前にG0公開判定の状態空間を区別し、今回のDraftでは製品コードを変更しない。

#470/#471はディスクhash不変でもLIVE貨幣が変わる反例。#505/#510はInvSee専用拒否を通らないPaper-native保存とACK前の書換え反例。#559はWorld登録ABAだけの検出、#560はBlockLocker LIVE一個の正本分類。静的24-JAR roster、0人、World登録の不変、再readbackは全writer静止を証明しない。

次の実装段階では、全正本writerの新規入場を拒否し、既入場を排出し、全World/SAVED/LIVE・外部収納・playerdata・BAN・Journalの候補読取と原子公開完了まで保護世代を維持する方式を、実導入コードとPaperで検証する。全writer排他を確保できない場合は必須入力をfalseとして候補全体を棄却し、入室拒否を続ける。正当な公開後の通常仕訳には監視空白を設けず、変更受付を再開できることも必要。

このWorkはG0のCAT Process/PI/TCEをDraftとして明示し、人間の意味承認・正式CAT Gate後にのみ実装へ進む。仕様保証を変更せず技術方式を評価する限りユーザーの新規仕様判断を先取りしない。24 JAR全writer閉包が実装不能と実証された場合だけ、残る入口と保証変更／サーバ構成変更／非公開維持を整理してエスカレーション。main、VPS、配布、Admissionに触れない。
