---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase6.journal-nontail-integrity
status: candidate
process: simplecoins.phase6.grant-recovery
source_refs:
  - https://github.com/surumeneco/SimpleCoins/blob/e293a7b9f9ebaf1ad5559d45c0204046e1d04494/src/main/java/co/surumene/simplecoins/domain/GrantLedger.java
refs: []
semantic_contract: markdown-v2
language: ja
---

# 起動済みGrantLedgerの非末尾ジャーナル改変（code-to-spec Candidate）

## 対象
| Ref | Aspect |
| --- | --- |
| SC-JOURNAL-NONTAIL | 起動済みledgerが永続化済み非末尾レコードの消失・破損を見逃すかを検証する |
| SC-JOURNAL-HEAD | headと最新末尾が正常な場合でも、それより古い記録が失われ得る |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| SC-NONTAIL-01 | sequence 1,2を永続化後、1.grantを削除しheadと2.grantを維持 | 次の照会・発行をfail-closedし、追加発行しない | delete-old-record Red |
| SC-NONTAIL-02 | sequence 1,2を永続化後、1.grantの内容を破損しheadと2.grantを維持 | 次の照会・発行をfail-closedし、追加発行しない | corrupt-old-record Red |
| SC-NONTAIL-03 | sequence 1,2が両方健全 | pending照会とsequence 3の発行が継続できる | intact-record control |

## 未確定・検証境界
現行GrantLedger.checkHealthy()はheadと最新末尾の整合を検査するが、非末尾の継続検証は行わない。この要求は二重計上・消失防止に関する**仕様候補**であり、全レコードの毎回再読込、署名済み索引、世代固定などの実装方式や性能コストは未決定である。Redの再現は不具合候補の証拠であり、規範Confirmed・正式CAT Gate・修正Greenではない。production sourceは変更しない。
