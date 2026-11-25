# SimpleCoins

Minecraft **Paper 26.2** 向けの独自貨幣プラグイン。物理貨幣、財布、資産の所有者・統計、オフライン配布、Java/Bedrock用表示経路を扱います。レジ機能は **1.1.0** の対象です。

> **開発中・未リリース（1.0.0-dev）**  
> このリポジトリの `develop` に個別の機能・テストが存在しても、製品全体の動作・全件決算・配布の一回性・入室解放が成立したとは限りません。必要な提供者を確認し、起動時決算の完了と通常会計タスクの稼働を条件に受付ガードを解放する構成です。依存不足・診断モード・決算失敗時は解放しません。**完成品としてサーバーへ導入しないでください。** 現状態は必ず対象HEADのコード・ログ・CIから確認してください。

## 文書の入口と正本

| 知りたいこと | 参照先 |
| --- | --- |
| ゲームプレイ・貨幣・財布・レジの要求 | [Google Drive 01](https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view) |
| 資産の所有者とP/F/D・統計式 | [Google Drive 02A](https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view) |
| 正本データの収集・仕訳・全件決算・安全な公開 | [Google Drive 02B](https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view) |
| コマンド・権限・config・配布予約 | [Google Drive 03](https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view) |
| 技術的制約・未決事項・一次調査 | [Google Drive 04](https://drive.google.com/file/d/1Py55wN7KsfMqIWfD9P2IwSDwXSzUBqeR/view) |
| 1.0.0の対象・G0〜G3・受入条件 | [Google Drive 05](https://drive.google.com/file/d/1fSsHEn1tFX_DvnzxDK3t5FjDrIuvMrnt/view) |
| 現在の作業順・担当・依存・進捗 | [Issue #423](https://github.com/surumeneco/SimpleCoins/issues/423) と関連 [#3](https://github.com/surumeneco/SimpleCoins/issues/3) / [#4](https://github.com/surumeneco/SimpleCoins/issues/4) / [#5](https://github.com/surumeneco/SimpleCoins/issues/5) / [#6](https://github.com/surumeneco/SimpleCoins/issues/6) |
| CAT-TDDの候補・Work・証拠 | [Lifecycle/](Lifecycle/README.md) |
| 結合/実機検証・CAT参考資料 | [docs/](docs/README.md) |

仕様の入口は[DriveのSimpleCoins README](https://drive.google.com/file/d/1lLgAP9zAf8e0_Il8Mdh4HGXNr2aJ4ShC/view)。**仕様の採用状況はDrive、現行の実装・CI・PR・リリース状態はGitHub**が根拠です。過去のIssue/PR/TaskQueue番号やDraftは、現在の残作業やConfirmed仕様を自動的に意味しません。

## 開発環境・構成

- **Java 25 / Gradle 9.2.1**（Gradle toolchain 25）。
- **Paper API `26.2.build.134-stable`**。テストはJUnit 5。
- Gradleマルチプロジェクト：`SimpleCoins`本体と独立した `admission-guard`（`STARTUP`、別JAR）。SimpleCoins本体は`POSTWORLD`でGuardを依存指定。
- 機能モジュール：`src/main/java/.../domain/`（資産・保存・純計算）、`.../paper/`（Paper連携）、`src/main/resources/`（設定・descriptor）、`admission-guard/`、`tools/`、`tests/`。
- GitHub Actionsは`.github/workflows/`。個々のGreenはそのテスト範囲だけの根拠であり、完全なG0/G3やCAT正式Gateの代わりではありません。

### ビルド・単体回帰

```bash
gradle --no-daemon clean test :admission-guard:test jar :admission-guard:jar
python3 tools/simplecoins_preflight.py admission-guard/build/libs/admission-guard-1.0.0.jar build/libs/SimpleCoins-1.0.0.jar
```

実際の配布版、JAR名、ビルド・CIコマンドは当該HEADの`build.gradle.kts`とワークフローで確認してください。**JARのビルド成功だけで本番導入しない**こと。とくにGuard JAR欠損や異常終了時の受付遮断には、サーバー側の独立した起動前検査が必要です（[admission-guard/README.md](admission-guard/README.md)）。

## 導入・運用手順（受入用、正式リリース前）

まずバックアップを取得した隔離コピーで確認してください。以下は現行実装の設定手順であり、本番配備の承認ではありません。

1. Java 25 / Paper 26.2を使用し、上記のビルド・preflightを通過した本体とGuardの2つのJARを同時に配置します。現行の全件決算経路には、有効なBlockLockerとWonderfulCoreLibrary、WWC/WWWそれぞれの互換ストレージReader登録が必要です。Geyser・Floodgate・Nexo・InvSee++は連携先です。必要な提供者が欠けると受付は閉じたままになります。
2. 初回起動で `plugins/SimpleCoins/config.yml` を生成し、停止して設定します。統計対象や資産データに影響する設定変更は停止中に行い、再起動で全件決算してください。診断専用の `settlement.diagnostic-only: true` は受付を開きません。通常は既定の `false` を使用します。
3. 起動時の決算・統計公開が完了してから接続し、`/sc stats` と対象プレイヤーの残高を確認します。失敗時は受付を迂回せず停止・ログと正本データを保全し、原因を解消して再起動してください。発行台帳やプレイヤーデータを任意に消して復旧しないでください。

### 主な設定

| キー | 意味・既定値 |
| --- | --- |
| `currency.unit` / `coins.<額面>.name` | C / `<額面> Coin`。識別ID・価値は固定 |
| `wallet.max-balance` | 640000。引下げ後の既存残高は保持し、上限超過中も出金可能 |
| `trader-drops` | 有効、確率0.1、枚数1、9額面の重み256〜1 |
| `statistics.default-enabled` / `enabled-worlds` / `disabled-worlds` | 既定ON、明示リスト。両リストにあるワールドは既定値、プレイヤーデータ等は常時集計 |
| `settlement.max-source-entries` | 1000000。探索上限。上限超過は不完全な集計を公開せず停止 |
| `resource-pack.public-url` | JE配布用HTTP(S) URL。空欄は配布要求なし。Nexo導入時は使用しない |

### 貨幣の表示

Nexoを導入していない場合は、`plugins/SimpleCoins/images/` の額面名に対応するPNGを差し替えて再起動します。半角スペース・アンダースコア・ハイフンは画像名の照合時に同一視されます。同じ額面に複数の候補を置かないでください。

- JE：生成された `plugins/SimpleCoins/SimpleCoins-java.zip` をクライアントから到達可能なHTTP(S)サーバーで公開し、そのZIPのURLを `resource-pack.public-url` に設定して再起動します。更新時には公開側のZIPも同じ生成物に置き換えます。接続時の要求とダウンロード結果を確認してください。
- BE：Geyser-Spigotの `config.yml` で `gameplay.enable-custom-content: true` を設定します。SimpleCoinsは生成した `geyser/SimpleCoins.mcpack` と `geyser/simplecoins.json` をGeyserの `packs/simplecoins-generated.mcpack` と `custom_mappings/simplecoins-generated.json` へ配置します。変更時はGeyserを含むサーバーを再起動して登録・受信・表示を確認してください。管理対象ファイルを直接編集しないでください。

Nexo導入時は、SimpleCoinsが生成したJavaパックをNexoの `pack/external_packs/simplecoins-generated.zip` に配置し、変更時には起動完了後に `nexo reload pack` を要求します。SimpleCoinsからのJE直接配布は行わず、従来の自主管理Geyserファイルも削除します。Nexoによるパック統合・ホスティング・配布を設定し、BEには対応する変換・マッピング・配布経路（Scaffolding等）を用意してGeyserを再起動してください。**Nexo/Scaffolding実環境での動作保証は1.0.0の受入対象外で、実環境未検証です。** 配置・再生成要求の成功はクライアント表示成功を意味しません。

### 運営コマンド

`/simplecoins` または `/sc` を使用します。権限はOP既定で、権限プラグインから設定できます。

| コマンド | 権限・用途 |
| --- | --- |
| `/sc stats` | simplecoins.admin：P/F/D/T等の統計 |
| `/sc balance <player>` | simplecoins.admin：個人保有額 |
| `/sc dead [page]` | simplecoins.admin：死蔵資産の所在・理由 |
| `/sc give <target> <amount>` | simplecoins.give：最小枚数で発行、既知オフライン対象は予約 |
| `/sc give <target> <item> <count>` | simplecoins.give：オンライン対象へ額面指定の貨幣またはwalletを配布 |

### IP BANの資産分類（開発中）

`plugins/SimpleCoins/last-player-addresses.yml` は参加成功時のUUID別最終IPを自動記録します。手動補正は別の `plugins/SimpleCoins/ip-ban-overrides.yml` に記述します。例（架空UUID・文書用IP）:

```yaml
"00000000-0000-4000-8000-000000000001": "192.0.2.42"
```

手動値が優先され、行を削除すると自動記録に戻ります。現在のPaper IP BANリストとの照合時に読み直すため、過去に解除されたBANは資産分類に残りません。対応IPが不明のUUIDを推測で登録しません。手動補正は統計上の帰属にのみ影響し、PaperのBAN設定を変更しません。これらのファイルは運営者専用であり公開・Git管理対象ではありません。既存プレイヤーの記録は次回参加または手動設定で補います。

## 開発・検証・リリースの境界

- `feature/*`で独立作業を行い、検証された単位を`develop`へ統合します。利用者のリリース承認前に`main`へマージしません。
- **G0**：全資産ソースと保存/LIVEの同一会計世代の保証。**G3**：実際の物理配布と受領証拠の永続化・故障復旧による一回性。現行の障害・未完了はIssue #423で追跡します。
- CAT Artifactの`Draft`、`Candidate`、正式`Confirmed`、スクリプト所有の各Gate、JUnit、Paper実機、JE/BEクライアント、公開・リリースは異なる状態です。**確認なしに別の状態へ昇格させない**でください。
- 作業履歴はGitコミット/PR/ActionsとIssueへ残し、仕様・受入基準の文書へ日付別の進捗を追記しません。既存の検証計画は入力/期待結果を保持し、古いBranchや旧TaskQueueの引用は過去の証拠として扱います。
