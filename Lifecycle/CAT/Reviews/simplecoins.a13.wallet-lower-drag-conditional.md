# A13 Wallet lower-only drag — 条件付き実装適合レビュー

- 対象: `simplecoins.wallet.drag-isolation`（財布GUIでの下段のみの通常アイテムドラッグを許可し、上段・財布元・財布カーソル・異常セッション・事前取消を拒否）
- 規範: [利用者確定の財布仕様01](https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view)、「通常のインベントリ操作に準じる」要求および2026-11-18の具体的なA13修正指示
- 対応するConfirmed Artifact: `docs/cat/confirmed/a13-wallet-drag-{process,pi,tce}.md`。上段での複合入出金を許容する新しい仕様承認ではない
- コード候補: [SimpleCoins PR #528](https://github.com/surumeneco/SimpleCoins/pull/528)（`WalletDragPolicy` と `WalletMenu.onDrag`）
- CAT正本: `surumeneco/CAT-TDD@fd3f5c7e0646627780250dcc3fa5684d35a5f9cd`
- スクリプト所有の実行証拠: [A13 JUnit + CAT run 44197841463](https://github.com/surumeneco/SimpleCoins/actions/runs/44197841463)、[26ファイルの成果物](https://github.com/surumeneco/SimpleCoins/actions/runs/44197841463/artifacts/14379293610)

## 実測された適合範囲

- Process Closure、独立Model Conformance、6分岐のベクトルとQueue生成は正式CAT CLIで`passed`。
- 新しい下段ドラッグ許可について、旧来の全件拒否と同じ挙動を返す純Javaスタブは直接テストでRedとなり、最小の新Policy実装後は全32真偽入力でGreen。**これは実Paper GUIイベントを用いたRedではない**。
- 正規CAT Queueの全6ケースは生成されたJUnitで実行され、スクリプト所有のGreen receiptと紐付いて`done`。既存Gradle JUnitとPaper 26.2の財布**レシピ登録**smoke、Domain、Phase6 CIも同じPR HEADで成功。
- `WalletMenu`はPaperのrawスロットから下段限定・財布のホットバー元枠を算出し、既に取り消されたイベントを`ignoreCancelled=true`で再許可しない。保護範囲は上段9枠、財布自身、財布カーソル、無効セッション。金額PDC、財布残高、仕訳、Admissionなどの製品変更はない。

## 正式 Implementation Conformance の未証明項目

`Lifecycle/Works/.cat-flow/conformance/implementation-conformance.json` の正式判定は **`inconclusive`**。`passed`に変更していない。

| 義務 | 審査と限界 |
| --- | --- |
| `interface.assess` | 機械的なJava公開Interface Inspectorなし。上記はモデルの純粋判断とソースのPaper adapter観測に限り、全可観測Interface集合を網羅しない |
| `capability.closed-set` | 実行時に追加で生じる観測や作用の不存在を機械証明していない。局所Policyは副作用を持たないが、それだけでPaper側の閉集合を保証しない |
| `cross-process.boundary.simplecoins.wallet.drag-isolation.pi.assess.paper.wallet-inventory-drag-adapter` | raw↔localスロット変換をコードで採用したが、JE/BEの実プレイヤーが発生させたドラッグ・他Pluginとのイベント連鎖を実測していない |
| `implementation-constraint.junit` / `implementation-constraint.java` | Gradle/JUnit成功は実測済み。ただしCATの技術Inspectorによる完全な制約証拠ではない |

## Review判断と段階統合境界

**A13のこの小さな修正だけについて、条件付きで`develop`への段階統合を許容する。** 元の全面禁止より通常の下段ドラッグ制約を緩めるが、上段アイコンや財布本体への移動は引き続き不可であり、財産価値を変更する新しい処理は追加していない。下段のみ・元財布枠なしの操作を許可する点は元仕様の通常インベントリ操作要求に一致する。

このレビューは**機械Gateの代行・実機証明ではない**。完全なCAT実装適合、Paper/JE/BEでの実操作、全外部ライターとの統合、G0/G3のクラッシュ整合、`main`リリースと本番配備を承認しない。未検証のPaperイベント変換はA13の実機受入として追跡し、実際に不整合が見つかった場合はその結果から修正する。

レビュー責務は`inconclusive`の意味を限定して解釈することに留める。Confirmed仕様、TestModel、Queue、生成テスト、Script所有の実行証拠、正式Gate判定は変更しない。
