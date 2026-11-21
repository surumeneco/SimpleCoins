---
cat_version: '0.2'
kind: work
id: simplecoins.a01.real-wwc-www-live-owner-writer
status: draft
process: simplecoins.world-source-discovery
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1Py55wN7KsfMqIWfD9P2IwSDwXSzUBqeR/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
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

# A01/G0 — 現配布WWC/WWW自身のLIVE貨幣owner writer経路の実行観測

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/pull/601 |
| https://github.com/surumeneco/WhatAWonderfulChicken/blob/main/src/main/java/co/surumene/whatawonderfulchicken/service/WonderfulChickenStore.java |
| https://github.com/surumeneco/WhatAWonderfulWolf/blob/main/src/main/java/co/surumene/www/persistence/WonderfulWolfLoadedIndividuals.java |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.world-source-discovery | simplecoins.a01.real-wwc-www-live-owner-writer | simplecoins.a01.writer-epoch-publication-contract;simplecoins.external-storage-provider-integration |

## Technologies
| Technology |
| --- |
| Real pinned Paper 26.2 build134; Java25; distributed WWC2.1.1/WWW1.1.2/WCL/WGL; source-selected real plugin store writes; independent default-deny guard; WCL readback |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.real-wwc-www-live-owner-writer.md;tests/paper-wcl-services-probe/**;.github/workflows/paper-wcl-services-probe.yml | — | tests/paper-wcl-services-probe/**;.github/workflows/paper-wcl-services-probe.yml |

## Checks
| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |

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

## 独立した技術検証・対象の優先順位

既存#601の静的24-JAR監査は`GenomePayloadFiles`の不変ゲノムファイルと非同期executor等の参照を確認した。しかしゲノム`.bin`そのものは通常、P/F/Dや貨幣額のwriterではない。G0の優先検証対象は、**実行時に所有者と物理貨幣の帰属を変更できる現配布Pluginの実store経路**とする。存在可能性だけを列挙し直すのではなく、現行配布JARの実コードを起動する。

独立したloopback・whitelist・0 connected players・入室Guard全拒否のPaper JVMの**baselineだけ**で、製品/依存JARを変更せず、テスト用Pluginから次の実行を観測する。

1. WWC：実`WhatAWonderfulChicken` Pluginの**実体`WonderfulChickenStore.save`**を介して、実Chicken PDCの`follow_target`をA→Bに変更する。コンテナ型`shulker`は二つの正規5000C貨幣（計10000C）を保持。現行WCL`readStored`で保存前後のEntity UUID・World UUID・収納slot・同一serialized ItemStack bytes・10,000C・ownerのA→Bを確認する。見た目や事前手書きPDCは正本としない。
2. WWW：現行WWW`WonderfulWolfFactory`生成の実個体と`WonderfulWolfLoadedIndividuals.saveAndRegister`を介し、指揮者A→Bのみ変更。正規10000C weaponと5000C cargo、実WCL/provider/元Entity鏡像関係、`PaperWonderfulLiveCandidates.collect`による15,000Cと指揮者の変化を確認し、先に取得した独立未公開候補はAを保持したままであることを確認する。
3. 両変更中オンラインプレイヤー0、入室Guard閉鎖、Paper main-threadを確認。これは**合計金額が不変でも帰属とP/F/Dが変わる**という会計世代の必須反例を現行外部Pluginの実write pathで確認する。イベントが正しく起動し、真のWCL値・元PDCが変更された場合のみ実証とする。
4. baseline以外のpersist/reload-saved/reload及び実BlockLockerの既存56,000C全件候補・Paper JVM回帰は変更しない。例外時はPartialを捨て、任意の金額0・owner無を成功としない。

**非主張**：この試験は該当2メソッドの到達性の正例であり、運営時の自動変更頻度、全writer入口、既入場worker drain、Paper NMS・非協調plugin/OS writer、G0原子公開、G3 ACKや入室許可を証明しない。仮に同じtickで保存MCAが不変でも、LIVE owner writerを閉じたことにはならない。A01のCAT Process/PI/TCEは未承認Draftのままであり、このWorkは`issue-work/verification`で正式CAT Gateではない。

## Acceptance
- 実配布WWC/WWW/WCL/WGL/Paperの不変JAR、隔離されたbaseline JVMの実Plugin保存APIを使い、変更前後のowner・額面・serialized ItemStack同一性と元保存候補の非追随を実測する。
- 正常ケースが実Paper markerに記録され、ローカル前段CAT Work構文/Domain/Phase6/実Paperの同一PR HEAD CIがすべてGreen。
- 独立した元の全保存実体、BlockLocker、BAN、WCL復元の回帰を保持し、製品JARを変更せず、入室許可・G0/G3・main・VPS・Releaseを触らない。
