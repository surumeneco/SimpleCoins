---
cat_version: '0.3'
kind: process
id: simplecoins.accounting.memory-journal
status: confirmed
process: simplecoins.accounting.memory-journal
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://github.com/surumeneco/SimpleCoins/issues/5
decision_ref: https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view#通常時の仕訳・差分反映
refs: 
  - simplecoins.accounting.memory-journal.pi.record
semantic_contract: markdown-v2
language: ja
---

# Process: 確定仕訳のメモリ反映

## 契約
閉世界: 真

## 境界
| PI |
| --- |
| simplecoins.accounting.memory-journal.pi.record |

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
| 確定仕訳 | simplecoins.accounting.memory-journal.pi.record/record |

完成した全件決算を初期値とする。確定操作に一つの記帳順序を付与し、再送で二重加算しない。操作内の全差額が整合する場合のみ一括反映する。負残高、順序欠落、未確定の物理結果を正常記帳しない。通常の主体別残高と死蔵所在のみ保持し、毎回全件計算・同期WALを要求しない。金額・帰属の観測はPaper側の責任であり本PIは物理操作の成立を証明しない。
