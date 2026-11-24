---
cat_version: '0.3'
kind: process
id: simplecoins.display.java-pack-delivery
status: confirmed
process: simplecoins.display.java-pack-delivery
source_refs:
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://drive.google.com/file/d/1Py55wN7KsfMqIWfD9P2IwSDwXSzUBqeR/view
  - https://github.com/surumeneco/SimpleCoins/issues/6
decision_ref: https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view#外部連携と非機能要件
refs: 
  - simplecoins.display.java-pack-delivery.pi.request
semantic_contract: markdown-v2
language: ja
---

# Process: Java版貨幣パック配布判定

## 契約
閉世界: 真

## 境界
| PI |
| --- |
| simplecoins.display.java-pack-delivery.pi.request |

## 仕様状態
| ID | Domain |
| --- | --- |

## 観測能力
| ID | 情報源 | Domain |
| --- | --- | --- |

## 作用能力
| ID | 対象 | Domain |
| --- | --- | --- |

## 起点
| ID | 由来 |
| --- | --- |
| 配布判定 | simplecoins.display.java-pack-delivery.pi.request/evaluate |

NexoなしではSimpleCoinsがJava版配布を担当する。Nexo導入時の配布はNexoへ委任する。BE用マッピング・配布は別経路であり、Javaパックの要求をBE配布とはみなさない。本契約は要求可否の判定であり、ネットワーク到達・クライアント受領・Nexo委任の実行を証明しない。
