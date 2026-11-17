---
cat_version: '0.2'
kind: work
id: simplecoins.grants.startup-persistent-verification
status: draft
process: simplecoins.grants.startup-guarded-evidence
source_refs:
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: implementation
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/6
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# Work: simplecoins.grants.startup-persistent-verification

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/GrantLedger.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/PaperSavedSourceDiscovery.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/.github/workflows/paper-guarded-bootstrap-smoke.yml |

## Scope

Multiple IDs in one cell are separated by `;`.

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.startup-guarded-evidence | simplecoins.grants.startup-persistent-verification | simplecoins.grants.existing-journal-contract |

## Technologies

| Technology |
| --- |
| Paper 26.2; Java 25; JUnit 5 |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.grants.startup-persistent-verification.md;src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java;.github/workflows/paper-guarded-bootstrap-smoke.yml | src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java | .github/workflows/paper-guarded-bootstrap-smoke.yml |

## Checks

`Command` uses shell-like quoting only to express argv; execution never uses a shell. List cells use `;`.

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| paper | regression | junit | @repo:SimpleCoins | gradle test | GrantLedgerTest | plugin JAR; Paper 26.2; grant head | GitHub JUnit and Paper startup logs | 180 |

## Gates

External/semantic gates are reported evidence. `passed` or `not-applicable` requires a concrete evidence/rationale reference.

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

## 現行実装と受入境界（旧「未実装」記述の修正）

- **起動時台帳の検査は現行製品コードへ接続済み**。現`develop@a7846d1399925967582f65676dc2d38cca6c7a58`の`SimpleCoinsPlugin.onEnable()`は、`NormalMutationJournal.verifyAll()`の後、Paperイベント・レシピ・Serviceの登録前に`new GrantLedger(getDataFolder().toPath().resolve("grants"))`を実行する。元の台帳がある場合はconstructorで再生・検証され、IO例外は`Cannot open durable issuance grant journal`として起動失敗経路に流れ、AdmissionBarrierを`fatalFailure`にした上でPaper停止を要求する。正常時のログは`SIMPLECOINS_ISSUANCE_JOURNAL_OPEN_VERIFIED`。**同処理を二重に実装し直す必要はない。**
- **保存済み受領監査も接続済み**。`SimpleCoinsPlugin`は検証済み`issuanceGrants`を`PaperSavedSourceDiscovery`へ渡す。同クラスはworld/playerdataの保存ソースを列挙し、`SavedGrantReceiptUniverseAudit.scan(manifest, issuanceLedger,...)`を非同期実行する。ACK済みなのに対応playerdataが欠損／ロールバックしている等の監査異常は`SIMPLECOINS_SAVED_DISCOVERY_FAILED`から主スレッド安全停止へ送られる。ここで得るものは**観測結果だけ**であり、現物配布も本番統計公開も入室許可も行わない。
- **実Paper CIの具体的な受入**：既存`.github/workflows/paper-guarded-bootstrap-smoke.yml`は、公式Paper 26.2 build134と独立した入室ガードで起動し、`plugins/SimpleCoins/grants/head`の生成および成功ログ、`normal-mutations/head`破損時の停止、**正常な通常仕訳台帳を残したまま発行台帳だけを破損させた場合の停止**、さらに**永続ACK済みだがplayerdataがない場合の停止**を別ケースで要求する。コードを読んだだけでCI実行済みとはしない。本Workの現行受入は、同じPR HEADの専用Paper CI Green・差分読み戻しを証拠とする。
- 元Workが指摘した「`onEnable()`は`NormalMutationJournal`のみを開き、`GrantLedger`を開かない」という記述は**現行ソースと矛盾し、既に解消済みの旧ギャップ**。本回次では起動処理を重複実装せず、実装状態・残る保証を正しく整理する。これまでの変更やWork本文が`Confirmed`・正式CAT Gateを得たことは意味しない。
- **残作業は別境界**：予約時の権限・コマンド、現物の配布・PDC receiptの同一保存世代、全writer排出から永続ACKまでの排他、完全なG0世界決算、通常仕訳の全経路、実プレイヤー参加、JE/BEクライアント受入、OS電源断耐久性はこの限定起動時検査では未保証。部分実装をもって1.0.0完成・入室開放と扱わない。
- **CAT**：本Workは構造上`Draft`のままで、Process Closure、Model/Implementation Conformance、正式な`Confirmed`への昇格は実施していない。CIの成功をこれらの代用としない。
