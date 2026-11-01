# Phase 6: snapshot・仕訳・クラッシュ復旧の実機受入手順

**状態：未実行。** TASK-20261101-005 / PR #10の独立検証用手順であり、現在のJava実装が以下の全ケースを通過したという意味ではない。規範仕様は `forGPT/XPlayServer/OriginalPlugins/SimpleCoins/02B_データ収集・決算処理.md`、候補ベクトルは `docs/cat/phase6-*.md`。CAT ArtifactはDraft、正式process-closure/model-conformanceと生成Red/Greenは未実施。

## 実行前条件と証拠

- **本番では実施しない。** 独立したPaper 26.2検証サーバーと使い捨てワールド・playerdata・SimpleCoinsデータを用意する。故障注入はサーバー停止中または隔離されたテスト用永続化境界で行う。生存する本番の保存ファイルを直接改変しない。
- 対象commit/JAR SHA-256、Paper・関連Plugin版、各source root、対象world一覧とWorldScope、開始時のオンライン人数、保存データのチェックサムを記録する。参加拒否の開始・解除時点をログで確認する。
- 正常系ではプレイヤー0人・権威sourceの列挙・保存済みとloaded memoryの同一時点性を**別々に**検証する。0人であることやファイル一覧が前後一致することだけを世界の静止証明にしない。
- 仕訳台帳と決算snapshotは異なる永続化対象であり、予約受付を現物貨幣の発行や決算確定と混同しない。

## 故障注入マトリクス

| ID / 対応Draft | 注入時点・入力 | 合格条件（観測すべき結果） |
| --- | --- | --- |
| P6-01 / SETTLEMENT-STORE-01,02 | 完全な世代1を保存後に再起動し、世代2を保存。旧世代番号から再度commit | 残高・D所在を同じ値で復元。世代飛越・stale更新を拒否 |
| P6-02 / SETTLEMENT-STORE-03,06 | 停止中にsnapshotを切詰め、未知版・不正合計・重複UUIDのコピーを個別に注入 | 破損を空世代とみなさず、起動・公開をfail-closed |
| P6-03 / SETTLEMENT-STORE-10,11,12 | 初回初期化markerの永続化後、最初のsnapshot書込みを失敗させる。逆にsnapshotだけ残しmarkerを除去 | 再起動後に勝手に世代0へ再初期化せず、権威不明を拒否 |
| P6-04 / SETTLEMENT-GEN-01,05,07 | 完了前staging、durable callbackの書込み前失敗、commit後例外を別々に注入 | 途中値を可視化せず、commit後の結果不明はpublisherをpoisonし、再読込前の再公開を拒否 |
| P6-05 / SETTLEMENT-STORE-07,08 | 同じ世代へ2writer、保持中のwriter lock、stale expected generation | 二重commitを許さず、lock取得待ちを無期限にしない |
| P6-06 / SC-JOURNAL-ACK-04,05 | sequence 1と2を予約し、head・tail 2を正常なまま非tail `1.grant` を欠損・破損・別Grantへ置換してsequence 1をacknowledge | 対象のdurable recordを再読込し、承認記録を書かず拒否。破損ファイルを上書き復元せず、ledgerをunhealthyとして後続発行を止める |
| P6-07 / SC-JOURNAL-ACK-06 | sequence 1/2のhead・tailと非tail `1.grant`が健全で、sequence 1の保存済みreceiptが一致する | sequence 1だけがdeliveredへ遷移し、sequence 2のpendingを維持。再起動後も両者の状態が一致し、二重配布しない |
| P6-08 / source-readiness-barrier / manifest-source-closure | 対象worldのEntity source欠落、未ロードsourceの変更、走査中の他Pluginによる変更 | 未確認を0資産としない。成功した決算世代を公開せず、参加受付再開を早めない |

非tail故障注入では、**操作前の`1.grant`のbytes/ハッシュ、注入直後のbytes/ハッシュ、承認失敗後のbytes/ハッシュ**を個別に保存する。`head`とtail 2が健全なままでも古い対象のdurable内容が不正なら拒否されることを確認する。正常系はsequence 2のpendingが不変であることを再起動後にも照合する。これらは既存Draftベクトルの検証準備であり、未実行のRed/Greenを合格扱いしない。

## 実行・判定の記録

各ケースで **not-run / pass / fail / blocked** を明示し、使用commit、故障注入の正確な境界、変更前後のファイル・世代・receipt・journalのハッシュ、サーバーログ、再起動後の観測を対応付ける。復旧時の自動再試行で元の破損証拠を消さない。JVM単体試験の成功はPaperのsaveData durability、外部Pluginとの時点整合、参加制御の実機合格を意味しない。

現時点の未完了条件は、規範確認と正式CAT Gate、実際の故障注入、Paperの保存時点・loaded/disk整合、全件source網羅、最終決算公開の受入である。**この手順書自体は受入試験の実行結果ではない。**
