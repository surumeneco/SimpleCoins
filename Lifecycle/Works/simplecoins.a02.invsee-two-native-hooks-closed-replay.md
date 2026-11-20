---
cat_version: '0.2'
kind: work
id: simplecoins.a02.invsee-two-native-hooks-closed-replay
status: draft
process: simplecoins.grants.paper-save-commit
source_refs:
  - https://drive.google.com/file/d/1Py55wN7KsfMqIWfD9P2IwSDwXSzUBqeR/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
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

# A02/G3 — 実InvSee++非同期保存と閉鎖済みPaper native2入口の交差検証

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/580 |
| https://github.com/surumeneco/SimpleCoins/pull/581 |
| https://github.com/surumeneco/SimpleCoins/pull/500 |
| https://github.com/surumeneco/SimpleCoins/pull/510 |
| https://github.com/surumeneco/SimpleCoins/issues/423 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.paper-save-commit | simplecoins.a02.invsee-two-native-hooks-closed-replay | simplecoins.grants.paper-save-commit;simplecoins.settlement.writer-epoch-publication |

## Technologies
| Technology |
| --- |
| Java25 Class-File API and test-only javaagent; official Paper26.2 build134; pinned real InvSee++ 0.31.22; real UUID.dat and signed receipt |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a02.invsee-two-native-hooks-closed-replay.md;tests/paper-invsee-writer-probe/**;.github/workflows/paper-a02-invsee-both-native-hooks-closed.yml | — | tests/paper-invsee-writer-probe/**;.github/workflows/paper-a02-invsee-both-native-hooks-closed.yml |

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

## 実行条件・判定・反証（規範ではなく隔離技術検証）

既存PR #580/#581で、公式Paper 26.2の `CraftPlayer.saveData()V` と `PlayerDataStorage.save(Player)` の2つの入口は、Java25 Class-File APIとbootstrapに公開したテスト専用フックで拒否できた。一方、これを他プラグインのwriterまで自動的に網羅したものと見なせないため、導入済みInvSee++ 0.31.22を使って独立検証する。

1. 独立Paper＋実InvSee++をloopback・0人で起動し、真正の5000C紙貨幣、完了署名receipt、無関係STONE、未ACKの製品`GrantLedger`予約1件を同一playerdata`UUID.dat`へ保存し、現行のfresh readbackで検証する。
2. 実InvSeeのoffline inventoryを**フック解放中に読み込み完了**させ、同インベントリの5000Cだけを除去する。保存イベントをcancelしない実観測者で入場回数を数える。両方のPaper保存入口のAgent変換マーカーを要求し、その後で`a02.nativeAgent.closed=true`にして実`api.saveInventory`を呼ぶ。
3. **初回実験で確定した実経路**：[PR #582 初回CI #44772192101](https://github.com/surumeneco/SimpleCoins/actions/runs/44772192101)は、実InvSeeの保存が`CraftPlayer.saveData()`へ到達し、そこで`NativeSaveHook.beforeNativeSave`に拒否された。JVMは失敗安全停止し、元の「両入口を迂回して貨幣を消す」という仮説は**不成立**。実InvSee経路について正規の否定的反例が得られたので、テストをこの**観測済み経路**に一致する正例へ修正した。単純な別writer迂回の成功を捏造してはならない。
4. 修正した検証の要求：保存要求開始前の閉鎖、**取消していないInvSee保存イベント1件**、実`CraftPlayer`拒否例外と`isSaved=false`、NMSフック自体の変換正常（呼出数を要求しない）、保存済み5000C＋receipt＋STONE・pending1・ACK0、fresh readback、file/dir強制同期、`Runtime.halt(148)`、Agentなしの独立Paper JVM再起動でも正規貨幣・receipt・pending1が残ること。InvSeeイベントが観測されない、例外が別の原因、元のファイルが変わった場合は合格しない。
5. 既存のInvSee非協調writer・イベントvetoだけの反例、Paper native保存2入口の正例、既知Future後のfalse ACK反例を維持する。Domain/Phase6/実Paper回帰が成功して初めて本Workを統合対象とする。

**境界**：この正例は**実InvSeeの特定の保存API一経路**がCraftPlayerフックを経由するという証拠にすぎない。全InvSeeオーバーライド、別の非同期保存・先行処理、直接ファイル書込み、他23 JAR、NMS残入口、ワールドLIVE/保存チャンク・全G0 writer、配布最終ACKまでの排出と原子性は依然未成立。追加のフックを運用VPSへ導入せず、製品JAR・入室・G0公開・ACK・mainを変更しない。CAT A01/A02のDraftはそのまま、正式Process/Model/Implementation Gateは別であり、ホストのWork構文検証を合格と同一視しない。

**次の判断**：同一保存への実到達経路の列挙、非同期先行writerの排出、世代保持、全必要ソースの同時凍結が閉じられない場合は、単なる2入口の正例を製品ACK認証へ昇格させず、具体的な障害と必要な実サーバー構成変更をエスカレーションする。