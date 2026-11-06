# Phase 7 運営照会・give・オフライン予約：受入検証契約

## 目的と権威境界

TaskQueue `TASK-20261101-006` の運営機能について、実装・実機検証の受入観測を定義する。**本書はテスト計画であり、機能の実装済み・CAT Gate合格・Paper実機合格を意味しない。**

確定仕様の出典：
- [03_運営設定・コマンド](https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view) — giveの2形式、権限、既知オフライン対象、予約と受領
- [05_1.0.0_実装計画](https://drive.google.com/file/d/1fSsHEn1tFX_DvnzxDK3t5FjDrIuvMrnt/view) — O01、G3、Phase 7の受入と統合条件
- [02B_データ収集・決算処理](https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view) — 死蔵資産の所在照会と現在状態による再分類

正式なコマンド名、権限ノード名、ページング構文は未確定。以下のテストでは、実装が定めた名前を対象HEADから記録し、ここで新たな仕様として固定しない。

## 受入マトリクス

| ID | 検証対象・操作 | 期待する観測 | 区分 |
| --- | --- | --- | --- |
| AG-01 | OP、非OP、明示権限を付与した非OPでgiveと管理照会を実行 | 既定はOP、権限なしは拒否、権限付与時のみ許可。拒否操作で現物・予約・統計が変化しない | Paper/LuckPerms |
| AG-02 | 金額5200をgive | 5000×1＋100×2の3枚。総価値5200で、不要な小額面への分解をしない | 自動＋Paper |
| AG-03 | 64枚超を必要とする額面・個数のオンラインgive | スタック上限内に分割。負数・0・整数範囲外・不正アイテム名を拒否し、部分発行しない | 自動＋Paper |
| AG-04 | `@p`/`@a`/`@s` 等のオンラインセレクタで複数対象を指定 | 対象ごとに指定額／個数を全量渡す。対象集合を誤って共有・重複計上しない | Paper |
| AG-05 | 既知オフライン名・UUID、未知名、曖昧な識別を指定 | 金額形式だけ既知UUIDを一意確定して予約。未知・曖昧は拒否。アイテム名＋個数形式はオフライン不可 | 自動＋Paper |
| AG-06 | オフライン金額予約を受付後、対象ログイン前に統計を照会 | 予約ID・対象UUID・金額・発行者等を永続記録し、未配布分を現物発行・世界総額Tへ計上しない | 永続化 |
| AG-07 | 予約受付後にプラグイン／サーバーを再起動 | 未配布予約が同一IDで残り、二重受付・予約喪失を起こさない | 故障注入 |
| AG-08 | 対象のインベントリが満杯または一部のみ空いた状態でログイン | 配布可能な分だけ確定し、未配布残額は予約に残す。空きが戻った後の再試行で約束額に収束する | Paper |
| AG-09 | 引渡し直後の切断、playerdata保存、台帳完了記録の順序を変えて停止 | 受領連番／累計配布額と現物保存が対応し、再ログインで二重配布・未配布消失を起こさない | 故障注入 |
| AG-10 | 受付台帳破損・受領記録巻戻り・保存失敗を注入 | 異常を検出して安全停止／未確定扱いにし、推測で再発行しない | 故障注入 |
| AG-11 | 管理者がP/F/D、任意UUIDの保有額、0額登録者を照会 | 権限内の対象だけ表示。死蔵Dは通常総額T=P＋Fや平均等に混入しない | Paper＋決算 |
| AG-12 | BAN・BAN解除・playerdata欠損状態を変えて死蔵資産の所在を照会 | 現時点の状態に基づく理由・UUID・金額・保管種別・ワールド/座標または論理位置を表示。BANイベント履歴のみで固定しない | Paper＋決算 |

## 実行・証拠の取り方

1. 本番ワールドへ故障注入しない。隔離したPaper 26.2テストサーバーで、対象Git SHA・JAR SHA・Java版・LuckPerms/Nexo/Geyserの導入状態・テストUUID・設定を固定する。
2. 金額形式／アイテム形式／オンラインセレクタ／既知オフライン名を分け、各操作前後の現物枚数・財布価値・予約台帳・受領連番・統計値を照合する。コマンド成功メッセージだけでは配布成功としない。
3. AG-07〜10は受付前・受付直後・部分引渡し・playerdata保存前後・台帳完了前後に停止点を設け、再起動後の**現物＋残予約＝約束額**と一回性を確認する。保存保証が不明な停止点は `inconclusive` とし、exactly-once成功としない。
4. 各IDに `passed / failed / not-run / inconclusive`、対象SHA、操作手順、観測ログ・台帳／PDCの差分を付ける。直接テスト可能な項目は先に実行し、CIは必要な環境差・ワークフロー検証に限定する。
5. AG-11〜12はPhase 5の所有者判定・全件走査・Phase 6の整合スナップショットが成立して初めて総合受入可能。未完成の外部アダプターや決算結果をゼロとして成功にしない。

### PR #2の保存済み受領照合境界（ソース観測、実機未検証）

[PR #2](https://github.com/surumeneco/SimpleCoins/pull/2) の`OfflineGrants.java`（Git blob `e7ed1299eecefac6bb8e94bb1e8b3a1dfa580c26`）では、予約の現物化後に`player.saveData()`を呼び、`verifySavedReceipt`で保存済みplayerdataとメモリ上の受領情報を照合する。これは**コード上の意図・分岐**であり、クラッシュ時の原子性や実機での保存成功を証明しない。

| 照合点 | ソースで確認した動作 | 隔離Paperで必要な検証 |
| --- | --- | --- |
| 保存先の一意性 | 全ワールドの`players/data`・`playerdata`から対象UUIDの`.dat`を探索し、`toRealPath()`で実体を集約。実体がちょうど1件でなければ`IOException` | 保存先0件、実体の異なる2件、同一実体への別パスを区別し、正しいworld/playerdataのみで成功するか確認 |
| PDC受領の一致 | GZIP NBTの`BukkitValues`から`grant_completed`・`grant_active`・`grant_paid`を`Long`として読み、メモリ上の3値と比較 | キー欠損、型不一致、保存前後の値不一致を個別に注入し、受領済みと誤認しないか確認 |
| 失敗時の制御 | `verifySavedReceipt`の例外は`deliver`から`fail`へ渡り、発行を停止して`Bukkit.shutdown()`を要求 | 保存失敗後の再起動・台帳再読込・未配布額を照合し、停止要求だけでexactly-once成立と見なさない |

この受入観点はAG-07〜10の既存ケースを具体化するもので、別の確定仕様や新しいタスクを発行しない。実際の保存パス・Bukkit API動作・プラグインの再起動復旧は**not-run**のままとする。

### AG-09 障害注入の判定表（実機未実行）

以下は**隔離Paperで実施する観測手順**であり、台帳とplayerdataの原子性を仮定しない。各停止点で同一予約ID・対象UUID・受領連番・予約残額・保存済みinventoryの枚数と額面を再起動前後で採取する。メモリ上のinventoryだけを永続化証拠にしない。

| 停止点 | 再起動後に必要な観測 | 合格条件 |
| --- | --- | --- |
| 受付永続化前 | 台帳に予約なし、現物増加なし | 予約がなければ発行しない |
| 受付永続化後・現物変更前 | 同一予約IDと未配布額、現物増加なし | 再試行可能で二重予約しない |
| メモリinventory変更後・playerdata保存前 | ディスク上の現物・受領記録・台帳を再読込 | 保存未確定を配布済みと推測しない |
| playerdata保存後・台帳完了前 | 保存済み現物と受領連番、未完了台帳 | 二重現物化せず、矛盾時は安全停止 |
| 台帳完了後・切断前 | 完了台帳と保存済み受領の一致 | 再ログイン後に再発行しない |
| 部分配布後・再接続前 | 保存済み配布額と残予約の対応 | 配布済み＋残予約＝約束額を維持 |

**判定**：各停止点で保存済み現物と台帳の整合を確認できた場合のみそのケースをpassとする。保存順序・fsync保証・停止注入の到達点を特定できない場合はinconclusive。二重発行、予約喪失、未確定状態からの推測再発行はfail。未実装の停止注入フックや隔離環境不足はnot-runとし、既存の純Java参照モデル成功を代入しない。故障注入は本番データで実施しない。

## 直接検証済みの部分と受入未達の境界

| 証拠 | 実測した範囲 | 本書の受入への効力 |
| --- | --- | --- |
| `develop`純Java金額・スタック境界（`CoinMoney` blob `b24011d3512d3531afe34d7c7512515df6ac0f2c`、`CoinDenomination` `f6d8a15fab50c242886f00d35e902289342ea2ef`、`IssuanceBudget` `607b004cfab076143e2add6dd8f470a622abb0c1`） | 2026-11-03に3ソースの`git hash-object`を照合し、Java 21 `javac --release 21`で実コンパイル・直接実行。5200=5000×1+100×2、総価値、64/65枚のスタック境界、複数対象の全体1024スタック上限、0/負数、非対応額面、BigInteger集計の**9/9 assertions成功** | AG-02/03の**純Java計算**に限る。実コマンド、権限、現物配布、予約、Paper実機は未実施 |
| [PR #125](https://github.com/surumeneco/SimpleCoins/pull/125)、HEAD `ca0e09032e071904c31ed46dca1f382511b00d4a`、[Actions #41499593007](https://github.com/surumeneco/SimpleCoins/actions/runs/41499593007) | `develop`から独立したDraftに既存`GrantLedger`とJUnitテストを同一blobで抽出し、当該HEADのGradleテストがSUCCESS。CATはCandidateのまま | 永続台帳の純Java単体テスト証拠。AG-06〜10のPaper/playerdata/クラッシュ後一回性を証明しない |
| [PR #126](https://github.com/surumeneco/SimpleCoins/pull/126)、最終HEAD `56acb329ccced8e735df0c5716e9c7e899c95aeb`、[Actions #41513802057](https://github.com/surumeneco/SimpleCoins/actions/runs/41513802057) | PR #125上のCAT CandidateとJUnit。Java 21のNIO直接probeに加え、一時workflow付きコミット `b73ccd5d0e720b8285d21bba1e91162a42eb87d8` でGradle/JUnitの期待Redを実行。workflow削除後の最終HEADではテストblobは不変 | シンボリックリンク経由のroot外`head`書込みを示す未修正の期待Red。Actionsの総合結果はFAILUREであり、CAT Gate/Green/AG-07〜10の合格ではない |
| [PR #131](https://github.com/surumeneco/SimpleCoins/pull/131)、最終HEAD `90338a340c4f6dfddfe170b84dd867559cdfff17`、[Actions #41536363845](https://github.com/surumeneco/SimpleCoins/actions/runs/41536363845) | PR #125上のCAT Draftと3件のJUnit。`GrantLedger`のSCG2チェックサム付き台帳で額面500×枚数`Long.MAX_VALUE`を再読込すると、想定`IOException`ではなく未捕捉`ArithmeticException`が発生することを実行確認。ActionsではCAT Draft構文Lint成功、Gradle/JUnitは2件Green・1件期待Red（総合FAILURE）。最終HEADは一時workflow削除後でテストblob不変 | 破損台帳の例外分類に関する未確定CAT候補。正式Gate/Green、再起動時一回性、AG-10の統合受入は未実施 |
| [PR #140](https://github.com/surumeneco/SimpleCoins/pull/140)、HEAD `aa3d86ce9408af875f1e476a91ab9379e6a5ccd2`、[PR #141](https://github.com/surumeneco/SimpleCoins/pull/141)、最終HEAD `85fb12881be5dc5258e7df92bcf79613117115f6`、[Actions #41548121596](https://github.com/surumeneco/SimpleCoins/actions/runs/41548121596) | PR #125上の既存`GrantLedger`で、typed枚数overflow、0金額、空白issuerの拒否後に`.grant`が生成されず、後続正常予約がsequence 1で再開・再読込できることをJDK25 Gradle/JUnitで実測。初回CAT構文Lintの`Rules/規則`欠落はPR #141で修正し、同一実ソースのJUnitと固定CAT-TDD版のDraft構文Lintが両方SUCCESS。CI用一時workflowは削除済み | 入力拒否時の純Java台帳非永続化に限る。例外型の規範確定、正式CAT意味論Gate、Paperでの受領・再起動・AG-06〜10の統合受入を証明しない |

上記は**ソース・単体テストの証拠**であり、受入マトリクスのPaper実機・故障注入・権限試験のステータスとは別管理する。現行`develop`には上記の未統合Draft PRの変更が含まれていない。既存のJE/BE配布証拠と隔離実機手順は[JE/BE/Nexo受入手順](phase7-je-be-nexo-acceptance.md)および[配布引渡し](phase7-je-be-pack-handoff.md)を正本とし、本書へ重複複製しない。

## 未実証と引渡し

AG-01〜12の**統合受入**はすべて `not-run`。実装された独立機能は規範CAT Gate、対応テストと安全なPR統合条件を満たしてから `develop` に段階反映する。実機確認用JARと再現手順は、該当機能が揃った後に対象SHAで別途発行する。`main` と本番は利用者のリリース承認まで変更しない。
