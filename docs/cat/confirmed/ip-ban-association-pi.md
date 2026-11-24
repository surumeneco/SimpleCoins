---
cat_version: '0.3'
kind: pi
id: simplecoins.accounting.ip-ban-association.pi.resolve
status: confirmed
process: simplecoins.accounting.ip-ban-association
source_refs:
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
decision_ref: https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view#IP-BANのUUID帰属（2026-11-25確定）
refs: []
semantic_contract: markdown-v2
language: ja
---

# PI: IP BAN照合

## 境界
| 相手Process | 方向 |
| --- | --- |
| simplecoins.accounting | 双方向 |

## 項目
| ID | 方向 | Domain | 存在 |
| --- | --- | --- | --- |
| 入力.自動一致 | 受信 | 真偽 | 必須 |
| 入力.補正あり | 受信 | 真偽 | 必須 |
| 入力.補正一致 | 受信 | 真偽 | 必須 |
| 出力.IPBAN | 送信 | 真偽 | 必須 |

## 操作
| ID | 方向 | 起点 |
| --- | --- | --- |
| resolve | 受付 | 照合 |

## 制約
| 式 |
| --- |

自動一致は当該UUIDの最終接続IPと現在BANリストの一致。未取得IPを推測せず一致としない。補正一致は手動指定IPとの一致で、補正なしなら値を参照しない。複数UUIDが同じIPと一致してよい。現在のBANリストにない過去のBANは一致ではない。
