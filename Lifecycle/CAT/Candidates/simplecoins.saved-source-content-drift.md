---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.saved-source.content-drift
status: candidate
process: simplecoins.settlement.snapshot
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/blob/51e134d860de8f937f731832d97db24b15f16b15/src/main/java/co/surumene/simplecoins/domain/WorldSnapshotTraversal.java
  - https://github.com/surumeneco/SimpleCoins/pull/284
refs: []
semantic_contract: markdown-v2
language: ja
---

# Code-to-spec Candidate: 保存ソースの同一メタデータ書換え

元の決算設計02Bの全件整合／失敗時の非公開と、既存Draft Red PR #284の観測を区別する。これは**未承認Candidate**であり、CAT Confirmedでも本番同期の成立証明でもない。

## 対象

| Ref | Aspect |
| --- | --- |
| simplecoins.saved.source | Anvil regionとplayerdataの保存ファイル |
| simplecoins.snapshot.traversal | Manifestの事前読取・走査・事後照合 |
| simplecoins.snapshot.publisher | 全件成功後だけ公開する後段境界（本Workの実装対象外） |

## 規則

| ID | 条件 | 要求（技術候補） | 検証 |
| --- | --- | --- | --- |
| drift.same.metadata | 走査中に保存ファイルの本文が変わり、size/mtimeが復元される | メタデータだけで同一と判定せず、保存ソースの内容照合が失敗した場合は部分結果を返さない | PR #284の同一サイズ・mtime固定の内容書換えRed |
| drift.unchanged | 同じファイルを変更せず走査する | 正常な入力は従来どおり処理でき、成功報告に同じ件数を残す | 変更なし対照試験 |
| drift.bounded | 大きな保存ソースを照合する | 全体をメモリに読み込まず固定サイズのバッファで検証し、サイズ増減や不正なファイル種類を拒否する | 大容量メモリ消費検査、長さと属性の変化試験 |
| drift.scope | source contentが照合で一致しても、Paperメモリ側の実体・外部プラグイン・同一時点の正本をまだ証明できない | 走査完了を起動決算確定や入室許可と混同しない | G0統合別受入／実Paper26.2 |

## Observation

`develop@51e134d` の `WorldSnapshotTraversal.SourceStamp` は `fileKey`・`size`・`lastModifiedTime` だけを照合する。PR #284のテストではチャンク値100の読取後、同じ12 KiBの合法NBT（値900）へ置換し、mtimeを戻すと `traverse()` が成功してしまう。ファイル内容の不一致が検知されないことをJDK21で再現した。

## Unresolved / non-goals

- ダイジェストの前後一致は**読み込まれた各ファイルの内容不変性に対する強化**であり、ワールド全体の一時点整合、ロード中Entity、外部プラグイン、非同期書込の排他を証明しない。
- 追加の全ファイルハッシュ読取のI/O・遅延について本番サイズの実測が必要。無制限な並行スキャンやメインスレッド処理を許可しない。
- 正規CAT Gate（Process Closure、Model Conformance、TDD Queue、Implementation Conformance）とJE/BE実機受入は別ステータス。
