---
cat_version: '0.2'
kind: work
id: simplecoins.a02.cooperative-drain-verify-ack
status: draft
process: simplecoins.grants.paper-save-commit
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/486
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

# A02/G3 — 協調writerの進行中排出→本物の保存済Grant読戻し→永続ACKの一続きの境界

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/pull/475 |
| https://github.com/surumeneco/SimpleCoins/pull/485 |
| https://github.com/surumeneco/SimpleCoins/pull/486 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/GrantLedger.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/SavedGrantDeliveryFileReadback.java |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.paper-save-commit | simplecoins.a02.cooperative-drain-verify-ack | simplecoins.a02.cooperative-veto-fsync-ack;simplecoins.a02.verify-to-ack-writer-gap |

## Technologies

| Technology |
| --- |
| Java 25; JUnit 5; actual compressed NBT bytes; real production GrantLedger/GrantReceiptState/PaperGrantInventorySlotPlan/SavedGrantDeliveryFileReadback; Java thread and CountDownLatch; FileChannel.force(true); pinned CAT issue-work validator |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a02.cooperative-drain-verify-ack.md;src/test/java/co/surumene/simplecoins/paper/CooperativeWriterDrainReadbackTest.java;.github/workflows/a02-cooperative-drain-verify-ack.yml | — | src/test/java/co/surumene/simplecoins/paper/CooperativeWriterDrainReadbackTest.java;.github/workflows/a02-cooperative-drain-verify-ack.yml |

## Checks

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| a02.cooperative-drain.junit | tests | junit | @repo:SimpleCoins | gradle --no-daemon :test --tests co.surumene.simplecoins.paper.CooperativeWriterDrainReadbackTest | pendingWriterMustDrainBeforeAuthoritativeReadbackAck;multipleInFlightMustDrainAndReopenOnlyAfterBarrierRelease | src/test/java/co/surumene/simplecoins/paper/CooperativeWriterDrainReadbackTest.java | build/test-results/test/TEST-co.surumene.simplecoins.paper.CooperativeWriterDrainReadbackTest.xml | 300 |

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

## 採用済み要求と追加の技術的疑問

Drive 02B/03とIssue #423は、独立台帳のACKを正本プレイヤーデータの現物貨幣・完了receiptを**実保存後**にのみ行い、保存世代をACK確定まで保護することを要求している。[#475](https://github.com/surumeneco/SimpleCoins/pull/475)では証明後に無通知writerが現物貨幣を消してもACKできる反例を実証。[#486](https://github.com/surumeneco/SimpleCoins/pull/486)では協調InvSee++のイベント拒否→ファイル/ディレクトリfsync→読戻し→ACK→クラッシュ再起動の特定経路を確認。ただしイベント拒否を**新規に開始**したとき、既に書込み区間へ入っていたwriterが完了したことまで示す証拠はない。

このWorkは協調writerという**限定した環境モデル**で、writer入場許可・進行中writer数・新規受付遮断・最後の既存writerの排出を一つの技術試験に実装し、その後に本物の圧縮playerdata+receiptを保存し元の実`GrantLedger`で一度だけACKする。成功でも「全外部writerが協調する」とは主張しない。

## 検証実施と受入

1. テスト用writerが許可を取得し、非同期スレッドで書込み完了前に停止。Main側がcloseを要求しても、進行中許可が残る間はfence完了Futureが終了しない。事後に新たなwriterの取得は不可であり、台帳は未ACKを維持。
2. 先行writerが本物の旧圧縮playerdataを書き終えて許可を返却したあとにのみ、close Futureが完了する。Main側が同じ保存先へ実CoinItemCodec互換の5000C/PDC itemと`GrantReceiptState`を書き込み、実ファイルとディレクトリにforceを要求し、実`WorldStorageDiscovery`→`SavedGrantDeliveryFileReadback.verify`を再取得。元のGrantLedgerの同一予約をACKする。
3. closeが保持される全期間、協調writerの新規取得は拒否。ACK後に台帳をcold-reopenして1件だけdeliveredかつ未配布0、同じsaved NBTに5000C+receiptと無関係STONEが残っていることを確認。2回目ACKは冪等で実貨幣も増えない。
4. closeを解除すれば新規協調writerが再び許可を取得できることを確認。ただし**ACK後の正規貨幣移動は許される**。旧発行スロットを永久固定して再起動監査する方針ではない。
5. テスト本体の同期原語と物理保存／台帳は本番サービスに配線しない。メインスレッドをブロックしてwriter排出を待つ設計と混同しない。既存の実InvSee++イベント・非協調NMS/ファイル書込み・Paper保存スレッド・OS電源断・全world writerの世代保証は**未検証/未保護**。

受入はGreenな2件の独立JUnit＋root Domain/Phase6 CIであり、正規CATのWork構造だけを検査する。正式Process/Model/TDD/Implementation Gateはnot-run、CAT Draftは未Confirmed、入室・公開許可を得ない。
