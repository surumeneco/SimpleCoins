---
cat_version: '0.3'
kind: pi
id: simplecoins.display.java-pack-delivery.pi.request
status: confirmed
process: simplecoins.display.java-pack-delivery
source_refs:
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://drive.google.com/file/d/1Py55wN7KsfMqIWfD9P2IwSDwXSzUBqeR/view
  - https://github.com/surumeneco/SimpleCoins/issues/6
decision_ref: https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view#外部連携と非機能要件
refs: []
semantic_contract: markdown-v2
language: ja
---

# PI: Java版貨幣パックの要求可否

## 境界
| 相手Process | 方向 |
| --- | --- |
| simplecoins.display | 双方向 |

## 項目
| ID | 方向 | Domain | 存在 |
| --- | --- | --- | --- |
| 入力.Nexo委任 | 受信 | 真偽 | 必須 |
| 入力.Java接続 | 受信 | 真偽 | 必須 |
| 入力.配布情報準備済 | 受信 | 真偽 | 必須 |
| 出力.要求許可 | 送信 | 真偽 | 必須 |

## 操作
| ID | 方向 | 起点 |
| --- | --- | --- |
| evaluate | 受付 | 配布判定 |

## 制約
| 式 |
| --- |

配布情報準備済は生成したJava ZIPに対応する有効な配布先とハッシュが準備された状態。接続種別が不明な場合はJava接続と断定しない。要求許可はクライアントでの表示成功とは異なる。
