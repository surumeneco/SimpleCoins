# SimpleCoins

Minecraft Paper 26.2 用の通貨プラグイン（**1.0.0 開発中**）。

## 開発状況

このブランチは Domain 実装の初期段階であり、**サーバーで使用できる Paper プラグイン JAR はまだ生成しません**。PluginMain、現物貨幣、財布 GUI、資産走査、全件決算、Bedrock/Nexo 経路などは未実装です。

実装済みの独立計算処理:
- 9 種類の額面と正の金額の最小枚数分解
- 等価両替 20 パターンの契約
- 財布の非破壊的残高遷移（上限引下げ時の既存残高保持）
- P / F / D の合計と主要な集中指標（D は T から除外）
- ワールド ON/OFF の衝突時デフォルトへの復帰
- PNG 画像名の区切り文字互換と重複・パストラバーサル検出

## ビルド・テスト

Paper 26.2 のビルド環境を想定し **JDK 25 + Gradle 9.2.1** を使用します。

```sh
gradle --no-daemon clean check
```

Domain 層は Paper API を使わないため、Java 21 でも独立検証できます。

```sh
mkdir -p out
javac -d out $(find src/main/java src/test/java -name '*.java')
java -ea -cp out co.surumene.simplecoins.domain.DomainContractCheck
```

仕様正本: [Google Drive SimpleCoins 文書入口](https://drive.google.com/file/d/1lLgAP9zAf8e0_Il8Mdh4HGXNr2aJ4ShC/view)。
実装順: 同フォルダの `05_1.0.0_実装計画.md` Phase 0〜8。
