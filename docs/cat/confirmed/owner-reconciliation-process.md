---
cat_version: '0.3'
kind: process
id: simplecoins.accounting.owner-reconciliation
status: confirmed
process: simplecoins.accounting.owner-reconciliation
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
decision_ref: https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view#通常時の仕訳・差分反映
refs: 
  - simplecoins.accounting.owner-reconciliation.pi.replace
semantic_contract: markdown-v2
language: ja
---

# Process: 所有者状態変化時の確定再分類

## 契約
閉世界: 真

## 境界
| PI |
| --- |
| simplecoins.accounting.owner-reconciliation.pi.replace |

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
| 再分類 | simplecoins.accounting.owner-reconciliation.pi.replace/replace |

既決02A/02Bの所有者状態変化に対するメモリ反映境界。対象UUIDに関する現物の完全な観測と状態確認が得られた場合だけ、旧個人額・旧死蔵所在を新観測に置換する。消滅したplayerdata等を資産として残さない。無関係な所有者やFを変更しない。入力の観測完全性は呼出側の責任であり、この境界だけでは局所的な走査・索引・公開制御の実装を証明しない。
