---
cat_version: '0.3'
kind: process
id: simplecoins.paper.failclosed-guard
status: draft
process: simplecoins.paper.failclosed-guard
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1AZxqlD11YUxDgpe10DYtG18ht5IoMZgc/view
decision_ref: ''
refs:
  - simplecoins.paper.pi.guard
semantic_contract: markdown-v2
language: ja
---

# Process: Paper standalone admission safety guard (Draft)

## 契約
閉世界: 真

## 境界
| PI |
| --- |
| simplecoins.paper.pi.guard |

## 仕様状態
| ID | Domain |
| --- | --- |

## 観測能力
| ID | 情報源 | Domain |
| --- | --- |

## 作用能力
| ID | 対象 | Domain |
| --- | --- |

## 起点
| ID | 由来 |
| --- | --- |
| 接続検証 | simplecoins.paper.pi.guard/evaluate |

## 適用範囲
このDraftはSimpleCoins本体の登録以前と本体無効化後の**独立受付拒否**を対象とする。本体が未起動・未登録・不健康である限り拒否する。技術候補はSTARTUPロード別JARとPaper ServicesManagerに登録する読み取り専用許可API。独立ガードJAR自体の欠損はサーバー外の起動前検査が必要であり、本Processだけでは防げない。
