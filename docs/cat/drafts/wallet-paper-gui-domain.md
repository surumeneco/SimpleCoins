---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.wallet.paper-gui
status: draft
process: simplecoins.wallet
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://drive.google.com/file/d/1fSsHEn1tFX_DvnzxDK3t5FjDrIuvMrnt/view
refs: []
base_revision: e28f2a8ded6c4f61e7119db8854d6a881d11c764
semantic_contract: markdown-v2
language: ja
---

# Domain Specification Draft: SimpleCoins wallet Paper GUI boundary

This is a **Draft**, not a Confirmed CAT specification, Process Closure result, Model Conformance result, or implementation authorization. It records an issue-bound source-to-acceptance mapping for the existing but unverified Paper wallet adapter under Issue #423/A13. It does not introduce new product behavior without the appropriate decision and canonical Gates.

## 対象

| Ref | Aspect |
| --- | --- |
| simplecoins.wallet | 9額面の入出金を1行GUIに接続するPaperイベント境界 |
| simplecoins.wallet.balance | 既存数値残高の保存・上限変更・引出し契約 |
| simplecoins.coin.paper | PDCで識別する額面現物の受入・実体化（貨幣側の仕様に従う） |

## 規則

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| wallet.gui.nine | 財布の使用から入出金GUIへ到達する | 9額面に対応する9スロット1行の選択を提供する。外部インベントリとの操作結果はJE/BEごとに検証対象とする | Paper 26.2の実GUI・JE/BE integration fixture |
| wallet.deposit.value | 正規の貨幣をGUI経由で預入し、残高増加が成立する | 消費された現物価値と増加した残高が等しく、処理全体の価値総額は不変 | ItemStack/PDCとWalletBalanceを含む価値保存の統合テスト |
| wallet.deposit.reject | 不正な素材、現上限超過、計算オーバーフロー等で預入が成立しない | 貨幣を消費せず、財布残高を変更しない | 偽装紙・不正PDC・満杯・上限・long overflow fixture |
| wallet.withdraw.value | 残高の範囲で指定額面・枚数の出金が成立する | 減額と現物生成の価値が等しく、財布残高は負にならない | 任意9額面×枚数の統合テスト、価値保存照合 |
| wallet.withdraw.reject | 残高不足、現物受取先の空き不足、出力失敗で出金が成立しない | 残高のみ減らす・現物のみ増やす状態を確定させない | 満杯・中断・失敗注入（具体的なatomic手段は未決定） |
| wallet.cap.reduced | 既存の財布残高より現在の設定上限が小さい | 既存残高を保持し出金を認める。入金後残高が現上限を超える預入は拒否する | 上限640000→引下げ→出金→再入金の状態遷移試験 |
| wallet.gui.event | 左右クリック、Shift、ドラッグ、連打、切断、GUI閉鎖に伴うイベント | 同じ操作による二重消費・二重生成・価値消失を防ぐ。詳細なイベント優先順位・取り消し順は技術検証対象 | InventoryClick/Drag/Closeと競合イベントのfault-injection試験 |
| wallet.item.identity | 財布生成・染色・再染色・セーブ／リロード | 財布の固定ID、PDC残高、スタック上限1を維持する。作成素材は中央鉄1と上下左右革4、四隅空欄 | ItemMeta/PDCを保存・再取得するPaper fixture、クラフト／染色検証 |

## 確定入力と未決定の分離

- **元仕様で確定**：財布の1スタック、3×3固定レシピ、9額面GUI、価値の数値保存、初期最大640000と設定変更、上限引下げ後の出金許可、染色時の識別子・残高維持。
- **Issue #423/A13の受入対象**：JE/BEイベント互換、クリック/Shift/ドラッグ、満杯/切断、保存/再起動、実際の価値不変をテストで実証すること。
- **未決定・未確認**：現在のPDCキー/transaction補償実装は技術候補として存在するが、正式CAT Confirmed・Gate、G0に依存する永続的なwallet+現物一括確定、JE/BE実クライアント、全外部writerとの整合は未達。既存コードの有無を規範的な実装承認に読み替えない。
- **仕様上対象外**：外部からの任意のアイテム複製に対する独立した防止機構。GUI操作自身による二重増減の防止とは区別する。

## 失敗時補償の技術候補（追加Draft、未承認）

