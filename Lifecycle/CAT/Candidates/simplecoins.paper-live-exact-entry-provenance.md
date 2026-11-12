---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.paper-live-exact-entry-provenance
status: candidate
process: simplecoins.settlement.paper-live-inventory
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/344
refs: []
semantic_contract: markdown-v2
language: ja
---

# ライブ資産の保管スロット単位での正確な照合 Candidate

## 対象

| Ref | Aspect |
| --- | --- |
| paper-report | `PaperLiveUniverseAssets.Report` 中のWorld/Player貨幣エントリ |
| source-detail | `PaperLiveUniverseAssets.Observed.sources` 中のLIVEソース別のスロットと金額 |
| physical-identity | 物理スロットID＋金額の一対一対応 |

## 観測

現行`Observed`はLIVEソース件数と総金額が一致するかを主に検査する。同額だが別のスロットを指す詳細を後から生成した場合、集計金額の同一性だけでは不一致を検出できない。価値が不変でも保管元の同一性が失われれば、所有者帰属や死蔵所在索引を誤る。

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| exact-entry-01 | 明細が報告した物理スロットと同額で存在 | World/Playerそれぞれの一件を一度だけ消費して照合 | 成功fixture |
| exact-entry-02 | 同額だが別物理スロットを明細が報告 | 総額一致でも受理しない | 置換slot回帰 |
| exact-entry-03 | 同じ物理スロットに異なる金額を載せる | 総額の偶然一致でも受理しない | 交換額面回帰 |
| exact-entry-04 | レポート中に同じ物理スロットが複数回存在 | 明細一意性だけに依存せず拒否 | report側の重複検証 |
| exact-entry-05 | 受領した明細が物理スロット数と同額で完全一致 | 従来のWorld/Player件数・総額照合を保持して成功 | 既存JUnit回帰 |
| exact-entry-06 | 一致した | チャンクロード集合の静止・所有者・現在BAN・決算確定を別途証明する | admission閉維持 |

## Boundary

ライブ保管スロットの認証は同一Paper走査内のReportと明細間に限る。別ワールド間の二重財布UUIDは既存の共有Setが拒否する。保存NBTの時間的一致・外部プラグイン・BlockLocker等は別工程。これはCAT Candidateであり正式Gate/Confirmedではない。
