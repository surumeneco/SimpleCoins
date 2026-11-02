# Phase 7: JE / BE / Nexo 配布・表示の実機受入手順

> 状態: **未実施の実機受入手順**。テスト実行・配布成功を記録した文書ではない。対象は未リリースの SimpleCoins 1.0.0 開発版であり、本番投入・main マージの承認ではない。

## 対象と証拠境界

- ソース基準: `feature/initial-domain-20261027` commit `01a06f9f0fb78c8f941f2aa2e1c3df5e8951d153`。実行時には**実際に配布するJARのcommit・SHA-256**を記録し、基準と差があれば手順の適合性を再確認する。
- `SimpleCoinsPlugin.onLoad` は Nexo が**検出されない**場合に Java ZIP と Geyser 用パック/マッピングを生成し、Geyser-Spigot がある場合はそのデータフォルダに管理対象ファイルを配置する。Nexo が検出された場合、SimpleCoins は自前のJavaパック配布を行わず、既存の自己管理 Geyser 出力を除去する。**Nexo 側へのアイテム定義の自動インポートは、このコードからは確認できない。**
- `PackDelivery.onJoin` は `resource-pack.public-url` がHTTP(S)のときのみ、生成ZIPのSHA-1を付けてJavaクライアントへ送信する。URLが空なら送信しない。**URLを設定しただけではZIPの公開・受信を証明しない。**
- Geyserの外部設置・独自配布経路は本実装の自動配置対象外。Geyser-Spigotのプラグインデータフォルダを前提とする。

## 実施前条件

1. **隔離された検証用Paperサーバー**を用いる。実サーバーの `world/`、`playerdata/`、`plugins/SimpleCoins/issuance/`、Geyser管理ファイルに直接変更を加えない。検証用データと停止時バックアップを用意する。
2. Java/Paper/Geyser-Spigot/floodgate/Nexo（任意）の実バージョン、SimpleCoins JARのSHA-256、使用branch/commit、検証日時、クライアントのJE/BE版、各設定の関連値を記録する。`plugin.yml` の `api-version: '26.2'` は実行対象の互換性証明ではない。
3. 起動ログのSimpleCoins・Geyser・Nexo関連部分を保存し、`/plugins` でロード状態を確認する。SimpleCoinsの初期化失敗は `Bukkit.shutdown()` を呼び得るため、**失敗したまま発行試験を続けない**。
4. JE用URLを使う場合は `plugins/SimpleCoins/SimpleCoins-java.zip` を検証用HTTP(S)配布先へコピーし、公開URLの取得バイトが生成ZIPと同一であることを確認する。認証情報やプレイヤー識別子をログに含めない。

## 配布成果物の照合（Nexoなし）

サーバー停止後の検証用コピーで、以下のファイルとハッシュを採取する。実際のパスはGeyser-Spigotのデータフォルダ名を確認してから確定する。

| 成果物 | 期待される役割 | 判定 |
| --- | --- | --- |
| `plugins/SimpleCoins/SimpleCoins-java.zip` | JE用9額面のモデル・テクスチャ | ZIP整合・内容・HTTP配布バイト一致 |
| `plugins/SimpleCoins/geyser/SimpleCoins.mcpack` | BE用生成パック | ZIP/PNG整合・内容ID |
| `plugins/SimpleCoins/geyser/simplecoins.json` | Geyser v2カスタムマッピング | 9額面とJavaのmodel識別子の対応 |
| `<Geyserデータフォルダ>/packs/simplecoins-generated.mcpack` | Geyserへの管理対象コピー | 生成元とのバイト一致 |
| `<Geyserデータフォルダ>/custom_mappings/simplecoins-generated.json` | Geyserへの管理対象コピー | 生成元とのバイト一致 |
| `<Geyserデータフォルダ>/simplecoins-managed-resources` | 自己管理ファイルのハッシュ台帳 | 他プラグイン管理ファイルを含まない |

```sh
# 停止時コピー上で実行。実パスに置換する。
sha256sum plugins/SimpleCoins/SimpleCoins-java.zip \
  plugins/SimpleCoins/geyser/SimpleCoins.mcpack \
  plugins/SimpleCoins/geyser/simplecoins.json
unzip -t plugins/SimpleCoins/SimpleCoins-java.zip
unzip -t plugins/SimpleCoins/geyser/SimpleCoins.mcpack
# 公開URLの取得結果を別ファイルへ保存してから比較。HTTPエラーは成功としない。
curl --fail --location --silent --show-error "$JE_PACK_URL" -o /tmp/simplecoins-je-downloaded.zip
cmp plugins/SimpleCoins/SimpleCoins-java.zip /tmp/simplecoins-je-downloaded.zip
```

`JE_PACK_URL` は検証用URLとして事前に設定する。ハッシュのみでは、クライアントが実際に読み込んだことは証明できない。既存のオフラインBedrock整合性検証ツールが利用できる場合も、その実行結果と対象HEADを別途記録する。

## 実機マトリクス

| Case | Nexo | Geyser-Spigot | 接続クライアント | 確認・合格条件 |
| --- | --- | --- | --- | --- |
| DIST-01 | なし | 任意 | JE | URLからZIPを取得し、9額面の名称・外観・モデルが表示される。接続時のパック要求と受信状態を確認 |
| DIST-02 | なし | あり | BE（Geyser経由） | Geyserのcustom contentが有効で、9額面の外観・識別子・表示が一致。ログにmapping拒否がない |
| DIST-03 | なし | あり | JE + BE | 同一額面を両クライアントで確認し、財布GUI・スタック/交換の操作結果を比較 |
| DIST-04 | あり | 任意 | JE | SimpleCoinsは独自ZIPを送信しない。Nexo側の定義・配布が別途確認されるまで**未対応/未合格** |
| DIST-05 | あり | あり | BE | 自己管理Geyserファイルが正しく整理され、Nexo側のBEマッピングが別途確認されるまで**未対応/未合格** |
| DIST-06 | なし | なし | BE | Geyser経路がないため対象外。BE配布成功と数えない |

各Caseで `PASS / FAIL / BLOCKED / NOT-RUN` を記録する。**Nexo導入時の表示が未実装・未検証なら、起動成功だけでPASSにしない。** Geyserの機能設定やBEクライアントキャッシュの影響を切り分ける際は、設定値と再接続結果を残す。

## 故障時の安全条件と記録

- 管理対象のGeyserファイルが第三者に変更されている場合、`ManagedResources.replace` は失敗させる設計。所有権不明のファイルを上書き・削除して試験を通さない。
- 起動失敗、クライアントのパック拒否、JE/BE間の識別子不一致、Nexo導入時の欠落は個別のFAIL/BLOCKEDとして残す。運用中の発行・経済機能の合格とは独立に扱う。
- 証拠はCase ID、JAR SHA-256、配布物SHA-256、Paper/Geyser/Nexo版、クライアント版、設定、関連ログ、実表示の確認結果、実行者・日時を一組にする。画像には個人識別情報を含めない。
- 検証終了後は検証環境を停止し、事前バックアップへ復帰する。検証用パックの公開URLを本番に向けて設定しない。

関連: `docs/verification/phase7-issuance-journal-offline-check.md`（停止時台帳診断）、`docs/verification/phase7-paper-offline-give-acceptance.md`（Paper発行・再起動試験）。**本手順は未実施であり、実機試験結果の代替ではない。**
