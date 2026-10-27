# SimpleCoins

Minecraft Paper 26.2 向けの独自貨幣プラグイン。開発中の **1.0.0 (未リリース)** です。

## 現在の実装

- PDCベースの9額面独立貨幣 (PAPER)、item_model、最大64スタック
- 20種類の不定形・等価両替、他のPAPERクラフトへの誤投入抑止 (手動・Crafter)
- 財布 (BUNDLE) のクラフトと染色、残高上限とGUIによる入出金の初期実装
- `/sc give <online player|selector> <amount>`（最小枚数）、`/sc give <online player|selector> <denomination|wallet> <count>`
- 行商人の設定可能な低確率貨幣ドロップ
- Java 26.2向け画像9種の初期生成とZIP生成。PNGファイル名の区切り記号互換・重複エラー
- 資産統計の **純粋計算部分のみ**。ディスク全件決算や実行中の資産追跡はまだ存在しません。

## 画像・パック

初回起動時 `plugins/SimpleCoins/images/` に9 PNG画像を生成し、`plugins/SimpleCoins/SimpleCoins-java.zip` を生成します。表示名と同名の PNG を差し替えて再起動すると反映します。

**ZIP生成とクライアントへの実配布は別処理です。** Nexo未導入では生成したZIPをHTTP(S)で公開して `resource-pack.public-url` に設定した場合のみ、Javaクライアントへ配布します。Nexoを使用する際はこのパックを二重配布しませんが、Nexo側への画像・モデル取り込みは未接続です。Geyser/Bedrock版のZIP変換と独自アイテムマッピングも未実装です。

## 残るリリースブロッカー

- 全領域（未ロード/オフライン/BlockLocker/WWC/WWW）の漏れない全件決算と障害時fail-closed
- ゲームイベント仕訳・統計登録、BAN種別判定・死蔵資産位置照会
- Exactly-onceの永続オフライン配布予約
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
