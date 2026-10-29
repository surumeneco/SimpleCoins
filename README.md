# SimpleCoins

Minecraft Paper 26.2 向けの独自貨幣プラグイン。開発中の **1.0.0 (未リリース)** です。

## 現在の実装

- PDCベースの9額面独立貨幣 (PAPER)、item_model、最大64スタック
- 20種類の不定形・等価両替、他のPAPERクラフトへの誤投入抑止 (手動・Crafter)
- 財布 (BUNDLE) のクラフトと染色、残高上限とGUIによる入出金の初期実装
- `/sc give <known player|online selector> <amount>`（最小枚数・永続予約）、`/sc give <online player|selector> <denomination|wallet> <count>`
- 行商人の設定可能な低確率貨幣ドロップ
- Java 26.2向け画像9種の初期生成とZIP生成。PNGファイル名の区切り記号互換・重複エラー
- UUID・連番・チェックサム付きのオフライン予約台帳、空き容量に応じた分割引渡しとplayerdata受領記録による復旧
- 資産統計の **純粋計算部分のみ**。ディスク全件決算や実行中の資産追跡はまだ存在しません。

## 画像・パック

初回起動時 `plugins/SimpleCoins/images/` に9 PNG画像を生成し、`plugins/SimpleCoins/SimpleCoins-java.zip` を生成します。表示名と同名の PNG を差し替えて再起動すると反映します。

**ZIP生成とクライアントへの実配布は別処理です。** Nexo未導入では生成したZIPをHTTP(S)で公開して `resource-pack.public-url` に設定した場合のみ、Javaクライアントへ配布します。Nexoを使用する際はこのパックを二重配布しませんが、Nexo側への画像・モデル取り込みは未接続です。Nexo不在時は `geyser/SimpleCoins.mcpack` とGeyser v2定義マッピング `geyser/simplecoins.json` も生成します。Geyserの `packs/` と `custom_mappings/` への導入、`enable-custom-content` の有効化、再起動と表示確認が必要です。現段階では自動導入しません。

## 残るリリースブロッカー

- 全領域（未ロード/オフライン/BlockLocker/WWC/WWW）の漏れない全件決算と障害時fail-closed
- ゲームイベント仕訳・統計登録、BAN種別判定・死蔵資産位置照会
- playerdata保存と台帳のクラッシュ境界の実機検証（永続予約・復旧処理は実装済み）
- Bedrock/Geyser・Nexo有無の双方でのパック生成/導入/表示検証
- GUI JE/BE、Shift・Crafter・染色・高負荷・クラッシュ時の実サーバー試験

**実機確認に合格すればそのまま正式リリースできる状態ではありません。**

## ビルド

JDK 25 + Gradle 9.2.1:

```sh
gradle --no-daemon clean build
```

CIでは未リリースJARをActions Artifactに保存します。製品仕様・Phaseと依存ゲートの正本は
[Drive SimpleCoins README](https://drive.google.com/file/d/1lLgAP9zAf8e0_Il8Mdh4HGXNr2aJ4ShC/view) を参照。

## 永続予約と安全停止

金額形式はオンラインの場合も一度予約に保存します。予約だけでは現物を生成せず、
インベントリへ入った分のみを受領記録と一緒に保存します。満杯なら残額を維持します。
台帳のチェックサム不一致・連番欠損・受領記録の巻き戻り・保存結果不一致では処理を続けずサーバーを停止します。
起動初期化の失敗も安全停止します。台帳とplayerdataは同じ時点のバックアップから復元してください。
この版から `issuance/head` が必須です。未リリースの旧試作台帳を自動移行・初期化しません。

財布のShift一括クラフトはUUID複製を避けるため拒否し、通常クラフトとCrafterでは毎回新しいUUIDを割り当てます。
財布GUIでは数字キー、オフハンド交換、ダブルクリック回収、クリエイティブ複製を拒否します。

検証範囲と未完了項目は [実装記録](docs/implementation-work.md) を参照してください。
