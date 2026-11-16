---
cat_version: '0.2'
kind: work
id: simplecoins.a02.paper-real-plan-save-readback
status: draft
process: simplecoins.grants.paper-save-commit
source_refs:
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/450
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

# A02/G3 — 実Paper保存の前後で既存Grant部品を接続するreadback実験

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/450 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/PaperGrantInventorySlotPlan.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/PaperGrantInventoryMaterializer.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/SavedGrantDeliveryFileReadback.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/GrantLedger.java |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.paper-save-commit | simplecoins.a02.paper-real-plan-save-readback | simplecoins.a02.after-ack-crash-replay-verification;simplecoins.a02.paper-save-commit-verification |

## Technologies

| Technology |
| --- |
| Official Paper 26.2 build 134; Java 25; isolated detached NMS CraftPlayer; real UUID playerdata + inventory + receipt PDC; loaded GrantLedger and verified discovered file manifest |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a02.paper-real-plan-save-readback.md;tests/paper-grant-save-probe/src/main/java/co/surumene/simplecoins/a02probe/PaperSaveProbe.java;.github/workflows/paper-grant-save-commit-probe.yml | — | tests/paper-grant-save-probe/src/main/java/co/surumene/simplecoins/a02probe/PaperSaveProbe.java;.github/workflows/paper-grant-save-commit-probe.yml |

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
| ci | passed | [real Paper #43851067302](https://github.com/surumeneco/SimpleCoins/actions/runs/43851067302); [Domain #43851067233](https://github.com/surumeneco/SimpleCoins/actions/runs/43851067233); [Phase6 #43851067212](https://github.com/surumeneco/SimpleCoins/actions/runs/43851067212) (tested feature HEAD `a3b0c242`, ordinary CI only; final HEAD recheck required) |
| merge | not-run | — |
| deployment | not-run | — |
| real-use | not-run | — |

## Purpose and bounded acceptance

- The adopted 03 grant contract requires saved inventory and completed/partial grant receipt in one authoritative playerdata generation; **only then** can the reserved issuance be ACKed. Failed saves and full storage must not advance the ledger. G0 and cross-plugin world snapshot remain separate.
- Existing Paper #450 demonstrates manual test-only coin+receipt writes, SIGKILL before/after ACK, and three-process replay, but does NOT actually call the available **production** `PaperGrantInventorySlotPlan.stage`, `PaperGrantInventoryMaterializer.buildCopy` and `SavedGrantDeliveryFileReadback.verify` in one physical-save path.
- Run the earlier 3-phase crash test unchanged. In an **additional fourth independent Paper process**, load that already-saved real 5000 coin, original unrelated stone stack and durable ACK1; confirm the prior state and save file. Reserve exactly one new 5000 monetary grant for the same UUID, stage from real Paper inventory and receipt, make a detached storage candidate, re-check no drift, write both inventory and receipt on the real isolated CraftPlayer, call actual `Player.saveData()`, read the same UUID file through discovered shared `players/data` and verify with production saved-file transition proof. Require all previous occupied storage and money identities unchanged, the new actual coin matching the next plan and receipt, and a fully completed proof before journal ACK2. A re-opened ledger must show exactly two delivered grants and zero pending without reissue.
- The negative control invokes the independent readback **before the second physical save**, when the staged receipt does not yet exist on disk, and requires an exception. Assert the ledger is still pending. This makes the test meaningful even if a helper is accidentally bypassed.
- This is verification-only `issue-work`: product delivery service **is not installed or enabled**. No online Player, no live/server world money, no command, no opening admission, no journal policy change; the test's new reserved grant is isolated inside its own CI world. Passing a local readback does not establish disk fsync, OS power-loss durability, post-read concurrent writer exclusion, full exactly-once for a connected player, or CAT Confirmed/Gates. Those need a properly authorized implementation Work and additional fault tests.

## 実Paper 26.2で確定した観測（保存前後の受領証明）

- 初回probe HEAD `87ca1e283bf8f0a19ed39175d9179e3e5627ebac` [CI #43850409401](https://github.com/surumeneco/SimpleCoins/actions/runs/43850409401)では**既存の2回目起動で** `SIMPLECOINS_SAVED_DISCOVERY_FAILED: Saved grant receipt disagrees with durable issuance evidence` としてfail-closedした。原因は旧試験が独立 `a02-test-grant-ledger` だけを更新し、実製品 `plugins/SimpleCoins/grants` は予約ゼロのまま、同じPaper playerdataに受領連番1が存在したこと。これは新規配布処理の失敗ではなく、試験と製品の2つの異なる正本が衝突した確かな誤構成。
- 試験のみ修正し、`SimpleCoinsPlugin` が実際に起動時オープンした `issuanceGrants` と同じ`GrantLedger` を利用する。反射参照は隔離テストプラグイン内だけで、製品APIや権限は変更しない。再起動後の台帳再開は、別の `new GrantLedger(plugins/SimpleCoins/grants)` で独立に検査する。起動時の非同期 saved-source audit が終わる時間を明示してから予約を開始する。試験用別台帳はもう使用しない。
- 検証HEAD `a3b0c242ec840d39e5b84b0de572e32533fdc602` の [実Paper #43851067302](https://github.com/surumeneco/SimpleCoins/actions/runs/43851067302)、同HEADのDomain #43851067233、Phase6 #43851067212 は全success。以前の1～3プロセスSIGKILL前後の試験は全て通過。
- 第4独立Paperプロセスでは正規旧額面5000貨幣x1とSTONE x2/別スロットを実Player.loadDataで復元し、起動時監査と一致した実台帳に2番目の正規5000貨幣x1を予約。実 `PaperGrantInventorySlotPlan.stage` で空きslot1とcompleted receipt2を計画する。**貨幣未保存時**に `SavedGrantDeliveryFileReadback.verify` を呼ぶと拒否し、ledger pending=1（`PAPER_A02_UNSAVED_PLAN_FILE_READBACK_REJECTED_PASSED`）。
- 続いて `PaperGrantInventoryMaterializer.buildCopy` が全36枠+受領PDC変化なしを再検査し、Player inventoryとPDCを同一の実 `CraftPlayer.saveData()` で保存。Paperの共有 `players/data/<UUID>.dat` を再発見し、実圧縮playerdataの保存前→保存後変化を `SavedGrantDeliveryFileReadback.verify` で読戻し、旧貨幣・STONEを保全、追加5000貨幣1枚、`receipt=2` の一致を確認。ログ: `PAPER_A02_REAL_PLAN_MATERIALIZER_SAVE_FILE_PROOF_PASSED amount=5000 retainedPriorCoin=true retainedOtherStorage=true savedReceipt=2`。
- **この実ファイル証明に成功した後だけ** `ledger.acknowledge(...,2)` を呼び、独立台帳再起動読取で2件ともdelivered=true・pending=0を検査。ログ: `PAPER_A02_VERIFIED_FILE_PROOF_BEFORE_ACK_PASSED durableGrants=2 pending=0`、`PAPER_A02_REAL_PLAN_SAVE_PROOF_ACK_REAL_PAPER_PASSED`。
- 本検証はテストプラグインが既存のproduction部品を順序どおり呼ぶ**実JVM検証**であり、製品に物理配布コーディネーターや接続者対象ログイン処理を追加していない。現実のログイン/切断、同時writer、OS電源断/fsync、最終的なG3 exactly-onceとG0は未完了。CAT正式Process/PI/TCEはDraftであり正式Gateはnot-run。
