---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.settlement.source-readiness
status: draft
process: simplecoins.settlement
source_refs:
  - https://drive.google.com/file/d/1otIcgSDNba7wIDe1bwFHCZZno5w7TWQW/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# DomainSpec: settlement source-closure readiness barrier

## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.settlement | join admissionと保存済み資産のsource closure |
| simplecoins.snapshot | durable generationとreadbackの同一性 |
| simplecoins.paper | 読取中/公開後の外部mutation通知と実機同期 |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| barrier.initial | barrier生成直後、または全sourceのstagingが完了していないとき | join admissionは閉じたまま | SETTLEMENT-BARRIER-01 |
| barrier.source-set | manifest由来の必須source IDを処理するとき | 必須集合は非空・不変。未知ID、重複完了、欠損sourceは失敗として閉じる | SETTLEMENT-BARRIER-02 |
| barrier.failure | staging中にsource失敗またはmutationが発生したとき | 試行をFAILEDにし、公開とjoinを拒否する | SETTLEMENT-BARRIER-03 |
| barrier.durable | 全source完了後に公開を検討するとき | exact generationのdurable persistとreadbackが成功・一致した場合のみ開ける | SETTLEMENT-BARRIER-04 |
| barrier.readback-failure | durable stateが欠損・破損・不一致、またはreadback providerが例外を出すとき | FAILEDとして閉じ、部分検証済み状態からのjoinを拒否する | SETTLEMENT-BARRIER-05, SETTLEMENT-BARRIER-09 |
| barrier.late-mutation | barrier開放後にsource mutationまたは遅延失敗を観測したとき | 以後のjoinを閉じる | SETTLEMENT-BARRIER-06 |
| barrier.restart | 失敗したbarrierを再開しようとするとき | 同一インスタンスを再開せず、権威状態を再照合した新インスタンスが必要 | SETTLEMENT-BARRIER-07 |
| barrier.reentrant | join admission callbackが再帰的にjoin admissionを要求したとき | nested callbackを実行せず、barrierを無効化し後続joinを拒否する | SETTLEMENT-BARRIER-08 |
| barrier.callback | source-read callbackがI/Oまたはruntime failureで終了したとき | source完了と数えずFAILEDにする | source callback fault-injection |

## 実装・検証境界

これはTask-20261101-005に基づくCAT **Draft**。source readinessの制御は、Paper loaded chunk/entityの凍結、既存プレイヤーの資産変更防止、権威source集合の発見、join listenerとの同一thread直列化、world/diskの原子的captureを証明しない。呼出元がmutationをbarrierへ通知し、captureからdurable verificationまでの物理的変更を抑止する必要がある。

このDomainSpecは規範的Process/PI/TCE、正式CAT機械Gate、Paper実機故障注入の代替ではない。現行状態はDraftであり、Confirmedへの昇格、process-closure、model-conformance、vectors、Queue生成、実機統合はnot-run。
