---
cat_version: '0.3'
kind: pi
id: simplecoins.terrainlegacy.pi.chunk
status: draft
process: simplecoins.terrainlegacy
source_refs:
  - https://drive.google.com/file/d/173sQAGgr2JuL36ixCPtJ23daGfoOYNYQ/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# PI: simplecoins.terrainlegacy.pi.chunk

## 境界
| 相手Process | 方向 |
| --- | --- |
| simplecoins.snapshot | 双方向 |

## 項目
| ID | 方向 | Domain | 存在 |
| --- | --- | --- | --- |
| 入力.root_tile_entities | 受信 | 列挙{absent,empty,nonempty,malformed} | 必須 |
| 入力.level_tile_entities | 受信 | 列挙{absent,empty,nonempty,malformed} | 必須 |
| 出力.visit | 送信 | 真偽 | 必須 |
| 出力.error | 送信 | 真偽 | 必須 |

## 操作
| ID | 方向 | 起点 |
| --- | --- | --- |
| inspect | 受付 | 走査要求 |

## 制約
| 式 |
| --- |

入力はterrain chunk内の各legacy tagを分類した仕様上の観測値であり、Java/NBTの内部表現をPIの状態として定義するものではない。分類不能はmalformedとして扱う。

出力はJavaの戻り値ではなく、legacy guard境界の観測結果を表す。`出力.error = 真` はvisitorへの引渡し前に`IOException`で拒否すること、`出力.visit = 偽` はvisitorを呼び出さないことを意味する。`出力.visit = 真` はlegacy guardが引渡しを許可することだけを表し、後続のdecode・visitor・他sourceの検証まで成功する保証ではない。`出力.error = 偽` もこのguard自身が拒否しないという限定的な意味である。

これはDraftであり、enumと例外結果の機械変換可否は正式Gateで別途判定する。
