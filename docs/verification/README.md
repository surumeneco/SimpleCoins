# 検証・受入資料の入口

ここにあるMarkdownは**どの条件で何を検査し、何を記録するか**を定めます。実行済みを意味しません。実装状況・現在の残作業は[Issue #423](https://github.com/surumeneco/SimpleCoins/issues/423)、製品の要求・G0〜G3は[Drive 01〜05](https://drive.google.com/file/d/1lLgAP9zAf8e0_Il8Mdh4HGXNr2aJ4ShC/view)が正本です。

| 検証対象 | 文書 |
| --- | --- |
| 保存済みソース・WorldScope・異常ファイル・重複・LIVE統合（G0） | [phase5-storage-discovery-acceptance.md](phase5-storage-discovery-acceptance.md) |
| 所有者判定のINCOMPLETE / FREE / DEAD / PERSONAL | [phase5-owner-evidence-decision-table.md](phase5-owner-evidence-decision-table.md) |
| プロフィール・IP・外部BANの証拠閉包 | [phase5-ban-source-boundary.md](phase5-ban-source-boundary.md) |
| BlockLockerの保護・ローカライズ・旧形式 | [phase5-blocklocker-alias-boundary.md](phase5-blocklocker-alias-boundary.md) |
| WCL/WWC/WWWのprovider・収納・未ロード個体 | [phase5-wcl-provider-contract.md](phase5-wcl-provider-contract.md) |
| サーバーリポジトリの追跡JAR一覧（**過去の固定観測**） | [phase5-deployment-plugin-inventory.md](phase5-deployment-plugin-inventory.md) |
| 決算・台帳・復旧のクラッシュ注入（G0/G3） | [phase6-crash-recovery-acceptance.md](phase6-crash-recovery-acceptance.md) |
| durable journalのリンク・保存境界 | [phase6-durable-link-boundary.md](phase6-durable-link-boundary.md) |
| 管理give・配布予約・統計・死蔵照会（G3） | [phase7-admin-give-acceptance.md](phase7-admin-give-acceptance.md) |
| JE/BE/Geyser/Nexoの実機操作 | [phase7-je-be-nexo-acceptance.md](phase7-je-be-nexo-acceptance.md) |
| JE/BEパック生成からクライアント配布まで | [phase7-je-be-pack-handoff.md](phase7-je-be-pack-handoff.md) |

## 実行と判定

- **開始前**：固定HEAD、対象JAR SHA、Paper/JDK/Geyser/BlockLocker/WWC/WWW等の実版、試験対象データと隔離環境を記録。保存・故障注入は本番では行わない。
- **実行時**：記載のケースIDごとに結果とログ/証拠を記録し、既存のCI成功を実機Greenへ流用しない。未検証は`not-run`、未取得・判断不能は`inconclusive`/`blocked`を区別。
- **完了判定**：対象HEADの実コード・テスト・実機と照合し、残るG0/G3未保証を「検証資料がある」だけで解除しない。
- **旧資料の来歴**：一部の本文に旧TaskQueueのTASK IDや古いDraft PRが引用されています。これらは当初の要求/観測の**過去の参照**であり、現在の担当・実装・成功状況を示しません。新しい実行の進捗はIssue/PR/Actionsに記録します。
- **機械成果**：`verify-*.py`、`verify-*.sh`などのテスト・検証スクリプトは、README整理だけで振る舞いを変更せず、実行結果をCAT正式Gateに読み替えません。

[docs/README](../README.md)に戻る。
