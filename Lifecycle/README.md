# CAT-TDD / Work の入口

`Lifecycle/CAT/Candidates/`はコード・テスト・ログなどから観測した**仕様化候補**、`Lifecycle/Works/`は作業境界・許可ファイル・依存・検査・Gateを定義する**Work文書**です。いずれもファイルが存在することだけでは確認済み規範仕様、テスト通過、実装完了を表しません。

| 読みたいこと | 正しい入口 |
| --- | --- |
| ゲーム・統計・決算・運用上の要求 | [Drive仕様の入口](https://drive.google.com/file/d/1lLgAP9zAf8e0_Il8Mdh4HGXNr2aJ4ShC/view) |
| 残作業・優先順位・担当・実行状況 | [Issue #423](https://github.com/surumeneco/SimpleCoins/issues/423) と領域別Issue |
| 観測に基づく逆仕様化・未確認規則 | [CAT/Candidates/](CAT/Candidates/) |
| 個別タスクのフロー・入力・検査・stage権限 | [Works/](Works/) |
| 補助的なCAT Draft・検証設計 | [docs/cat/](../docs/cat/README.md) |
| 実際に成功した試験・変更 | 対応する固定コミットのJUnit/CI・PR・実機証拠 |

## 解釈上の境界

- `status: draft` / `candidate` は**ユーザー承認済みのConfirmedではない**。`decision_ref`が空の場合は判断権限の存在を推測しない。
- `Gates`表の`not-run`は、単なるJUnit GreenやPR mergeでは埋まらない。正規CATスクリプトの実行・入力・出力ハッシュ・確定した決定根拠を確認する。
- Workの`mode`、`flow`、`work_kind`、許可pathはCAT-TDD正本のルーティングに従う。タスク文書の存在だけで他のWorkや本番ソースの変更を許可しない。
- 古いHEADや旧PRを参照するWork/Candidateは**その時点の観測**として保持する。現行挙動・完了状態は現ソースと現Issueを優先する。
- 作業を完遂したら、新しい日時別の完了段落を全Workに積み上げず、Git履歴・当該Issue/PR/証跡へ記録する。機械所有のArtifactや旧証拠を恣意的に書き換えない。

[リポジトリREADME](../README.md)に戻る。
