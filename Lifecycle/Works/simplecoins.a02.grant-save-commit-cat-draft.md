---
cat_version: '0.2'
kind: work
id: simplecoins.a02.grant-save-commit-cat-draft
status: draft
process: simplecoins.grants.paper-save-commit
source_refs:
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
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

# A02 — 予約配布の保存・受領・ACK境界のCAT規範候補

> **現行規範の参照**：2026-11-23の利用者によるCAT承認判断委任後、A01/A02の正式Process/PI/TCEは `docs/cat/confirmed/` を正本とする。本WorkのDraft・当時の未承認記述は旧技術検証の履歴であり、最新規範の未承認を意味しない。全writer排他と製品G0/G3は引続き未成立。

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/434 |
| https://github.com/surumeneco/SimpleCoins/pull/437 |
| https://github.com/surumeneco/SimpleCoins/pull/468 |
| https://github.com/surumeneco/SimpleCoins/pull/473 |
| https://github.com/surumeneco/SimpleCoins/pull/510 |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.paper-save-commit | simplecoins.grants.pi.delivery;simplecoins.grants.tce.delivery | simplecoins.a02.paper-save-commit-verification;simplecoins.grants.saved-file-readback |

## Technologies

| Technology |
| --- |
| CAT-TDD markdown-v2; Paper 26.2 playerdata; GrantLedger; PDC receipt; persistent physical money |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a02.grant-save-commit-cat-draft.md;docs/cat/confirmed/a02-grant-delivery-process.md;docs/cat/confirmed/a02-grant-delivery-pi.md;docs/cat/confirmed/a02-grant-delivery-tce.md;tests/a02_grant_draft_partition.py;.github/workflows/a02-grant-delivery-cat-draft.yml;.github/workflows/cat-a02-grant-draft-lint.yml | — | tests/a02_grant_draft_partition.py;.github/workflows/a02-grant-delivery-cat-draft.yml;.github/workflows/cat-a02-grant-draft-lint.yml |

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

## Existing evidence and normative limits

