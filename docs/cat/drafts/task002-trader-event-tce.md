---
cat_version: '0.3'
kind: tce
id: simplecoins.paper.tce.trader-event
status: draft
process: simplecoins.paper.trader-event
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
decision_ref: ''
refs:
  - simplecoins.paper.trader-event
semantic_contract: markdown-v2
language: ja
---

# TCE: 行商人発行の一回性 (Draft)

網羅要求: partial

## 振る舞い
| ID | 起点 | 条件 | 結果 | 効果 |
| --- | --- | --- | ---: | --- |
| no-roll | 行商人死亡 | 抽選に失敗 | 1 | 永続無発行レコード |
| issue | 行商人死亡 | 抽選成功・新規UUID・記録正常 | 1 | 生成貨幣を死亡ドロップへ一回追加 |
| replay | 行商人死亡 | 既存UUIDの永続記録 | 1 | 再抽選・再追加しない |
| write-fail | 行商人死亡 | 永続書込失敗 | 0 | ドロップの部分追加を拒否 |
| restart-pending | 起動 | 未確定レコードあり | 0 | 正常発行の許可を与えない |

これはDraftの技術試験である。Paperクラッシュ時の現物/ファイル統合正しさを保証する受入Gateではない。
