---
cat_version: '0.3'
kind: pi
id: simplecoins.paper.pi.guard
status: draft
process: simplecoins.paper.failclosed-guard
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1AZxqlD11YUxDgpe10DYtG18ht5IoMZgc/view
decision_ref: ''
refs: []
semantic_contract: markdown-v2
language: ja
---

# PI: guard admission check (Draft)

## 境界
| 相手Process | 方向 |
| --- | --- |
| simplecoins.paper.startup-admission-eligibility | 双方向 |

## 項目
| ID | 方向 | Domain | 存在 |
| --- | --- | --- | --- |
| 入力.mainEnabled | 受信 | 真偽 | 必須 |
| 入力.serviceRegistered | 受信 | 真偽 | 必須 |
| 入力.verifiedAdmission | 受信 | 真偽 | 必須 |
| 入力.stopping | 受信 | 真偽 | 必須 |
| 出力.allowLogin | 送信 | 真偽 | 必須 |

## 操作
| ID | 方向 | 起点 |
| --- | --- | --- |
| evaluate | 受付 | 接続検証 |

## 制約
| 式 |
| --- |
