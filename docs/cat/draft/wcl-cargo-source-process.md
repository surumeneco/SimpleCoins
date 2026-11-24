---
cat_version: '0.3'
kind: process
id: simplecoins.accounting.wcl-cargo-source
status: draft
process: simplecoins.accounting.wcl-cargo-source
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/681
decision_ref: ''
refs: 
  - simplecoins.accounting.wcl-cargo-source.pi.capture
semantic_contract: markdown-v2
language: ja
---

# Process: WWC/WWW通常仕訳における収納実体の選択

## 契約
閉世界: 真

## 境界
| PI |
| --- |
| simplecoins.accounting.wcl-cargo-source.pi.capture |

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
| 収納読取 | simplecoins.accounting.wcl-cargo-source.pi.capture/capture |

02Bの通常仕訳は局所的な実データの前後差分を確定操作単位で反映する。GUIが実収納として有効な間は、その可用貨物スロットのみをWCLの対応する貨物と置換して一度だけ読取る。GUI終了後は同一個体のWCL読取を利用する。装備や非表示の貨物は従来の読取を保持する。正本が不明なら0とせず保留する。選択結果は記帳前の観測源指定であり、実体読取、帰属判定、差分反映、入室可否の成功を意味しない。

このDraftは既決の02Bに対する技術的観測境界の形式化であり、貨幣、所有権、BAN、統計、配備の規範変更ではない。
