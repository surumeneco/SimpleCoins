# A16: Implementation Conformance の条件付きレビュー

- 対象: `simplecoins.presentation.currency-image-selection` の、名称変更時に対応PNGが存在しない場合の額面別標準画像選択
- 仕様権限: [Confirmed Process / PI / TCE](../../../docs/cat/confirmed/a16-image-fallback-process.md)、[利用者承認済みDrive 01](https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view)
- レビュー対象: [SimpleCoins PR #526](https://github.com/surumeneco/SimpleCoins/pull/526)、製品の `JavaCurrencyPack.build` およびソース由来JUnit
- CAT正本: `surumeneco/CAT-TDD@fd3f5c7e0646627780250dcc3fa5684d35a5f9cd`。現行技術ルーティングでは `Java` と `JUnit` を個別指定する
- 実行根拠: [PR #526 CAT JUnit CI](https://github.com/surumeneco/SimpleCoins/actions/runs/44186048206)、[16点の正規CAT証拠](https://github.com/surumeneco/SimpleCoins/actions/runs/44186048206/artifacts/14374207068)

## 実行済みで判定できる範囲

- Confirmed仕様のProcess ClosureおよびModel Conformanceはそれぞれ機械判定`passed`。有限な選択規則s0〜s3から導出したJUnit4件はGreenで、正規CAT Queueの4件はスクリプト所有の証拠により`done`に遷移した。
- 新しい振る舞いs3は変更前の本物の製品コードで期待した理由のRedを確認済み。旧来から成立していたs0〜s2に人工的Redを作っていない。
- ソース・JUnit観測境界は**リソースパック候補生成**に限られる。内蔵PNG、カスタムPNG、曖昧PNG、破損PNGの選択に関するJE/BEアーカイブのバイト列検査は成功。貨幣のPDC、額面ID、公開Admission/決算/配布は本PRで変更しない。

## 機械判定が残す不確実性

正式 `implementation-conformance` は **`inconclusive`** であり、次の項目はGreenと同値ではない。

| 未証明義務 | レビュー判断・範囲 |
| --- | --- |
| `interface.select` | Driverの観測を通じて選択結果を検査したが、外部から見える全インターフェース集合の技術Inspector証明は未実施 |
| `capability.closed-set` | 対象局所の選択は確認したが、実行時の追加観測・作用が存在しないことを完全証明していない |
| `cross-process.boundary.simplecoins.presentation.pi.image-selection.simplecoins.presentation.currency-pack` | JE/BE ZIP候補の生成は確認。クライアント配布・Geyser/Nexo/実端末の統合境界はG2/A17の別ゲートであり未検証 |
| 実装技術制約 | Java 25/JUnitのテスト実行・Paper向けCIの成功は確認。CATの個別技術Inspectorが未提供で、正式義務は未証明 |

## 条件付きレビュー判断

上記の`inconclusive`を`passed`へ読み替えず、有限な画像選択の振る舞いがConfirmed仕様と矛盾したという証拠も現時点で得られていない。対象実装は決算・配布・Admissionを操作しない**独立したパック候補生成の修正**なので、現行プロジェクトの独立単位を段階統合する方針において、**非公開の`develop`への条件付き統合は技術上許容できる**と判断する。

これはCAT全体適合・G2クライアント描画・1.0.0正式受入・`main`配備の承認ではない。残存する4義務は本来の技術Inspector/実統合を備えた工程で追跡し、G0/G3のfail-closedを維持する。本Reviewは条件付きagent判断であり、CATのスクリプト所有証拠・Queue・Confirmed仕様・実装適合`inconclusive`を編集しない。
