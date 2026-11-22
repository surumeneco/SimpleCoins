---
cat_version: '0.2'
kind: work
id: simplecoins.a01.live-wcl-writer-agent-interception
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

# A01/G0 — 実WWC/WWW loaded owner writerのJVM Agent入場制御検証

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/603 |
| https://github.com/surumeneco/SimpleCoins/pull/604 |
| https://github.com/surumeneco/SimpleCoins/pull/605 |
| https://github.com/surumeneco/SimpleCoins/issues/423 |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.world-source-discovery | simplecoins.a01.live-wcl-writer-agent-interception | simplecoins.a01.writer-epoch-publication-contract;simplecoins.a01.real-wwc-www-live-owner-writer |

## Technologies
| Technology |
| --- |
| Java 25 standard Instrumentation/Class-File API; official Paper 26.2 build 134; immutable distributed WWC 2.1.1/WWW 1.1.2/WCL/WGL; isolated default-deny real Paper JVM |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.live-wcl-writer-agent-interception.md;tests/paper-wcl-services-probe/**;.github/workflows/paper-wcl-services-probe.yml | — | tests/paper-wcl-services-probe/**;.github/workflows/paper-wcl-services-probe.yml |

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

## 問題・境界

G0公開には全貨幣owner writerについて新規入場拒否と既入場排出が必要。#603により現配布WWCの`WonderfulChickenStore.save`とWWWの`WonderfulWolfLoadedIndividuals.saveAndRegister`は実貨幣の価値を変えずに帰属を更新する実到達可能なwriterと確定した。#604/#605の最終候補再確認は必要だが、公開直前への割込みを排除できない。

本Workは、ユーザーが要件不変の技術手段の選定を委任した範囲で、**JVM Agentを検証用の第一候補**にする。既存A01 Draftの意味/確定状態、本番構成、配布、G0許可、G3 ACKを変更しない。

## 検証手順

1. 既存の現配布4 JARとPaper 26.2固定試験内で、Agentは**クラス名とメソッドdescriptorを完全一致**で二つの実メソッドに限定する。各ターゲットの変換成功をCIに記録し、一方でも未計測なら全体の「正例」は失敗とする。
2. 各実体の通常の正本保存を先に完了し、WCLからowner A→Bと貨幣額不変を実測。次にAgentの閉鎖を線形化し、同一実Plugin APIを用いたowner Cへの**新規保存を拒否**する。拒否の原因・入場カウンタ・実PDC／WCL owner B不変・貨幣額・serialized ItemStack不変をすべて確認する。
3. プローブ専用でのみ、各試験直後に閉鎖を解除する。これは公開保護世代の再開や製品実装を意味しない。正常baseline、BlockLocker、各再起動、既存A05試験に退行がないことをCIで確認する。
4. 必要なら旧配布でなく**固定した現配布**のバイトコード構造／メソッド数を実測し、未知クラス、例外復帰、既入場処理、さらに下位PDC/外部保存経路の未捕捉を別途記録する。

## 継続検証 — 実書込みが入場した後の閉鎖と排出

PR #606の実到達2入口の「閉鎖後の新規拒否」は、**閉鎖直前に入場した書込みがその後で実行される**危険を検証していない。これは[#592](https://github.com/surumeneco/SimpleCoins/pull/592)がPaper nativeで反証した境界であり、WWC/WWWでは独立に実証する必要がある。今回も既存fixtureを利用し、個別test-only JVM Agentに限った検証とする。

- AgentがWWC `WonderfulChickenStore.save` とWWW `WonderfulWolfLoadedIndividuals.saveAndRegister` の実メソッドに入場した**直後、実ボディ前**をそれぞれ一度だけHoldする。Holdは明示的に武装した1回だけ、期限付きで行い、起動時通常実装には影響させない。
- Paperメインスレッドの実writerが保持されたことを別スレッドから検知する。**同じロック**で新規入場を閉じた時点でActive=1を観測し、`awaitDrain` は明示タイムアウトまで偽でなければならない。入場数0を便宜的に正例としない。
- 別スレッドからHoldを解く。閉鎖済みでも**既入場の実メソッド本文は完了し**、実PDC/WCL owner A→Bが変化すること（価値、ItemStack bytesは不変）を確認する。Agentの本物のnormal return hookを通過して初めてActive=0、`awaitDrain` 真にできる。解放ボタンだけで排出済みとしない。
- **同じ閉鎖世代のまま**、各writerの別UUID owner Cへの新規保存は拒否され、WCL/PDCのowner Bが不変であることを確認する。試験終了時にのみgateを再開し、従来のA05・BlockLocker・三段階保存復元回帰を再検証する。
- 終了できないHold、異常終了、誤順序、想定外の引き継ぎ、他スレッド実API呼出、元writerが未計測等は合格にせずfail-closed。例外時の全writer排出・非同期子処理・他writerとの同一gateを証明するものではない。
- データは隔離された公式Paper 26.2と配布WWC/WWW/WCL/WGLの固定fixtureだけを用い、main/本番VPS/製品JAR/Admission/G3 ACKを変更しない。DraftをAI自身でConfirmedにせず、issue-work検証とCIの結果を正式CAT Gateと混同しない。

## 継続検証 — 外部Plugin保存メソッドを経由しないPaper PDC直接書換え

#606/#607の正例は、実WWC/WWWの特定の上位保存メソッドをAgentで阻止する。**Bukkit/Paper PDCの直接setter/removerはこれらを通らず、ownerだけを変え得る**。この経路は、全writer閉包の証明に必須の具体的な候補であり、単なる静的JAR列挙では排除できない。

- 現配布WWCの本物の保存済みowner Bと10,000Cシュルカーを用い、上位WWC保存入口がclosedで拒否されている**同一gate区間**に、実Entityの`PersistentDataContainer.set(follow_target,...)`を直接呼ぶ。
- **負例（正しく制御されない旧経路）**：Java25 AgentがWWC/WWWの入口だけをinstrumentし、PDC下位mutatorをinstrumentしない隔離JVMで、保存メソッドに入らずowner B→Cへ書換えが到達し、WCL `readStored`が新しいownerを返すことを検査する。元のPDC値はテスト中に復元してfixture汚染を防ぐ。負例の成功はG0不成立の証明であってG0成功ではない。
- **対応実験**：別の隔離JVMで、実行中のPaperのPDC実装クラスに限定して`set`と`remove`の正確なdescriptorをJava25 Class-File APIで識別し、同じ入場・排出monitorの別入口としてinstrumentする。閉鎖中の新規PDC owner書換え・削除は本文前に拒否し、owner Bと金額/ItemStackが不変であることを実WCL Reader/PDCで検証する。
- 上位WWC writerが閉鎖前に入場し、閉鎖後に同じスレッドでPDC `set`へ到達する場合は、**入場済み親の同期的子**として下位書換えを許し、上位とPDCの両scopeが正常returnしてからのみ排出可能とする。独立した新しいPDC rootへこの許可を継承しない。
- 実行マーカー・実PDCクラス名・ターゲットclass/method/descriptor一致・旧バイパスと新拒否を必須証拠にし、実装されていない追加API、他の直接変更経路、反射・別クラスローダー・非同期continuation、Paper native保存やファイルI/Oは制御済みと解釈しない。
- この実験は既存の`Lifecycle/Works/simplecoins.a01.live-wcl-writer-agent-interception.md`の限定検証を広げるもので、新しい独立製品仕様やCAT Confirmedを作らない。実VPS・製品JAR・Admission・G0公開・G3 ACK・mainは不変。

## 継続検証 — WWWのnative装備を上位writer/PDCを通らず書き換える経路

[PR #605](https://github.com/surumeneco/SimpleCoins/pull/605)の現行実Paper A05試験では、WWWのWCL武器10,000C/収納5,000C・owner・ItemStack bytesが不変でも、Paper native mainhandだけを変更でき、古い決算候補を拒否する必要があると実証済み。#606/#607/#608で上位writerとPDC setter/removerを同じテストAgent gateへ入れた**後でも**、`EntityEquipment.setItemInMainHand`は独立書込み入口の候補として残る。

- 既存の実WWW founder/wolf・WCL readLoaded・native mainhand=10,000Cのfixtureを再使用。上位WWWの新規入場が拒否され、既入場処理が排出済みの**同じ閉鎖期間内**に、実Paperの`Wolf.getEquipment().setItemInMainHand(STONE)`を直接実行する。
- **負例**：装備APIが未計測のAgentで、WCL owner/B・武器/収納の額とItemStackが同じまま、native mainhandだけSTONEになることを確認する。実装備を元の10,000Cに戻し、元の安全なfixtureへ回復する。G0公開してよい根拠ではない。
- **対応実験**：現Paperの実装備クラス名と`setItemInMainHand(ItemStack)`および`setItemInMainHand(ItemStack,boolean)`の実bytecode descriptorを確認し、同じtest-only Agentの共通writer gateへ入場/正常returnを記録。閉鎖中の外部からの書込みを本文前に拒否し、実装備のserialized bytesとWCL証拠の不変性を確認する。今あるPDC setter/removerやWWC/WWW Writer試験・BlockLocker/3 JVM復元を維持する。
- Instrumentation失敗、直接装備変更の否認、別の`EntityEquipment.setItem`経路、オフハンド/防具・NMS内部/reflective writer、別Plugin・非同期は個別未証明として残す。単一方法の正例から装備全スロットや全24 JARのwriter閉包へ一般化しない。
- 本番Agent・製品コード・保存帳簿・G0/G3の公開・VPS・mainは変更しない。issue-work検証であり、CAT Draftを自動Confirmedにしない。

## この段階では証明しないこと

**この2つの入口だけをAgentが拒否できても、24 JARの全writer、WWC/WWWの別入口、直接PDC mutation、Paper/NMS、非同期処理、例外時排出、LIVE/SAVED同一会計世代、G3保存/ACKを証明したことにはならない。** この限定結果で`SettlementSnapshotPublisher`を有効化したり入室Guardを開放しない。原子的公開までの全入口閉包が実証されない限りG0は未完了、G3は未ACKを保つ。

## Acceptance

- 実PaperでWWC/WWWの実Pluginオブジェクトと実WCL Readerを使った両方のAgent阻止正例、変換marker、正しい保存証拠と回帰CI。
- 最初から閉鎖なしの既存保存は正常に実行され、新規入場拒否時だけ実writer本体が実行されない。
- CAT Work検査はissue-work構造のみ。正式CAT Process/Model/Implementationのpassed、製品全writer制御、実機受入、main/VPS/リリースの完了は主張しない。
