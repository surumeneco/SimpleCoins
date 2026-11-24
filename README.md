# SimpleCoins

Minecraft **Paper 26.2** 向けの独自貨幣プラグイン。物理貨幣、財布、資産の所有者・統計、オフライン配布、Java/Bedrock用表示経路を扱います。レジ機能は **1.1.0** の対象です。

> **開発中・未リリース（1.0.0-dev）**  
> このリポジトリの `develop` に個別の機能・テストが存在しても、製品全体の動作・全件決算・配布の一回性・入室解放が成立したとは限りません。現在の `SimpleCoinsPlugin` は決算エンジンを公開接続しておらず、独立した受付ガードを全プレイヤー拒否で維持しています。**完成品としてサーバーへ導入しないでください。** 現状態は必ず対象HEADのコード・ログ・CIから確認してください。

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
