---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase5.deployment-jar-inventory
status: draft
process: simplecoins.owner-verification
source_refs:
  - https://drive.google.com/file/d/1sn-FXbU7nFDr6yc6YxPAe2V6U6mV2CZD/view
  - docs/verification/phase5-deployment-plugin-inventory.md
refs: []
semantic_contract: markdown-v2
language: ja
---

# DomainSpec: Phase 5 配備JARのファイルシステム証拠

## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.phase5 | 所有者・BAN外部ソースの実配備候補を照合するための読取専用補助ツール |
| simplecoins.runtime | 実際のPaper plugin manager・ロード済みサービスとは別の証拠 |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| JAR-OBS-01 | plugins直下に複数のJARが存在する | ファイル名の安定順にSHA-256、バイト数、ルートdescriptor名を報告し、同一入力で同一結果とする | test_sorted_inventory_and_exact_bytes_sha256 |
| JAR-OBS-02 | 不正ZIPまたはplugin descriptor欠損 | 黙って完全な配備一覧とせず、理由を返し非ゼロ終了する | test_corrupt_jar_is_not_silently_ignored, test_no_descriptor_is_explicitly_incomplete |
| JAR-OBS-03 | JARがsymlink、または指定pluginsディレクトリがsymlink | シンボリックリンクを追跡して外部ファイルを読まない | test_symlink_jar_is_not_followed, test_missing_or_symlink_directory_is_rejected |
| JAR-OBS-04 | 収集が成功した | 出力はあくまでfilesystem-root-jars-onlyとし、loaded_plugins_verified=falseを明示する | test_sorted_inventory_and_exact_bytes_sha256 |
| JAR-OBS-05 | 非JARファイルやpluginの設定・BANリストが同ディレクトリにある | 非JARを読まず、実BAN状態や完全な外部source一覧を推論しない | test_sorted_inventory_and_exact_bytes_sha256 |
| JAR-OBS-06 | SHA計算とZIP descriptor検査の間にJARファイルが変更された | 一貫した単一ファイルの証拠とみなさず、changed_during_scanとして失敗する | test_modified_jar_during_scan_is_rejected |

## 境界と状態

TASK-20261101-004の既存受入資料から切り出した、配備候補の**静的な観測補助**。既存の設計判断や所有者分類ロジックを変更しない。実際のPaperロード状態、BAN providerの完全性、UUID/IP対応、BlockLocker/WWC/WWW/WCL APIの動作を検証したことにはならない。

本書はCAT Draftであり、Confirmedではない。正式CAT Process/PI/TCEの規範確認、process-closure・model-conformance・生成テスト等の機械Gate、実機統合試験は未実施。補助スクリプトの単体テスト成功をそれらの代替にしない。