- Approved product 03/02B requires UUID+reservation identity and restart-stable pending delivery until the exact physical coins and PDC receipt are persisted as one playerdata generation; receipt-only or ledger-only is never an actual issue.
- [#434](https://github.com/surumeneco/SimpleCoins/pull/434) demonstrated genuine Paper 26.2 `CraftPlayer.saveData()/loadData` and silent non-durable `saveData()` return under disabled writing/IO fault; verified persisted item and PDC before ACK and post-SIGKILL acknowledgment recovery on another JVM. [#437](https://github.com/surumeneco/SimpleCoins/pull/437) added full-inventory refusal and preservation of unrelated existing stored items.
- `GrantLedger.acknowledge(grant,seq)` is a lower-level API trusting a caller-supplied sequence; a standalone Java filesystem probe succeeded at ACK without any real playerdata. This is a caller proof obligation; **no live issuance service connected in SimpleCoinsPlugin has been shown to exploit it**.
- Draft Process/PI/TCE in `docs/cat/confirmed/a02-grant-delivery-*` specify candidate proof-before-ACK, no delivery on failed save, owner of recovery, and explicit uncertainty. They **cannot self-promote to Confirmed**. Full inventory, logout, race, OS power-loss and external Plugin mutation may need refined state abstraction; TCE remains `網羅要求: 部分`.
- [#467](https://github.com/surumeneco/SimpleCoins/pull/467)/[#468](https://github.com/surumeneco/SimpleCoins/pull/468) cover partial-save/final-save-before-ACK crash boundaries; [#473](https://github.com/surumeneco/SimpleCoins/pull/473) adds **real Paper pre-save SIGKILL** and the ninth/tenth independent JVM recovery. Existing durable ACK replay must not cause a new physical delivery; however an acknowledged coin can legitimately leave its original slot later.
- Existing `cat-a02-grant-draft-lint.yml` had an exact eight-case whitelist and fails on valid new fail-closed cases. Preserve its original eight regression scenarios as a mandatory subset, while permitting additive cases; the new independent exhaustive partition test rejects overlaps, gaps and weak ACK conditions. This change is to verification compatibility, not to formal CAT logic or approval.
- A deterministic stdlib-only Draft partition check enumerates all 128 assignments of the seven PI Boolean inputs across prepare, verify and recover, requiring a unique case per origin and forbidding every ACK without saved physical proof, complete receipt, stable generation, other-inventory preservation and authentic pending reservation. It checks that contradicted delivered states block without reopening an already-ACKed reservation. **This is independent issue-work verification, not CAT model-conformance or a proof that external observables have been classified correctly.**
- New Draft cases isolate **no-reservation yet contradictory receipt**, **already-ACKed reservation** (idempotent no-op, not new issuance), and **post-proof writer gap** (a finite after-save hash witness is not an atomic verify-to-ACK barrier). Inapplicable/incomplete observation must not be guessed as a safe false contradiction flag.
- Legitimate normative decision must select the guarantee boundary: Paper JVM crash (SIGKILL) vs filesystem/OS power loss, stable physical source evidence, concurrent login/logout and recovery ownership. After that, run upstream process closure/model-conformance/TDD/conformance before any product connection; remain default-deny until exact evidence.

## G3 writer排他について補強された技術証拠（本Workは引続きDraft）

- [#499](https://github.com/surumeneco/SimpleCoins/pull/499)：協調writer先行入場permitをCountDownLatchで保持し、`closeAndDrain`で新規入場拒否、**既入場writer全排出後**に真の圧縮playerdata（正規貨幣/receipt+STONE）・ファイル/親ディレクトリ強制同期・現行`SavedGrantDeliveryFileReadback`・`GrantLedger`ACK/再起動読み戻し相当の実台帳再オープンが成功。限定JUnitでありPaper saveDataではない。
- [#500](https://github.com/surumeneco/SimpleCoins/pull/500)：実配布InvSee++ 0.31.22・公式Paper 26.2で`SpectatorInventorySaveEvent`を初回許可したあと新規拒否を開始しても、**初回に入場した保存が後から完了**して元の5000C貨幣を消失。completed receiptと未ACK台帳は残存、fresh readback拒否。後続保存は拒否される。新規拒否のみをG3の全writer fenceと扱えない。
- 正式実装前に、全対象writerの明示的な入場・排出・排他保持を証明し、異常・タイムアウト・非協調writerを検出したらACK不可で停止する必要がある。既存のPaperの無人時pauseも同期タスク・実貨幣変更・独立ファイルwriterを止めないことを[#497](https://github.com/surumeneco/SimpleCoins/pull/497)/[#498](https://github.com/surumeneco/SimpleCoins/pull/498)が実証している。
- [#502](https://github.com/surumeneco/SimpleCoins/pull/502)：実InvSee++の**既に入場済みの特定1保存Future**を待ってから、同じUUID.datの現物/receiptをsync/readbackし、ACK一回→JVM強制終了→別Paper JVMで現物5000C・receipt・delivered1/pending0を復元した。全writerドレインではなく既知Futureの正例に限定。
- [#505](https://github.com/surumeneco/SimpleCoins/pull/505)：**InvSee++保存イベント拒否を実証した状態**から、同じ`CraftPlayer`へPaper標準`Player.saveData()`を直接行い、拒否イベントを発火せず正規5000Cを保存済み0に上書き可能。真正完了receiptと未ACK予約が残存し、fresh readbackが拒否。file/dir fsync→`Runtime.halt(141)`→別Paper JVMの復元まで実測。従って**InvSee固有イベントの一括拒否案はPaper native writerの共通排他手段にならない**。
- [#510](https://github.com/surumeneco/SimpleCoins/pull/510)：実InvSee++の既知Futureを排出し次回InvSee保存を拒否、file/dir fsyncと正しいfresh保存証拠を取得した**後・永続ACK前**に、別入口のPaper `CraftPlayer.saveData()`で現物5000Cのみを除去。freshな再検査は不一致を拒否したが、古いsequenceによる**意図的な低レベル`GrantLedger.acknowledge`誤使用**ではdelivered1/pending0が永続化し、別実Paper JVMで現物0とともに復元された。#502の限定正例を全writer排他へ一般化できない。未接続の製品coordinatorがこの誤ACKをしたという主張ではない。**次に実装する共同writer境界にはPaper native保存の入場/停止/排出・読戻し後からACK完了までの保持が必須**であり、実証不能ならACK不可・default-denyを維持。TCE既存24分岐と`Draft`は維持し、PIの証拠入力への写像を補強する。
- 保証範囲を狭める無断対応は認めない。実装候補は`Player.saveData()`、InvSee非同期保存、Paper/NMS/他プラグインwriterを**同じ入場・排出・ACKまでの排他境界で捕捉できること**を別途実証する必要がある。現行DraftをConfirmedにせず、未成立時のACK不可・入室拒否を維持。プラグイン内部で決算を行う既存02B仕様を、外部バッチへ無断変更しない。
- 正規の`Confirmed`昇格・Process Closure/Model/Implementation Gate、実Paper製品coordinator接続、全writer閉包、OS電源断耐久性は**未実施**。両技術検証のGreenをG3の製品完成と扱わない。
