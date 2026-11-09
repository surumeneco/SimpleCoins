# SimpleCoins Paper起動ガード — 技術検証候補（未導入）

## 目的と限界

`SimpleCoins.jar`または独立した`SimpleCoinsGuard.jar`が存在しない、改変されている、`plugin.yml` が不整合、またはPaperのホワイトリストが全員拒否になっていない場合に、**Paperを起動せず失敗**する外部プロセス前提の補助ガードである。`JavaPlugin` がロードされる前に、そのプラグイン自身はログインを拒否できないため、起動スクリプト側に検証を置く。

これは利用者が既に指定した「決算中はOP含め誰も入れない」条件への、**実装候補**である。本番運用方式の最終確定ではない。ホワイトリストの既存登録者を一切変更しない。安全に起動できるか検査して、不一致なら起動を拒否する。プラグインがクラッシュした後の全経路、既に起動したPaperのホワイトリスト操作、プロキシ・Geyser経由の例外には独立の検証が必要。

## 前提

- Paper 26.2用の正式に検査済みSimpleCoins JARと独立したSimpleCoinsGuard JARをサーバー直下の`plugins/SimpleCoins.jar`・`plugins/SimpleCoinsGuard.jar`へ配置し、それぞれのSHA-256を記録する。両プラグインの関係は現在のDraft PR #311で検証中であり、正式採用には別の受入が必要。
- `server.properties` に `white-list=true` と `enforce-whitelist=true` をそれぞれ**一度だけ**設定する。
- `whitelist.json` は`[]`とする。既存のプレイヤーを無断で削除することはなく、管理者が退避・切替を承認した後に実施する。
- JARの配布バージョン、Paper本番サーバー構成、監視サービス、`systemd`の起動方式は本番導入前に確定する。
- `bash`、`sha256sum`、`python3`、`flock`が利用可能である。

## 使用例（導入前のコピー先・設定を読み替えること）

```bash
sha256sum /opt/minecraft/server/plugins/SimpleCoins.jar
sha256sum /opt/minecraft/server/plugins/SimpleCoinsGuard.jar
bash tools/paper_start_guard.sh \
  /opt/minecraft/server \
  <SimpleCoins.jarのSHA-256・64文字> \
  <SimpleCoinsGuard.jarのSHA-256・64文字> \
  -- java -Xms2G -Xmx6G -jar paper.jar --nogui
```

`systemd`では、既存のPaper `ExecStart` と置換する**検討対象**であり、この資料作成だけで本番の `systemd` を変更しない。ハッシュと実際に起動するJARの対応を確認すること。

## 合格条件と安全境界

- いずれかのJAR欠損・SHA違い・記述子不一致・SimpleCoinsからGuardへの必須依存欠損・許可登録者の存在・`server.properties`不一致・同時起動は、すべて `START_GUARD_DENIED` を返し Paperを起動しない。
- 合格時は `START_GUARD_PASSED` を記録し、指定されたPaperコマンドを置換起動する。両JARのSHA-256だけでJARが正しい貨幣処理を実装済みとは証明できない。
- 全件決算と通常仕訳監視が稼働していない現時点では、**ホワイトリストもプラグイン入室ガードも解除しない**。従って、起動ガードの合格は遊べる状態を意味しない。
- Paperサーバーが停止しても `white-list=true`を維持し、次回起動のたびに再検証する。プラグインが手動で停止された場合はサーバー停止を要求するが、異常停止全経路の保証は別試験。

## 検証とリリース状態

オフラインテスト `python3 -m unittest discover -s tests -p test_paper_start_guard.py -v` で、正常起動条件と欠損・改変・不整合の17ケースを検査する。**この単体テストは本番Paper・Geyser・JE/BE実機の受入証拠ではない。**

正式な採用条件は、利用者がホワイトリストを空にする運用方式を認め、Paper実環境での起動・接続拒否・決算成功後の解除・監視喪失後の再拒否が検証されること。未承認のまま本番へ適用しない。
