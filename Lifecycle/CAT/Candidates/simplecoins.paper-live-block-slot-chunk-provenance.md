---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.paper-live-block-slot-chunk-provenance
status: candidate
process: simplecoins.settlement.paper-live-source-coverage
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/345
  - https://github.com/surumeneco/SimpleCoins/issues/4
refs: []
semantic_contract: markdown-v2
language: ja
---

# LIVEブロック収納の物理スロットとChunk帰属の整合 Candidate

## 対象

| Ref | Aspect |
| --- | --- |
| source | 実際に読み取った一つのPaper ChunkSource |
| physical-slot | `world/<world>/<blockX>,<blockY>,<blockZ>:<slot>` 形式のLIVEブロック収納キー |
| assignment | ChunkSourceのワールド・Chunk X/Zと物理ブロックX/Zの対応 |

## 観測

現行`PaperLiveUniverseAssets.Observed`はスロットキーと金額を原集計に照合するが、同一World内の二つのChunkSource間で実在スロットを交換した場合、明細全体のスロット集合・額・World別Chunk件数が同じなので許してしまう。後段の所有者分類器には誤ったChunkSource IDが渡る。この候補は、既存の「保管先に結び付く帰属の検証」「重複なく全件の実体を読む」という要件の細分化である。

## 規則

| ID | Condition | Requirement | Verification |
| --- | --- | --- | --- |
| block-slot-01 | LIVE block inventory has physical coordinates | `floorDiv(x,16)` and `floorDiv(z,16)` must equal the declaring ChunkSource coordinates | 有効なChunk境界、負座標 |
| block-slot-02 | Two chunks exchange genuine block physical keys while preserving amounts | Refuse capture despite equal global slot map and money totals | 2 Chunk間の付け替え |
| block-slot-03 | A block-style key uses malformed coordinates or a nonnumeric slot | Refuse capture rather than guessing physical ownership | 不正な数値表現 |
| block-slot-04 | LIVE entity key uses `world/<world>/entity/<uuid>/...` | Keep identity/value validation; entity position is not inferred from UUID keys | 有効なEntity装備/収納の保持 |
| block-slot-05 | Scope is only this capture validation | No assertion of global snapshot freeze, ownership/BAN external proof, journal reconciliation, atomic publication or player admission | 既存fail-closed境界 |

## Boundary

本候補で扱うのはブロック収納キーのみ。EntityのChunk帰属はUUID文字列から導出できないため、Entity位置の別検証を追加したとは主張しない。実際のLIVE走査パスは既存の読み取り処理を維持し、外部プラグイン所有者を推定しない。

CAT Candidateであり、確認済み規範仕様への昇格、公式CAT Gate通過、参加許可・本番配備は含まない。
