# SimpleCoins Phase 7 — JE/BE・Geyser・Nexo 実機受入手順

status: **verification-plan / not-run**  
対象: `TASK-20261101-006`（運営照会・オフラインgive・配布検証）  
実装参照: [SimpleCoins PR #2](https://github.com/surumeneco/SimpleCoins/pull/2)（Draft）。本書は未リリースの実装・予定動作を検証する手順であり、機能の実装済み・実機合格を宣言しない。

## 実施条件と安全境界

1. 本番と分離したPaper 26.2互換の検証サーバーを用意し、Java 25、SimpleCoinsの**実際のJAR SHA-256**、Paper/Geyser/Nexoの正確な版を記録する。対象の未リリースPRを検証する場合はビルド元コミットを固定する。依存プラグインが未実装・未導入なら、そのケースを`blocked`とし代替結果を合格としない。
2. テスト前にworld、playerdata、plugins/SimpleCoins、Geyser関連データを一組としてバックアップする。異常停止・復旧試験は検証サーバーのみで行い、実データを使用しない。
3. クライアントはJEとBEを別々に用意する。BEはGeyser-Spigot同居構成と外部Geyser構成を混同しない。キャッシュの影響を識別するため、初回接続・再接続・パック変更後を分けて記録する。
4. 各ケースの結果は`pass / fail / blocked / not-run`のいずれかとし、`pass`にはログ・実機画面・実ファイル・コマンド結果等の再確認可能な証拠を添える。JUnit/CI成功を実機合格へ転記しない。

## 検証マトリクス

| ID | 条件と操作 | 期待する確認点 | 必須証拠 |
| --- | --- | --- | --- |
| JE-01 | Nexoなし、Java用ZIP生成、HTTP(S)公開URLを設定してJE初回接続 | 配布要求・受諾・モデル/9額面画像の表示。ZIP生成のみで配布済みとしない | ZIP hash、URL到達結果、JEクライアント画面、Paperログ |
| JE-02 | JEでパック画像を変更し再起動・再接続 | 変更後のモデルが表示され、旧キャッシュとの混同がない | 変更前後のZIP hash、クライアント表示 |
| JE-03 | Java用公開URL未設定 | ZIP生成とクライアント配布を区別でき、未配布を成功としない | 設定抜粋、クライアント結果 |
| NX-01 | Nexo導入状態で起動・再起動 | SimpleCoins独自ZIPの二重配布がない。Nexo画像/モデル統合は別途実装・検証対象 | Nexo/Paperログ、実際の配布一覧 |
| NX-02 | Nexoの有無を切り替える | SimpleCoinsが管理した未変更ファイルのみ扱い、第三者ファイルを破壊しない | 変更前後のファイルhashと配置一覧 |
| BE-01 | Nexoなし、同居Geyserを起動前に設定 | `geyser/SimpleCoins.mcpack`と`geyser/simplecoins.json`の生成、`packs/`・`custom_mappings/`配置、Geyser v2 mapping読込 | ファイルhash、Geyser設定とログ、BE画面 |
| BE-02 | `enable-custom-content`無効/有効を切替 | 無効時の表示と有効時の表示を区別し、表示されない状態を配布成功としない | 設定差分、BE画面、Geyserログ |
| BE-03 | Geyserを外部プロキシに分離 | 生成物の手動配置が必要な構成として扱い、同居時の自動配置と混同しない | 配置先・hash、外部Geyserログ、BE画面 |
| BE-04 | 既存の同名・改変済み管理対象ファイルを用意 | 無断上書きしない。異常をログに残し、安全に停止/保留する | 前後hash、ログ、再起動結果 |
| BE-05 | パックZIPとmappingの片方だけが公開失敗する故障注入 | 新旧の不整合を成功と扱わず、クライアントへ混在世代を提供しないことを確認 | 前後2ファイルhash、故障ログ、BE結果 |
| ADM-01 | 権限あり/なしの管理者・一般利用者で残高/死蔵資産照会 | 権限外の照会を拒否し、許可された照会だけが対象/理由/位置を返す | permission設定、コマンド結果、監査ログ |
| GIVE-01 | オフライン既知UUIDへgive予約、再起動後に初回参加 | 同一予約の二重現物化がなく、受領記録と永続台帳が一致する | 台帳/受領hash、前後inventory、ログ |
| GIVE-02 | 満杯inventory・再接続・強制停止を各別に試す | 未配布残量が失われず、同一予約が二重発行されず、未整合時はfail-closed | 保存データ、再起動/停止ログ、再試行結果 |
| GIVE-03 | 他者/不正な対象指定と権限なしgive | 無権限実行・対象取り違え・不正金額を拒否する | コマンドとpermission、台帳の非変更証拠 |

## 実施記録の最小フォーマット

各ケースにつき `case_id`、`status`、`tested_at`、`server_commit`、`jar_sha256`、`paper_version`、`geyser_version`、`nexo_version_or_absent`、`client_edition_version`、`configuration_ref`、`evidence_refs`、`observed_result`、`limitation` を保存する。ログや画像にはトークン、IP、プレイヤーの私的識別情報を含めない。

## 現在の未検証境界

本書作成時点で上記実機ケースは**すべて not-run**。特に統計公開、管理照会、Nexo統合、Geyserの実配布、クラッシュ後の永続予約とplayerdata同期は、部分実装やDraft PRの存在だけでは受入済みにならない。実機で再現不能なケースは`blocked`とし、必要な実装・テスト環境・仕様判断を別途記録する。 `develop`への文書統合は、プラグイン実装の受入・リリース承認を意味しない。
