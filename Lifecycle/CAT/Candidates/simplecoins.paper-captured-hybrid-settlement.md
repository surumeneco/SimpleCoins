---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.paper-captured-hybrid-settlement
status: candidate
process: simplecoins.settlement.source-generation
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/345
refs: []
semantic_contract: markdown-v2
language: ja
---

# Paper実読取から保存＋ライブ決算候補への一回限りの接続 Candidate

## 対象

| Ref | Aspect |
| --- | --- |
| captured-paper | メインスレッドで収集した同一世代の`PaperLiveUniverseAssets.Observed` |
| saved-authority | 外部MCA/MCCとplayerdataの保存データ走査結果 |
| source-partition | 実読取済みLIVE ChunkおよびオンラインPlayerから構築した`SavedLiveSourcePartition` |
| physical-wallets | LIVE走査から保存NBTへ渡す共通の財布UUID集合 |
| owner-classifier | BlockLocker、Entity所有者、現在BAN、playerdata存否の外部により検証された解決器 |

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| paper-bridge-01 | `scanWithSources` が一回のPaper読取から金額明細を返す | 読み取ったChunk/Player IDを`SavedLiveSourcePartition`へ一意に登録する | 実読取ChunkとPlayerの自動選択 |
| paper-bridge-02 | 保存NBTを追って読む | 同じ財布UUID集合を使用し、shadow対象の保存値を重複計上しない | 保存済みplayerdata fixtureとの統合 |
| paper-bridge-03 | LIVE Chunk/Playerの金額ソースが準備済み | 追加の`liveChunk`/`livePlayer`手動申告を利用者に要求せず、`HybridLiveSourceStage`へ渡す | 200C LIVE+300C保存の統合 |
| paper-bridge-04 | 同じ捕捉内容で再決算または途中失敗後に再利用する | 一回だけ許可し、再利用は禁止 | 二重使用拒否テスト |
| paper-bridge-05 | 帰属判断に未解決・なりすまし・偽の物理IDがある | 部分値を確定せずIOException | 所有者解決失敗テスト |
| paper-bridge-06 | Paper capture中にWorld・Playerが欠ける | 空資産とみなさず読取全体を失敗させる | 既存`Observed`およびPaper検証 |
| paper-bridge-07 | 一回限りの候補が成功する | 同時点性や全ワールド/保護/BAN/仕訳の完成は主張しない | admissionは常に閉 |
| paper-bridge-08 | saved走査後にLIVE所有者分類中、新たなregionまたはplayerdataソースが出現する | 決算候補を返す前に元Manifestの保存ソース発見集合を再検証し、増減や再発見失敗を検知した場合はIOExceptionで拒否 | post-LIVE callbackによる保存ソース増加と不変control |

## Boundary

`Capture`はメインスレッドのPaper読取から作成し、保存NBTの処理は安全な別実行文脈で行う。実世界が両読取の間に変わらないことは別の世代バリアで証明する必要があり、出力は公開できない決算候補である。未検証の所有者をFreeと推定する動作を認めない。本ArtifactはCAT CandidateでありConfirmed仕様や正式Gate合格ではない。
