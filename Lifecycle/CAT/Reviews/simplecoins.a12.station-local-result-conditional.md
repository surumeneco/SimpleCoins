# A12: Merchant/Cartography結果取得局所ガードの条件付き実装レビュー

- 対象: 確定済み `simplecoins.currency.station-result-local-decision` と、製品 `CoinExchangeRecipes.onStationResultClick` の限定的なPaperイベント防御
- 仕様権限: [Confirmed Process](../../../docs/cat/confirmed/a12-station-local-decision-process.md) / [PI](../../../docs/cat/confirmed/a12-station-local-decision-pi.md) / [TCE](../../../docs/cat/confirmed/a12-station-local-decision-tce.md)。[ユーザー承認と統合](https://github.com/surumeneco/SimpleCoins/pull/532)
- 検証対象: [SimpleCoins PR #529](https://github.com/surumeneco/SimpleCoins/pull/529)。独立してConfirm済みの局所Processだけを採用し、従来の `simplecoins.currency.station-consumption-exclusion` は `閉世界: 偽` を維持
- CAT-TDD実行正本: `surumeneco/CAT-TDD@fd3f5c7e0646627780250dcc3fa5684d35a5f9cd`、[正規生成JUnit・Queue証拠 CI](https://github.com/surumeneco/SimpleCoins/actions/runs/44292664004)、[保存された16件の証拠](https://github.com/surumeneco/SimpleCoins/actions/runs/44292664004/artifacts/14421723744)
- 確認したPR HEAD: `c77f827eb044f4589e53431b56baf5a1a2f5f40f`。この後にレビュー記録を追加したHEADについてはCIを改めて照合する。

## 実測・検証で成立した限定範囲

1. 承認済み局所Processは、正規化済み3真偽値の全8入力組合せを4排他的TCE分岐で閉包する。正規CATスクリプトのProcess Closure・Model Conformanceが `passed`。
2. 生産コードの `stationResultShouldCancel(target,readable,marker)` は `target && (!readable || marker)` を評価し、`onStationResultClick` はMerchant/Cartography結果raw slot 2の判定に利用する。テストドライバーは製品メソッドを直接呼び、仕様由来の期待値を自作しない。
3. 正規CAT `cat_compile_v2.py --framework junit` が出力した4ケース `case.s0`〜`case.s3` はJDK25/Gradle/JUnitで全件Green。CAT `green-s0`〜`green-s3` のスクリプト所有証拠が `passed`、Queue全4件が `done`。既存のイベント処理を純粋関数として抽出したため、架空の事前Redを要求しない。
4. Paper 26.2 build 134にて、実PluginManagerへの合成InventoryClickEvent投入で結果取得時の正規・不正PDC貨幣拒否、普通の素材の非拒否、非結果スロットの非拒否を観測。上段入力読取例外を注入したMerchant/Cartography各1件を追加し、合計18ケースを確認。[製品イベントCI](https://github.com/surumeneco/SimpleCoins/actions/runs/44292663896) と [旧A12 Paperイベント境界](https://github.com/surumeneco/SimpleCoins/actions/runs/44292664063) は当該HEADで成功。
5. 取得したCATアーカイブの14 Implementation Obligationの内訳: **9 `passed` / 5 `inconclusive`**。合格9件はTCE4分岐のbehavior 4、allowed-set 4、domain semantic-map 1件。これは局所関数の挙動・生成テストの根拠に限定される。

## 未証明義務と外部責務

| 未証明義務 | 未成立理由と次工程 |
| --- | --- |
| `interface.evaluate` | Javaメソッドの単体実行は確認。CAT技術Inspectorによる公開インターフェース集合の証明は未実施 |
| `capability.closed-set` | 純粋局所判定について追加観測・作用がないことの正式な閉集合検査は未実施 |
| `cross-process.boundary.simplecoins.currency.pi.station-result-local.paper.merchant-cartography-result-event-adapter` | 実Paperでの限定的合成イベントの正例はあるが、接続クライアント・全クリック型・全第三者プラグイン・後段変更を含む完全な契約証明はない |
| `implementation-constraint.junit` | JUnit実行・XML・Queueは成功。CAT技術制約の独立Inspector証拠が未整備 |
| `implementation-constraint.java` | Java25ビルドは成功。CAT技術制約の独立Inspector証拠が未整備 |

正式 `implementation-conformance` は **`inconclusive`**。コードレビューやCI成功を使って `passed` と書き換えない。既存Confirmedの全体A12はProcess Closure未成立であり、当該局所Processの証拠によって解消しない。実クライアントのMerchant/Cartography、他Pluginが後段で解除する取消、drag/number-key/double-click、直接NMS書込み、JE/BEネットワーク、全writer世代排他、保存確定・G0/G3は別の受入条件。

## 条件付きレビュー判定

現状の変更は、既存製品コードのステーション結果操作で貨幣が通常の紙として消費される既知リスクを、正規貨幣と不正PDCも含めて **局所的に拒否する安全側の措置**。承認済みの閉世界はその純粋判定関数のみに適用し、既存A12 Processや決算・配布の全体ゲートを変更していない。システムは独立Guardによるdefault-denyのままで、`SimpleCoins` の導入だけでは入室可否・決算結果・貨幣配布を開放しない。

この限定性、CAT由来の4件Green、実Paperイベント18件、既存の広域回帰CIを条件として、**保護機能の独立部分だけを非リリースの`develop`へ条件付き統合することは妥当**と判断する。これはユーザーによる1.0.0リリース承認でも `main`・VPS配備許可でもなく、A12全体の完了宣言でもない。正式義務と実クライアント・グローバルwriter排他は残作業として追跡する。

このReviewはエージェントによる技術判断であり、Script-owned Gate/Queueの代行ではない。統合前には**最終HEADのCIを再確認**する。
