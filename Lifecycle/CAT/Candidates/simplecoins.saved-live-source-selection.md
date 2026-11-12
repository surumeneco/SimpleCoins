---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.saved-live-source-selection
status: candidate
process: simplecoins.settlement.source-routing
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/337
refs: []
semantic_contract: markdown-v2
language: ja
---

# 保存済み・ロード済み正本の選択と集合閉包 Candidate

## 対象

| Ref | Aspect |
| --- | --- |
| chunk-identity | world・chunkX・chunkZ（REGIONとENTITYは異なる保存レコード） |
| player-identity | Player UUID（saved playerdataとlive Playerは同一所有先） |
| source-authority | LIVE: 読取対象のロード済み実体、SAVED: 未ロードの保存NBT |
| coverage | 選択したライブ保管先の実読取済み集合と重複排除 |

## 観測

02Bでは稼働中のロード済みチャンクとオンラインプレイヤーはメモリを正本にし、未ロード/オフラインは保存済みNBTを参照すると定める。現行の`WorldSnapshotTraversal`は保存ファイルを全件訪問し、`PaperLoadedWorldAssets`と`PaperLivePlayerAssets`はライブ資産を別に列挙するが、両者を同じ物理ソースとして選択・閉包する計画が無い。両方を単純合算すれば二重計上となる。

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| partition-01 | 同じワールド座標がロード済みかつ保存されたREGION/ENTITYに存在 | 両方の保存レコードをLIVE扱いとして保存額を採用しない | REGION/ENTITY両経路の規則テスト |
| partition-02 | 未ロード座標の保存REGION/ENTITYが存在 | それぞれ独立のSAVEDソースを一回だけ選択 | 重複した保存呼出し拒否 |
| partition-03 | 同じPlayerがオンラインかつplayerdataも存在 | 保存Player値を採用せずLIVEを選ぶ | オンライン/オフライン選択テスト |
| partition-04 | 選択したLIVE ChunkまたはPlayerを読み取り損ねる | 集合閉包を失敗し、走査完了の報告を返さない | 未訪問LIVEのテスト |
| partition-05 | 同一保存ソース・ライブソースの重複報告、選択集合外のライブ読取 | 全体を失敗として再利用不能にする | 重複・未知ソースのテスト |
| partition-06 | 集合が閉包した | あくまで入力の選択・読取範囲が揃ったという証明にとどめ、決算結果の公開権限は与えない | isCompleteSettlementSource=false |

## Boundary

本実装の責務は保存/ライブのルーティングと列挙集合の一意性に限る。実際のチャンクが走査中にロード/アンロードされた、資産が移動した、外部プラグインが変更した場合の同一時点性は、別の世代管理・凍結・仕訳の責務である。内部資産の金額、所有者、BlockLocker、BAN、WWC/WWW、全件完了、参加受付開放を自動判定せず、正式CAT ConfirmedやGateは未達である。
