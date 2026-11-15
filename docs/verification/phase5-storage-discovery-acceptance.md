# Phase 5 保存済み資産走査：受入検証契約

## 対象と正本の境界

本書は、SimpleCoins 1.0.0の保存済み資産走査について、**何を検証し、何が揃うまで決算・統計を公開してはならないか**を定義する実行用の検証計画である。現行の実装仕様やCAT Artifactを新たに確定させる文書ではない。

- 現在の要求・作業：Drive 02Bの全件正本照合と[Issue #423](https://github.com/surumeneco/SimpleCoins/issues/423) A01/A03、[Issue #3](https://github.com/surumeneco/SimpleCoins/issues/3)。全対象の有界走査・保存済み/LIVEの一意性・未読取時のfail-closedが受入範囲。旧TaskQueueは現在の担当や実行順を決めない。
- 実装・検証の照合先：対象HEADの[`WorldStorageDiscovery.java`](../../src/main/java/co/surumene/simplecoins/domain/WorldStorageDiscovery.java)と[`PaperSavedSourceDiscovery.java`](../../src/main/java/co/surumene/simplecoins/paper/PaperSavedSourceDiscovery.java)、関連PR/CI。**古いPR #34の状態を現在の実装状態とみなさない**。
- 本書の検査はCATのProcess/PI/TCE、機械Gate、スナップショット整合、経済決算の受入を代替しない。各検査結果は実際に使用したコミットSHAと実行環境に紐付ける。

## 検証の単位と判定

| ID | 対象 | 入力・操作 | 期待する観測 | 実行区分 |
| --- | --- | --- | --- | --- |
| SD-01 | ワールド発見 | Paper/Multiverseが公開する実ディメンションpathと共通level rootを列挙 | ワールド名や固定`world/DIM*`の推測に頼らず、ディメンションと独立したプレイヤーデータの両方を閉包する | 実機統合 |
| SD-02 | ディメンション | `region/`、`entities/`、`DIM-1/`、`DIM1/`、`dimensions/<namespace>/<path>/`にMCAを配置 | 対象ルートの正規ファイルを漏らさず候補に含める | 自動 + 実機 |
| SD-03 | 除外と独立データ | 除外ワールドのregion/entityとPaper 26.2共通`players/data`（旧`playerdata`も含む）のUUIDデータを配置 | 対象外ワールド資産は除外する一方、ワールド非依存のプレイヤーデータを共有rootから一回だけ検査する | 自動＋Paper |
| SD-04 | 別名・重複 | 同一UUIDの複数playerdata、重複ワールドルート、同一実ファイルのハードリンク、非正規座標/UUID表記 | いずれも権威ある単一ソースとして採用せず、曖昧性をエラーにする | 自動 |
| SD-05 | 非正規ソース | 必須ファイル位置のディレクトリ、必須ディレクトリ位置の通常ファイル、シンボリックリンク、欠落/削除済みソース | 正常な空データと区別し、部分結果を公開せず異常を報告する | 自動 + 故障注入 |
| SD-06 | 有界性 | 無関係なファイルや除外ワールドも含む多数の訪問対象、極小のvisit budget | 訪問数上限で失敗し、途中までのmanifestを有効な結果として返さない | 自動 |
| SD-07 | 保存済み/ロード済み照合 | 同一Entity/BlockEntity/収納スロットがディスクとメモリの両方に存在 | 物理資産を一回だけ計上し、スナップショット時点が不明なら決算を停止する | 実機統合 |
| SD-08 | 更新競合 | 走査中に保存ファイルを更新・削除、またはチャンクをロード/アンロード | 一貫性を証明できない場合は再試行または停止し、成功した決算として公開しない | 故障注入 |
| SD-09 | 外部収納 | BlockLocker、WWC/WWW/WCL等の所有者・収納情報が未取得または不完全 | 未取得をFREE/0として集計せず、該当する外部連携の契約に委ねてUNKNOWNを維持する | 実機統合 |
| SD-10 | 公開境界 | SD-01〜09およびSD-11の一部が未実施・失敗・不確定 | 統計・仕訳・決算結果を「全件完了」として公開しない | 統合 |\n| SD-11 | Paper 26.2共有playerdataバックアップ | 共通`players/data/UUID.dat_old`だけが存在し`UUID.dat`が無い状態でguard起動 | 孤立バックアップをゼロ資産とみなさず探索失敗・参加拒否を維持・安全停止。旧pathや正常起動だけでは代替しない | JUnit＋Paper故障注入 |

`SD-05`の具体的な拒否条件（例：祖先ディレクトリのsymlink、manifest生成後の物理ソースの変化）はCAT Draft候補であり、個々の意味論が未確定のまま実装Greenを許可する趣旨ではない。観測と採否の判断を分離する。

## 実行プロトコル

1. **固定対象**：対象Git SHA、Java/Paper版、ワールド構成、WorldScope、走査上限、使用プラグインの版を記録する。テスト対象がPRのHEADなのか`develop`なのか明記する。
2. **独立フィクスチャ**：本番ワールドに故障注入しない。別の一時ワールドまたはコピーにregion/entity/playerdataを用意し、ファイルの物理同一性（symlink/hardlink）も検査する。実ワールドを変更するテストは停止状態とバックアップを前提に別途許可を得る。
3. **正常系**：正規の各ディメンション、正規UUID、独立した複数ワールド、除外ワールド、外部チャンクsidecar（`.mcc`は親MCA経由）を検査する。
4. **異常系**：SD-04〜06の失敗を、単なる非計上ではなく**例外・失敗状態**として観測する。Redテストが存在する場合、想定失敗と他の回帰失敗を分ける。
5. **整合性**：manifest取得だけでは資産の全件性を証明しない。実機では保存済みデータの読取、ロード済み状態との照合、外部提供元の完全性、スナップショット固定、決算の公開制御まで別々に証拠を残す。
6. **判定**：各IDを`passed / failed / not-run / inconclusive`で記録し、対象HEAD、コマンド、実測結果、ログまたはテスト名を添える。全件性が不明なまま`passed`にしない。

## 現行実装と未充足領域

PR #34の`WorldStorageDiscovery`は、呼出側が渡したルートを有界に走査して候補manifestを返す**発見器**である。MCA内部のNBT/BlockEntity/Entity/ドロップを解析せず、Paper/Multiverseのワールド一覧の完全性、ロード済み状態との照合、時点一貫性、決算公開可否を証明しない。したがって、単体JUnitがGreenでもSD-01、SD-07〜10は未実証である。

現在の回帰作業は[PR #34](https://github.com/surumeneco/SimpleCoins/pull/34)を基点とし、物理同一性（[#65](https://github.com/surumeneco/SimpleCoins/pull/65)、[#67](https://github.com/surumeneco/SimpleCoins/pull/67)）、保存元形状（[#66](https://github.com/surumeneco/SimpleCoins/pull/66)）、manifestソースの存在（[#71](https://github.com/surumeneco/SimpleCoins/pull/71)）、走査順序/予算（[#74](https://github.com/surumeneco/SimpleCoins/pull/74)）、symlink境界（[#76](https://github.com/surumeneco/SimpleCoins/pull/76)、[#77](https://github.com/surumeneco/SimpleCoins/pull/77)）を独立検証している。Draft Redは欠陥再現の証拠であって、修正済みの証拠ではない。

## 引渡し条件

- 独立した完成単位はCATの該当Gateとテストを満たしたうえで`develop`へ統合し、対象HEADを読み戻す。
- Paper実機でSD-01〜10の必要部分を確認するまでは、Phase 5の保存済み全資産走査を完成扱いしない。
- 不明な所有権・欠損したソース・読取不能・スナップショット不整合は、ゼロ資産の証拠ではない。実行できなかった検査は未実行として残す。