- 財布のPDCを書き換える段階と、現物貨幣を消費・支払う段階はBukkitの単一トランザクションではない。現実のプレイヤーイベント内で片側のインベントリ操作のみが失敗した場合、価値が増殖または消失する。
- 失敗時の候補対応：変更前の財布ItemStack・対象現物スロットまたはカーソル・出金先のストレージ配列を**変更前に**スナップショット化し、メインスレッド上で財布変更→現物変更を順に実施する。いずれかが例外または部分的な支払失敗なら、現物→財布の順に両方を復元する。復元自体が失敗した場合は会計の健全性を主張せず、同セッションのGUI再操作を禁止し、入室判定権限を**即時失効**させてからサーバー停止を要求する。既存ガードの次回ポーリングを待つだけで済ませない。
- 独立の故障注入試験：財布変更前・現物変更前/途中・復元途中/復元失敗の各例外を区別し、正常経路の価値保存・例外時の双方復元・復元不能時のfail-closedを観測する。
- **未保証**：インベントリ双方を一括永続化する仕組み、プロセス強制停止の途中状態、外部プラグインによる後続イベント変更、同一UUID財布の別途複製、JE/BE実操作。インメモリ復元の成功は永続仕訳・再起動整合の完了ではなく、G/Iの正規Gateを満たす必要がある。
- **バンドルの二重用途を禁止する技術候補**：財布の基底素材はBundleだが、現物貨幣はBundleのバニラ内容物として収納せず、独自GUIのPDC数値残高にのみ変換する。独自GUI外の右クリック等でバニラBundle格納処理が進まないようPaperのInventoryClick系入口を拒否する。イベントの後続プラグインや全JE/BEクライアント操作による残存経路は別途実機試験する。
- **GUI下段の通常操作**：財布GUIの9個の上段アイコン、財布自体の固定ホットバースロット、番号キー/オフハンド/ドラッグ・Shiftでの危険な移動は取り消す。一方、下段の財布ではないアイテムへの通常の左/右クリックについては、Minecraftの在庫整理を妨げない。Shiftで入金する場合は正規貨幣のみ専用トランザクションへ送る。
- **使用イベントのPaper適応**：空中右クリックではブロック操作結果が既定DENY、アイテム使用結果がDEFAULTであり、isCancelledは前者を返す。財布の使用を妨げないようアイテム使用結果を独立に判定し、アイテム使用DENYまたはブロック右クリックの保護DENYは尊重する。正規財布以外の操作は変更しない。Drive 01の確定済み使用契約の実装上の補正であり、残高・価値・GUI操作の規範変更ではない。
- **既存キャンセルを尊重する候補**：前段の保護・外部プラグインがキャンセルしたInventoryClickEventを、下段アイテムへの通常クリックという理由でWalletMenuが再許可してはならない。下段へのパススルーは、WalletMenu実行前にも未キャンセルの操作だけに限る。登録ハンドラのignoreCancelledとPaperでの前段キャンセル回帰を検証する。
- **Bundle内容物の拒否候補**：財布PDCが付いたBundleにバニラ収納物が一つでも含まれていれば、財布として読取・入出金・染色を許可しない。数値残高とバニラ収納物の二重価値管理を防ぐため、PDCの値や見た目が正しくてもBundleMetaの実内容物を検査する。空のBundleである正規財布の保存/再読込は変えない。
- **スタック上限改変の拒否候補**：PDCが正しい財布でも、ItemStackに適用される**実効**最大スタック数が1ではない個体は財布として認めない。Paperは素材デフォルトの上限1をItemMetaに明示的に保持しない場合があるため、`ItemMeta.hasMaxStackSize`の有無では拒否しない。合算・分割により同一財布UUIDと数値残高を複製できる経路を塞ぎ、通常作成・染色後は実効上限1、改変上限64は拒否する実Paper試験を行う。外部データを複製した同一UUID財布の全件重複検出はG0に残す。

## 現行実装との対応と受入未達

- `develop@e28f2a8ded6c4f61e7119db8854d6a881d11c764`の`WalletItemCodec`・`WalletCrafting`・`WalletMenu`・`WalletTransferTransaction`は、旧[PR #319](https://github.com/surumeneco/SimpleCoins/pull/319)の製品ソースと各Blob SHAが一致する。旧`WalletTransferTransactionTest`も現行と一致。
- `SimpleCoinsPlugin`は後続改訂があり、財布レシピとGUIを登録する一方、G0の全件決算・G3の配布保存・通常仕訳の本番確定が未接続である。入室ガードは閉鎖中。
- 旧PRの独立Paper試験・JUnitが過去に成功していても、現在の`develop`全体、永続会計やJE/BEの全操作の正式受入を意味しない。
- `WalletTransferTransaction`の補償は**同期・メモリ内のみ**。クラッシュで財布PDCと貨幣が異なる保存世代に落ちる経路はG0/G3および仕訳・復旧設計に残る。正式なCAT Source/Decision/Process Closure/Model/Implementation Gatesの完了宣言はしない。

## Source identity

- 確定した機能要求：`forGPT/XPlayServer/OriginalPlugins/SimpleCoins/01_貨幣・財布・レジ.md`（[Drive](https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view)）。
- 1.0.0保証／受入：`05_1.0.0_実装計画.md`（[Drive](https://drive.google.com/file/d/1fSsHEn1tFX_DvnzxDK3t5FjDrIuvMrnt/view)）。
- 旧Draft由来：[PR #319](https://github.com/surumeneco/SimpleCoins/pull/319) `Lifecycle/CAT/Draft/simplecoins.wallet.paper-gui.md`。旧TaskQueueを再開せず、現行 Issue #423/A13のDraftとして現在のコード実態に照合。
- コード基点：`surumeneco/SimpleCoins develop@e28f2a8ded6c4f61e7119db8854d6a881d11c764`。将来の変更は固定HEADに基づいて再評価する。
