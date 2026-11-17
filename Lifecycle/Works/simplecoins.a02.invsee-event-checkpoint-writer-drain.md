---
cat_version: '0.2'
kind: work
id: simplecoins.a02.invsee-event-checkpoint-writer-drain
status: draft
process: simplecoins.grants.paper-save-commit
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/499
  - https://github.com/surumeneco/SimpleCoins/pull/500
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

# G3 — InvSee++実writerで「保存イベント許可済み」と「ファイル書込み完了」を区別

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/478 |
| https://github.com/surumeneco/SimpleCoins/pull/486 |
| https://github.com/surumeneco/SimpleCoins/pull/499 |
| https://github.com/Jannyboy11/InvSee-plus-plus/blob/722def99e663561a1e8e4cbc865003af003cdb0e/InvSee++_Platforms/Impl_Paper_26_2/src/main/java/com/janboerman/invsee/paper/impl_26_2/InvseeImpl.java |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.paper-save-commit | simplecoins.a02.invsee-event-checkpoint-writer-drain | simplecoins.a02.cooperative-drain-verify-ack;simplecoins.a02.cooperative-veto-fsync-ack |

## Technologies

| Technology |
| --- |
| Official Paper 26.2 build134; pinned InvSee++ 0.31.22; SpectatorInventorySaveEvent two priority observers; real offline UUID.dat and SimpleCoins receipt; fresh saved-file proof; CAT issue-work structural validation |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a02.invsee-event-checkpoint-writer-drain.md;tests/paper-invsee-writer-probe/src/main/java/co/surumene/simplecoins/a02invsee/InvseeWriterProbe.java;.github/workflows/paper-a02-invsee-post-entry-veto.yml | — | tests/paper-invsee-writer-probe/src/main/java/co/surumene/simplecoins/a02invsee/InvseeWriterProbe.java;.github/workflows/paper-a02-invsee-post-entry-veto.yml |

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

## Technical question

InvSee++ Paper 26.2 `InvseeImpl.save()` calls `EventHelper.callSpectatorInventorySaveEvent` synchronously, checks `event.isCancelled()`, then queues the actual `FakeCraftPlayer.loadData()/saveData()` inside `CompletableFuture.supplyAsync` via `scheduler.executeSyncPlayer`. Existing #486 registered a veto **before** this event and proved that an immediate cancellation keeps the original 5000C coin+receipt through ACK and restart. It did not establish how to account for a writer that has **already passed** the veto checkpoint. #499 tested in-flight permit drain in JUnit, not this real writer.

## 実プラグインを使用する2つの独立した境界試験

固定InvSee++ JAR、公式Paper 26.2、独立したdefault-deny guard、零人/loopback/使い捨てWorldを使用する。双方とも製品の実`GrantLedger`、実`CoinItemCodec`、`PaperGrantInventoryMaterializer`、実`CraftPlayer.saveData()`、本物のプレイヤー`UUID.dat`を使用し、先に本物の5000C貨幣/受領PDCが保存されていることを確認する。以下の2つのシナリオは**互いに独立した新規世界**で実行する。これはテストであり、本番の全writer排他実装ではない。

### 制御A：先行writerが正規貨幣を消した場合はACK拒否（既存の反例）

1. テスト用InvSee++保存listener LOWESTは**最初の保存イベントだけ許可**する。MONITORで同じイベント処理中に新規writer受付を閉鎖し、保存完了Futureはまだ未観測。これを全writer排出とはみなさない。
2. `InvseeAPI.saveInventory()`へ渡した最初のinventoryは5000C貨幣のみ除去したもの。既にイベント入場済みの保存は`SaveResponse.saved`で完了し、同じ対象プレイヤーの現物5000Cが0へ減り、完了receipt/STONEは残る。正規永続台帳は未ACK。
3. 2回目のInvSee++保存は新規ゲート閉鎖で`notSaved`、計数は許可1・拒否1。新たな実UUID.dat読戻しで現物証拠が不成立のためACKしない。入室default-deny維持。

### 制御B：先行writerが正規貨幣を保持して完了した後だけACK（追加の正例）

1. 同じLOWEST→MONITORのゲート時点で最初の保存イベントだけを許可する。ただし最初のInvSee++ inventoryは**正規5000Cと他のSTONEを変更しない**。新規拒否開始直後にACKせず、実`InvseeAPI.saveInventory()` Futureが`SaveResponse.saved`で正常完了し、playerdataの保存済み現物と認証済みreceiptが残ることを確認する。
2. 完了後もゲートを閉じたまま同じUUIDの第2回実保存を試み、`notSaved`を検証する。許可1・拒否1以外は拒否し、台帳はなお未ACK。
3. 実`UUID.dat`とその親ディレクトリの`FileChannel.force(true)`が戻ったあと、**新しいManifestと本物の`SavedGrantDeliveryFileReadback.verify`**で、現物5000C・同一root receipt・変更していないSTONE/装備等を検証する。成功して初めて既存の一件だけ`GrantLedger.acknowledge`し、同じファイルの内容不変、元予約のACK冪等性、台帳再オープン時のdelivered 1／pending 0を確認する。
4. 実JVMを`Runtime.halt(140)`で終了し、**同じ世界を新しいPaper JVMで起動**して現物5000C、署名済みreceipt、ACK済み1件、pending 0件、入室default-denyを実測する。通常のJava shutdown hookによる自動保存には依存しない。

**本試験の閉包範囲**：正例の「排出」は、テスト自身が生成・追跡する**1件のInvSee++ `CompletableFuture`**が完了したときだけをいう。実ゲームの他のInvSee++入口、外部プラグイン、Paper独自保存、非協調writer、filesystem置換、OS電源断をこの計数で全件停止できると主張しない。先行writerが実際には`SaveResponse.saved`後に遅延書込みを行わないという保証も含め、G3全writer閉包は**別途立証が必要**。検証用ゲートを製品に配線せず、正式CAT Confirmed/Gatesと入室解禁は行わない。

## Acceptance

- 固定SHA256のPaper/InvSee++実JARと本物の正規貨幣、PDC receipt、他収納STONE、実`GrantLedger`を使用し、制御A/Bを別の空のPaperワールド/JVMで検証する。
- **制御A**は遅い拒否だけでは先行writerの現物消去を防げない反例を保持。新規読戻し拒否・未ACK・後続拒否を実測する。
- **制御B**は先行writer Future完了までACKせず、新規拒否を保持したまま実保存fsync→fresh読戻し→1回ACK→`Runtime.halt(140)`→別Paper JVM cold restartで現物・receipt・台帳一件性を実測する。
- Domain/Phase6と、`tests/paper-invsee-writer-probe/**`に紐づく既存の実Paper回帰CIを**変更後の同一HEAD**で確認し、全件成功後のみ`develop`へ統合する。
- Workは`status:draft`、CAT正式Gateはnot-run、製品ソース/設定/ガード/`main`は非変更。本番配布や実VPS受入は検証外。
