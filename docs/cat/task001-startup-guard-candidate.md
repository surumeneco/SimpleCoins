---
cat_version: '0.3'
kind: candidate
id: simplecoins.paper.startup-offline-guard-candidate
status: candidate
process: simplecoins.paper.startup-offline-guard-candidate
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1sbhEqynLy7qu3iZt-kV1UrkibF2COXCj/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Candidate: Paper起動前の参加受付保護

## 参照した既存要件

決算中は管理者を含め参加を拒否し、正本走査と仕訳監視を検証してから受付を再開する。Paperプラグイン自体が欠損した場合、プラグイン内のLoginEventリスナーは実行されない。

## 技術候補と観測可能な振る舞い

Paper起動コマンドの直前に、対象JARの存在・ハッシュ・plugin.ymlと、全員拒否のホワイトリスト状態に加え、OPがホワイトリストを迂回できるJava版の性質を踏まえて`ops.json`の登録者0人も確認する。いずれか不正ならPaperを起動しない。この方式では本番のホワイトリスト設定を変更せず、検査不合格は起動失敗として扱う。正本スキャン完了とジャーナル有効化の前に参加受付を再開する機能は持たない。

## 未決定の事項

運営がホワイトリストの既存登録者を退避・切替すること、起動ガードをsystemd等すべての起動経路へ確実に配置すること、Paper本体停止後の保護、Proxy/JE/BEの経路ごとの検証、決算成功時の安全な解除手段。他プラグインによるホワイトリスト権限迂回は外部の実機検証事項。本候補をCAT Confirmed仕様に昇格したものではない。
