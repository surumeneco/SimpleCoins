---
cat_version: '0.3'
kind: process
id: simplecoins.terrainlegacy
status: draft
process: simplecoins.terrainlegacy
source_refs:
  - https://drive.google.com/file/d/173sQAGgr2JuL36ixCPtJ23daGfoOYNYQ/view
refs:
  - simplecoins.terrainlegacy.pi.chunk
semantic_contract: markdown-v2
language: ja
---

# Process: simplecoins.terrainlegacy

## 契約
閉世界: 偽

保存済みterrain chunkのlegacy `TileEntities` を検査する境界。未対応の資産がある場合、資産計上を完了したものとして下流のvisitorに渡さない。これは既存Taskの要件から作成したDraftであり、規範確認・正式Gateを通過していない。

## 境界
| PI |
| --- |
| simplecoins.terrainlegacy.pi.chunk |

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
| 走査要求 | simplecoins.terrainlegacy.pi.chunk/inspect |
