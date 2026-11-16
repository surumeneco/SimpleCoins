---
cat_version: '0.3'
kind: process
id: simplecoins.world-source-discovery
status: draft
process: simplecoins.world-source-discovery
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/429
decision_ref: ''
refs:
  - simplecoins.world-source-discovery.pi.manifest
semantic_contract: markdown-v2
language: ja
---

# Process: Paper 26.x保存ソース発見とプレイヤーデータの閉包 (Draft)

## 契約
閉世界: 真

## 境界
| PI |
| --- |
| simplecoins.world-source-discovery.pi.manifest |

## 仕様状態
| ID | Domain |
| --- | --- |

## 観測能力
| ID | 情報源 | Domain |
| --- | --- |
| 観測.各ワールドのディメンションパス | Paper World#getWorldPath | 真偽 |
| 観測.レベル共有プレイヤーデータ | 許可済み共有ルートのplayers/data又は既存playerdata | 真偽 |

## 作用能力
| ID | 対象 | Domain |
| --- | --- | --- |
| 作用.正本候補破棄 | 未公開Manifest | 真偽 |

## 起点
| ID | 由来 |
| --- | --- |
| 発見 | simplecoins.world-source-discovery.pi.manifest/discover |
| 集合再検証 | simplecoins.world-source-discovery.pi.manifest/recheck |

## 認められた要求と未決境界
02Bの設計正本は、Minecraft 26.1+ の `dimensions/minecraft/<dimension>/region|entities` と共有 `players/data` を別の正本として列挙し、WorldScopeのON/OFFはリージョンを選び、プレイヤーデータはワールド非依存で常時対象とする。World名からデータ保存位置を推測せず実パス由来のみを正本とする。

本Draftは共有ルートを選ぶ権限と同一物理ファイル／別World別名の完全性を設計するが、LIVEメモリ、他Plugin書込み、全保存ファイルの同時点、OS電源断を保証しない。個別技術例で証明できない条件は上位決算G0と区別する。
