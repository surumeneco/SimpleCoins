---
cat_version: '0.2'
kind: work
id: simplecoins.a01.writer-epoch-publication-contract
status: draft
process: simplecoins.settlement.writer-epoch-publication
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1Py55wN7KsfMqIWfD9P2IwSDwXSzUBqeR/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: specification
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# A01/G0 — 全writer世代から原子公開するための規範境界

## Evidence
| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/pull/471 |
| https://github.com/surumeneco/SimpleCoins/pull/510 |
| https://github.com/surumeneco/SimpleCoins/pull/559 |
| https://github.com/surumeneco/SimpleCoins/pull/560 |
| https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view |

## Scope
| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.settlement.writer-epoch-publication | simplecoins.settlement.writer-epoch-publication;simplecoins.settlement.writer-epoch-publication.pi.authorize;simplecoins.settlement.writer-epoch-publication.tce.authorize | simplecoins.world-source-discovery;simplecoins.external-storage-provider-integration;simplecoins.grants.paper-save-commit |

## Technologies
| Technology |
| --- |
| Java25; Paper26.2; fixed CAT-TDD; real Paper/Paper-native and 24-plugin writer evidence; JUnit after approval |

## Repositories
| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.writer-epoch-publication-contract.md;docs/cat/drafts/a01-writer-epoch-publication-*.md | — | — |

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

## 非重複の技術作業と停止条件

既存PaperSavedSourceDiscovery.observedはManifest/world-setの局所証拠だけで、PaperCapturedHybridSettlementは未公開LIVE/SAVED候補、SettlementSnapshotPublisherは呼出元の全writer静止を証明しない。SimpleCoinsPluginはAdmissionBarrierをBLOCKEDのまま保持する。これらを改修前にG0公開判定の状態空間を区別し、今回のDraftでは製品コードを変更しない。

#470/#471はディスクhash不変でもLIVE貨幣が変わる反例。#505/#510はInvSee専用拒否を通らないPaper-native保存とACK前の書換え反例。#559はWorld登録ABAだけの検出、#560はBlockLocker LIVE一個の正本分類。静的24-JAR roster、0人、World登録の不変、再readbackは全writer静止を証明しない。

### writer閉包の証明単位と実装に渡す証拠（技術案、規範未承認）

`PI.新規writer入場拒否実効`／`PI.既入場writer排出完了`を呼出側が任意の`true`で満たしたと見なす実装は禁止する。**各真偽を成立させる観測可能な証拠の集合**を別に保持し、下記を検証したうえでのみ真を導出する方式を候補とする。対象入口が未知・証拠発行主体が不明・発行後に登録集合が変化した場合は偽とする。証拠型・取得APIの選択は実装検証段階に残し、現DraftのPI/Process/TCEを自己確定しない。

| 検証対象 | 必須の証拠 | 現時点の到達証拠と不足 |
| --- | --- | --- |
| 入場対象の閉世界集合 | 稼働中のPaper本体、実際に有効な各PluginのCodeSource/版と、外部コード・非同期worker・直接ファイルwriterを含む実際の資産変更入口一覧 | 24 JAR静的一覧・loaded Plugin/CodeSource観測はあるが、全入口の到達性・未知コードの排除は未証明 |
| Paper nativeのplayerdata保存 | `CraftPlayer.saveData`だけでなく`PlayerDataStorage.save(Player)`等の独立入口を識別し、各入口の入場拒否・既実行排出を別々に観測 | #580〜#582で2入口の**隔離Agent技術実験**が成功。導入済み全writerへの適用・全入口網羅・G0使用は未証明 |
| InvSee++等のPlugin保存 | イベント発火前に入場したFuture、無通知保存、Paper内部への委譲経路それぞれの起動・終了と、閉鎖後の拒否を実測 | #502の既知Future排出と#505/#510の迂回反例は別証拠。InvSeeイベント取消のみを全排他と見なさない |
| LIVE World/Chunk/Entity・外部収納 | main threadとasyncの双方で実貨幣・財布・保護・ownerに影響する書込みを、閉鎖開始より前に入場した実行中処理も含めて排出 | #470/#471のLIVE増額は保存ファイル検査を通り抜ける。WWC/WWW/WCL、WorldEdit等の全実到達入口は未閉包 |
| SAVEDファイル・playerdata・BAN・Journal | 同じ会計世代における選択ソース・保存操作・外部BAN情報取得のread/write境界と、閉鎖前後の世代識別 | ContentWitness・ServerLoad後discovery・World登録ABA検出は局所証拠でありwriter排他ではない |
| 候補公開から通常仕訳への引継 | 単一の閉鎖世代IDで候補の全入力を参照し、公開の最後まで閉鎖を維持。解放時点でJournal監視が継続し、書込み受理が重複・欠落しない | `SettlementSnapshotPublisher`は原子的置換の部品であり、writer閉包・Journal引継を呼出元の正当な証拠なしに補完しない |

検証時は、**閉鎖要求→新規入場阻止→既入場排出→同じ世代で全候補取得→最終原子公開→監視下で入場再開**を一つの可観測なトランザクションとして扱う。途中に未捕捉の入口が一つでも残る、対象writerの排出を証明できない、世代が変わる、Bukkit/外部Provider/Journalが途中失効する、またはタイムアウトする場合、公開・入室許可は行わない。停止直後の単発hash照合や閉鎖中のイベント取消は不足の代用にならない。G3の永続ACKには別途**同じplayerdata最終保存からACKまで**閉鎖する証拠が必要で、G0世代証拠を自動転用しない。

この方式で実到達するすべてのwriterを捕捉できるかは**まだ未判定**。単なるJVM Agentの2フック正例を全閉包に昇格させず、サーバーfork／起動時Agent／既存Pluginの協調化等の導入形態変更を採用する前に、実構成と費用・変更範囲を確認する。現行のPaperプラグインのみで全writerを実効的に閉じられないことが具体的に確定した場合だけ、選択肢をU01へエスカレーションする。

このWorkはG0のCAT Process/PI/TCEをDraftとして明示し、人間の意味承認・正式CAT Gate後にのみ実装へ進む。仕様保証を変更せず技術方式を評価する限りユーザーの新規仕様判断を先取りしない。24 JAR全writer閉包が実装不能と実証された場合だけ、残る入口と保証変更／サーバ構成変更／非公開維持を整理してエスカレーション。main、VPS、配布、Admissionに触れない。
