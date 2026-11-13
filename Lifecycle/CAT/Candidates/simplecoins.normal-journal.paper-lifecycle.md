---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.normal-journal.paper-lifecycle
status: candidate
process: simplecoins.normal-journal.paper-lifecycle
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/323
  - https://github.com/surumeneco/SimpleCoins/pull/322
decision_ref: ''
refs: []
semantic_contract: markdown-v2
language: ja
---

# NormalMutationJournalのPaper起動・停止配線 Candidate

このArtifactは既存の決算失敗時の入室拒否を維持したまま、Paperのライフサイクル上で永続仕訳コンポーネントを生成、検査、終了させるための**技術Candidate**。ゲーム内で通常仕訳を発生させる機能や入室許可ではない。CAT Confirmedではない。

## 対象

| Ref | Aspect |
| --- | --- |
| startup | SimpleCoinsPlugin.onEnableと私有データフォルダーnormal-mutations |
| journal | PREPARED/EVENT_APPLIEDと単一writer lockの既存domain candidate |
| shutdown | Paper disableとjournalリソース解放 |
| admission | SettlementSnapshotPublisherが未完了の間は常にclosed |

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| journal.paper.start | 入室0人のPaper起動 | 一意の仕訳journalを開き全ファイル検証後、既存レシピを登録する | 正規Paper startup probe + disk head |
| journal.paper.failure | 不正・壊れた記録や排他獲得失敗 | 起動で無条件失敗、Bukkit shutdown、入室拒否 | 破損/二重open試験 |
| journal.paper.stop | plugin disable | 作成済みjournalをcloseしwriter lockを解放する | Paper終了と再open |
| journal.paper.noadmit | 仕訳イベント/スナップショット結合はまだ無い | 正常なjournalのopenを入室許可の証拠としない | 独立GuardのmayAdmit=false |

## 検証境界

- 起動時journalの存在・整合性検査とPaper正本の全件照合は異なる。Recordの状態を現物の確認として扱わない。
- Source/World全件、BAN/BlockLocker/WWC/WWW、通常仕訳イベント全経路、Snapshot generationと公開ゲートは未実装のままである。
- Paper本番導入・JE/BE操作・CAT正式Gateとdevelopマージは別途検証対象。
