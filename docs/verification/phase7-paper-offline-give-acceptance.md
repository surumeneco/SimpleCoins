# Phase 7: Paperオフラインgive・再起動・受領一回性の実機受入

> 状態: **未実施の実機試験手順**。既存JUnit/CIや停止時台帳診断のGreenを、Paper上での一回性の証拠に置き換えない。未リリースのSimpleCoins開発版を隔離した検証環境で扱う。

## 根拠と適用範囲

- 基準ソース: `feature/initial-domain-20261027` commit `01a06f9f0fb78c8f941f2aa2e1c3df5e8951d153` の `CoinCommand`、`OfflineGrants`、`domain/GrantLedger`、`plugin.yml`。実施前に対象JARのcommit/ハッシュと差分を照合する。
- `/simplecoins give <known player|online selector> <amount>` はオンライン/既知オフライン対象に永続予約を作る。オフラインは過去参加済みで一意に解決できるUUIDのみ。対象がログインしたときに配送する。
- `/simplecoins give <online player> <denomination> <count>` はオンライン限定の額面指定予約。`wallet <count>` はオンライン対象へ直接渡す**別経路**であり、オフライン予約・受領一回性の試験に含めない。
- `simplecoins.admin` が必要。権限拒否・構文テストのCI成功だけでは許可された発行の実機動作を証明しない。
- `OfflineGrants` は受領PDC（`simplecoins:grant_completed`、`simplecoins:grant_active`、`simplecoins:grant_paid`）とインベントリを `player.saveData()` で保存し、保存済みplayerdataのPDC読戻し後に台帳をacknowledgeする設計。**電源断・ファイルシステム耐久性を保証する実測ではない。**

## 準備と証拠

1. 本番とは別の**使い捨てPaper 26.2互換検証環境**、検証専用ワールドと既知のテストプレイヤーを使用。Paper/JDK/プラグインのバージョン、SimpleCoins JAR SHA-256、起動ログ、時刻を記録。実機が未用意なら全Caseを `NOT-RUN` とする。
2. テストプレイヤーAを一度ログインさせ、正常に退出させる。Aが `Bukkit.getOfflinePlayers()` から一意に見つかる状態にする。管理者Bには `simplecoins.admin` を付与し、無権限プレイヤーCも用意する。実アカウントのUUIDや台帳原本は公開レポートに転載しない。
3. **停止時に** `plugins/SimpleCoins/issuance/`、ワールドの `playerdata/`、`plugins/SimpleCoins/config.yml` とJARを同一時点でバックアップ。検証用環境以外の台帳やplayerdataは編集しない。
4. 検証専用の発行量を決め、開始時のAのコイン実数・財布残高・インベントリ空き・PDC受領値・台帳連番/予約状態を記録する。**予約額と物理的に受け取ったコイン額を混同しない**。テスト間は停止時バックアップから復元するか、連番と期待額の増分を区別して記録する。

## 受入マトリクス

| Case | 操作（検証用環境のみ） | 必要な観測・合格条件 |
| --- | --- | --- |
| GRANT-01 | 無権限Cが `/simplecoins give A 100` とタブ補完を試行 | 拒否・補完空。台帳連番と物理コイン数に変化がない |
| GRANT-02 | Aがオフライン中に管理者Bが `/simplecoins give A 100` | 一意の予約IDが発行され、**オフライン中は物理コインが増えない**。次回ログインで100相当を受領し、PDCと台帳の状態が整合 |
| GRANT-03 | GRANT-02完了後にAが退出・サーバー正常停止・再起動・再ログイン | 同じ予約の再配布が起きず、物理コインと完了受領値が維持される。新しい予約を勝手に作らない |
| GRANT-04 | Aの36収納スロットを安全な検証アイテムで埋めてからオフライン金額予約、ログイン後に空きを作る | 満杯時に未配布予約が消えない。空き発生後に分割配布され、受領総額が予約額を超えず、途中のPDC `active/paid` が再起動後も整合 |
| GRANT-05 | 既知でない名前、曖昧な名前、オフライン対象への `wallet 1` / 額面指定 `100 2` を試す | 不正・未対応経路は明示的に拒否され、予約/物理配布の副作用がない。額面指定とwalletは**オンライン限定** |
| GRANT-06 | オンラインAへ `/simplecoins give A 100 2`、別に `/simplecoins give A wallet 1` | 額面指定の予約が200相当を100額面2枚として配送。walletは別経路で新規IDを持つことを確認。両者を同一の耐久性保証とみなさない |
| GRANT-07 | 同じAに連続して異なる金額の予約を発行し、ログイン・再起動を繰り返す | 各予約が順序どおり一度だけ反映され、受領連番が未配布の古い予約を飛び越さない |
| GRANT-08 | 停止時バックアップを使用して、予約・受領の巻戻り/破損を**別コピー上で**故障注入 | 安全停止または明示的な失敗となり、黙って二重配布しない。故障注入ができなければ `NOT-RUN`。本番データへ故障注入しない |

`GRANT-04` では、満杯で `inserted == 0` の場合は配送が保留される設計。空きを作った後の再試行はjoin/InventoryClose経路で起こり得るが、**実際のタイミングを観測**し、推測で成功扱いしない。`GRANT-08` は正常停止/再起動の試験とは別の障害復旧試験である。

## 台帳とplayerdataの突合

- 各Caseの前後で、実際のコイン/財布数、予約ID（内部記録のみ）、台帳連番、`delivered`、PDCの `completed/active/paid`、サーバーログを一組として記録する。プレイヤー識別情報は公開報告では伏せる。
- 停止後の**同一時点コピー**で `docs/verification/verify-issuance-journal.py` を実行できる。手順と終了コードは `docs/verification/phase7-issuance-journal-offline-check.md` に従う。診断が `ok` でもplayerdataとインベントリの同時永続化・受領一回性は**別途観測が必要**。
- 台帳/受領に不一致、`Issuance persistence failed`、`Cannot identify unique saved playerdata`、起動失敗/安全停止があれば、そのCaseはFAIL。**再発行で上書きしない**。停止時のバックアップ・ログ・ハッシュを保全し原因調査する。
- クラッシュ注入では、正常停止と強制停止を区別し、各復旧後に台帳・playerdata・物理コインの三者を検証する。強制停止で成功したと推定しない。

## 結果記録テンプレート

| Case | JAR SHA-256 / Paper版 | 操作前予約・受領・物理額 | 操作後予約・受領・物理額 | 再起動/ログ証拠 | 結果 |
| --- | --- | --- | --- | --- | --- |
| GRANT-01〜08 | 未記入 | 未記入 | 未記入 | 未記入 | NOT-RUN |

判定は `PASS / FAIL / BLOCKED / NOT-RUN` のいずれか。**未実施のCaseはPASSにしない**。テストが通ってもJE/BEパック・Nexo表示の合格とは独立であり、`docs/verification/phase7-je-be-nexo-acceptance.md` で別途検証する。試験終了後は検証環境を停止してバックアップへ復帰し、本番JAR・mainへの反映は利用者の明示的なリリース判断まで行わない。
