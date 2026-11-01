# Phase 5: BlockLocker・BAN・companion所有権のPaper実機受入

**状態：未実行。** TASK-20261101-004 / PR #8の統合受入手順。既存 `docs/cat/phase5-external-owner-ban-evidence.md` 等のCAT Draftと、`forGPT/XPlayServer/OriginalPlugins/SimpleCoins/02A_所有者・統計定義.md`・`02B_データ収集・決算処理.md` を参照する。Draftの構文検査やpure domain JUnit Greenを外部API・保存NBT・決算の実機合格と混同しない。

## 前提と証拠

- 本番のBAN・BlockLocker保護・WWC/WWW個体を変更しない。Paper 26.2の**隔離テストサーバー**に使い捨てworld、playerdata、BlockLocker、WWC/WWW/WCL（実際に導入可能な対象のみ）を用意する。
- PR #8のcommit/JAR SHA-256、Paper・BlockLocker・各companion Plugin版、導入/未導入の組合せ、world/保存配置、テストUUID、BAN source一覧と参照時点を記録する。権威データがないAPIは未検証として扱う。
- 同じ物理コンテナ・Mobをloaded memoryと保存NBTの双方から二重計上しないことは、所有権判定とは別のsnapshot受入条件。未知の証拠を0資産として成功公開しない。

## 統合検証マトリクス

| ID / 対応CAT Draft | 検証対象・操作 | 合格条件 |
| --- | --- | --- |
| P5-OWNER-01 / BLOCKLOCKER-AGG-01,02 | 実際に付属したPRIVATE看板、無関係な看板、MORE_USERSのEveryoneをそれぞれ作成 | 付属した正規の看板だけが有効。Everyoneはフリー、無関係な看板は所有者を変えない |
| P5-OWNER-02 / BLOCKLOCKER-AGG-03,04 | ダブルチェストの片側に保護看板を付け、左右・向き・連結を変更 | 正当な相互連結は一つの保管単位。矛盾する配置や重複primaryはUNKNOWNとしてfail-closed |
| P5-OWNER-03 / BLOCKLOCKER-TEXT-01,02,04 | 保存NBTの表面4行、PDC UUID、表示名照合、色/ローカライズ別名を比較 | 表示名・PDCの不一致、欠損・復号不能を任意の個人/フリーへ分類しない |
| P5-OWNER-04 / PAPER-BAN-01,02,03 | 現在のprofile BANをON/OFF、未解決profile、他必須source欠損を検証 | 確認済みBANはBANNED。単一Paper CLEARや取得不能を総合CLEARに昇格しない |
| P5-OWNER-05 / PAPER-IP-01,02,03 | UUIDとIPの完全対応が確認できる場合と、対応欠損・複数アドレスの場合 | 完全な権威対応がなければUNKNOWN。既知のBANは優先し、推測でIP→UUIDを結ばない |
| P5-OWNER-06 / BAN-EVIDENCE-01,04 | 必須外部BAN sourceの導入/停止、観測欠損、重複source IDを試す | 必須source全てのCLEARを確認した場合だけ総合CLEAR。欠損はUNKNOWN、BANNEDは優先 |
| P5-OWNER-07 / COMPANION-OWNER-01,03 | WWC/WWWの指揮者・飼い主が異なる個体、未ロード個体、API無効化を検証 | 確認済み指揮者を優先。未ロード・API不在を指揮者なしと推定せずUNKNOWN |
| P5-OWNER-08 / COMPANION-WWC-01,02,03 | WWC loaded個体と保存済みPDC follow_targetを照合し、欠損・不正UUIDを試す | 正規のpositive evidenceだけ使用。PDC欠損・不正型をFREEと断定しない |
| P5-OWNER-09 / OWNER-EVIDENCE-04,05,06 | UNKNOWN owner/BANの保護下に実貨幣を置き、決算stagingと公開状態を確認 | 不確定証拠をゼロ扱いせず、未完了決算の公開を拒否。既存の公開済み世代を破壊しない |
| P5-OWNER-10 / COMPANION-WCL-01,02,03 | WCL providerのloaded/保存済み個体に対してSuccess(空/非空)、NotFound、Failureを発生させ、relationshipsのowner/commander根拠を検査 | NotFound/Failureを空資産や所有者不在とみなさない。provider固有のrelationship契約が未検証ならUNKNOWN。providerが未導入ならblockedと記録 |
| P5-OWNER-11 / BLOCKLOCKER-SNAPSHOT-01,02,03 | チャンク境界に跨る保護看板とダブルチェストを作り、隣接チャンクの保存看板/パレット候補を一部欠落させる | 完全な隣接候補集合の証明なしにFREE/個人を確定しない。両半分の相互連結・同一snapshotを別途検証 |

## 実行境界と未決事項

- 各ケースは **not-run / pass / fail / blocked** とし、対応する保存NBT・看板座標・UUID・外部API結果・server log・対象commitを紐付ける。テスト中に所有者/保護を変えた場合は、保存前後のsnapshot世代を分ける。
- WCL `develop`の`WonderfulStorageReader`・`StorageReadResult`・`StorageSnapshot`の型は静的確認済みだが、実配備版、WWC/WWW provider登録、relationshipsのowner/commander意味、未ロード個体の完全性は**実機・対象版の現物で検証するまで未確定**。NotFoundとFailureを空収納へ変換しない。未導入のプラグインを検証済みとしない。
- 外部BANプラグインの権威一覧、UUID/IPの完全対応、未ロードWWC/WWWの保存データ契約は未確定。各検証をblocked/not-runのまま保持し、必要な外部ソースを勝手に省略しない。
- 正式CAT Process/PI/TCEの規範確認・機械Gate、Paper 26.2・BlockLocker・WWC/WWW/WCL実機、loaded/disk同一時点性、決算公開は未実施。**本書の作成は実機テスト成功ではない。**
