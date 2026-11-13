---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.live-universe-wallet-identity
status: candidate
process: simplecoins.settlement.live-sources
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/338
refs: []
semantic_contract: markdown-v2
language: ja
---

# 複数ワールド・オンラインプレイヤーのライブ財布UUID共通化 Candidate

## 対象

| Ref | Aspect |
| --- | --- |
| live-world-set | 決算対象として明示的に選択されたロード済みPaper Worldの集合 |
| live-player-set | 同一世代にオンラインであるPlayerの保管区画 |
| shared-wallet-identity | すべてのワールドとオンラインPlayerを横断する一個の財布UUID集合 |
| shared-entity-identity | すべてのWorld内に現存するEntity UUIDの衝突検査 |
| aggregate-budget | world・chunk・tile・entity・slot・player・NBT byteの実行予算 |

## 観測

既存`PaperLoadedWorldAssets.scan(World)`は呼び出しごとに財布UUID集合を作るため、複数ワールドのチェスト間で同じ財布が複製されても重複検出できない。オンラインPlayer集合も`PaperLivePlayerAssets.scanOnline`が別集合を使う。02Bは同じ物理資産の重複計上を禁止しているため、これらは単一のライブ走査に渡す必要がある。

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| universe-01 | 異なるワールドに同じ財布UUIDがある | 二重計上せずIOExceptionで走査全体を拒否する | 2 World中の実チェストfixture |
| universe-02 | ワールド内財布とオンラインPlayerの財布UUIDが同じ | 同一集合で検出し走査失敗にする | 同一wallet IDの境界テスト |
| universe-03 | 重複がなくワールドごとに別財布UUIDがある | 明示されたWorld集合の金額を合算する | 分離された2 Worldの実走査 |
| universe-04 | World/Entity UUID/slotが重複、aggregate budget超過 | 全体結果を返さず拒否する | 重複選択・予算超過 |
| universe-05 | 空のWorld集合またはオンラインPlayer集合がある | ワールド非依存のPlayer資産を捨てない | 空集合・正常結果 |
| universe-06 | ライブ保管先だけを正常に読んだ | 保存NBT・所有者/BAN・世代整合が未検証なら決算完了にしない | isCompleteSettlementSource=false |

## Boundary

ここで単一化するのは**ロード済みのPaper保管元**に限り、保存済みNBTとの財布UUID照合・所有者・保護看板・現在BAN・WWC/WWW・外部プラグイン変更・仕訳を包含しない。全ワールドの不変性は別途証明が必要で、実際の読み込み・ゲームAPIはPaperメインスレッドでのみ行う。CAT CandidateでありConfirmedへの自動昇格および参加受付開放を禁止する。
