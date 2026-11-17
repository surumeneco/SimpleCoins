---
cat_version: '0.2'
kind: work
id: simplecoins.a00.paper-launch-preflight-spike
status: draft
process: simplecoins.settlement.startup-admission-protection
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1fSsHEn1tFX_DvnzxDK3t5FjDrIuvMrnt/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/312
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: verification
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# A00/G0 — Paper起動前の独立ガード実現性検証

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/312 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/tools/simplecoins_preflight.py |
| https://github.com/surumeneco/SimpleCoins/blob/develop/admission-guard/src/main/resources/plugin.yml |
| https://github.com/surumeneco/SimpleCoins/issues/423 |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.settlement.startup-admission-protection | simplecoins.a00.paper-launch-preflight-spike | simplecoins.settlement.default-deny;simplecoins.world-source-discovery |

## Technologies

| Technology |
| --- |
| Bash; SHA256; Python3 standard library; flock; zipfile; JDK25/Gradle9.2.1; official Paper plugin.yml artifact layout; CAT pinned issue-work structural validation |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a00.paper-launch-preflight-spike.md;tools/paper_start_guard.sh;tests/test_paper_start_guard.py;.github/workflows/a00-paper-launch-preflight-spike.yml | — | tools/paper_start_guard.sh;tests/test_paper_start_guard.py;.github/workflows/a00-paper-launch-preflight-spike.yml |

## Checks

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| a00.launch.preflight | tests | exit-zero | @repo:SimpleCoins | python3 -m unittest discover -s tests -p test_paper_start_guard.py -v | missing-plugin;digest;descriptor;whitelist;operator;lock | tools/paper_start_guard.sh;tests/test_paper_start_guard.py | Python unittest status | 120 |

## Gates

| Gate | Status | Evidence |
| --- | --- | --- |
| process-closure | not-run | — |
| model-conformance | not-run | — |
| implementation-conformance | not-run | — |
| semantic-review | not-run | — |
| test-oracle-review | not-run | — |
| red-review | not-run | — |
| pr-review | not-run | — |
| ci | not-run | — |
| merge | not-run | — |
| deployment | not-run | — |
| real-use | not-run | — |

## 原要件・技術観測と対象境界

採用済み02Bは全件正本照合と仕訳監視の完了まで管理者を含め参加を拒否する。現製品`SimpleCoinsAdmissionGuard`は独立STARTUPプラグイン、`SimpleCoinsPlugin`はPOSTWORLDであり、ロード済み実行時の default-deny が前提。既存`tools/simplecoins_preflight.py`はJAR記述子と入口の存在を検証するが、実Paper開始に先立つ`server.properties`・`whitelist.json`・`ops.json`の状態検査や起動プロセスの重複防止を担わない。

旧[#312](https://github.com/surumeneco/SimpleCoins/pull/312)の独立した`tools/paper_start_guard.sh`とPython unittestを**製品非接続の実験用起動前ガード**として抽出する。指定されたJAR二つのSHA-256と記述子、`white-list=true`、`enforce-whitelist=true`、空`whitelist.json`と空`ops.json`を要求する。合格なら指定コマンドを起動し、違反なら起動しない。ファイル内容は変更せず、同時起動用lockファイルの生成と排他取得のみ行う。これらは**事前配置した別途隔離テスト環境**に対する技術条件であり、実VPSや通常本番のoperator名簿を編集してはならない。

テストは21件の隔離fixture（正常／欠損／SHA違い／記述子誤り／symlink／ホワイトリスト重複・不一致／OP迂回）を対象とする。さらに同じPR HEADから実Gradle生成の本物のSimpleCoins/Guard JARを一時ディレクトリにコピーし、SHAを得て起動前チェックを実行し、終了コードとマーカーが一致することを観察する。これは**Paper JVMを起動しない試験**であり、既存のPaper guarded bootstrap CI、JE/BE・Geyserクライアント・権限迂回・起動後の構成書換え・外部writer停止とは区別する。

## 安全性と適用の留保

- このスクリプトは実装方式の採用済み規範ではなく、**技術候補**である。ローカル検証Greenはホワイトリストを空にする運用方針・`systemd`への正式採用・デプロイを意味しない。
- ハッシュ・JAR記述子・起動前ファイル検査の**瞬間的な一致**は、チェック後にプラグインや設定が変更された場合、権限を持つ別のPaper起動経路、GeyserやProxyによる別系統の接続、OS/ファイルアクセス競合を排除しない。既存の独立AdmissionGuardを外したり、G0/G3の証明の代わりに用いたりしない。
- 既存ユーザーの登録やOP権限を削除・変更しない。空の運営名簿を実本番へ強制することは、運用上の別決定と実VPS受入が必要であり本Workの権限外。
- CATは既存設計に照らす`issue-work/verification`であり、新たなPaper仕様・Confirmed Process/PI/TCE・正式CAT Gateの成立を宣言しない。合格しても製品の全件決算、現物仕訳、配布ACK、入室開放は未完成。

## 完了条件

旧PRを丸ごとmergeせず、実験用スクリプトと実用的な回帰のみ抽出、専用CIで現在の製品JAR二つを使った成功経路／拒否経路と21ケースの独立Pythonテスト、固定CAT Work構造、Domain/Phase6の現HEAD CI成功を確かめる。限定検証単位としてdevelopへ統合してよいが、元の旧PRと正式な本番起動の判断は区別する。実VPS適用・Ops名簿変更・startup時の認可制約変更をこのPRで行わない。
