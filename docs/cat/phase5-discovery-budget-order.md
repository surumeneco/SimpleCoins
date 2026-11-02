---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase5.discovery-budget-order
status: candidate
process: simplecoins.phase5.asset-scan
source_refs:
  - https://github.com/surumeneco/SimpleCoins/blob/b88cee565e78376f0161965cc9fdf7ad3b196915/src/main/java/co/surumene/simplecoins/domain/WorldStorageDiscovery.java
refs: []
semantic_contract: markdown-v2
language: ja
---

# WorldStorageDiscovery の訪問予算と列挙順序（code-to-spec Candidate）

## 対象
| Ref | Aspect |
| --- | --- |
| SC-DISCOVERY-BUDGET | Files.walkFileTree の訪問上限は候補以外のファイル・ディレクトリにも適用される |
| SC-DISCOVERY-ORDER | discovery の結果は入力Mapの反復順と独立した安定順序を持つ |

## 規則
| ID | 条件 | 観測される現行動作 | 検証 |
| --- | --- | --- | --- |
| SC-BUDGET-01 | world rootとregion配下に3個の訪問対象がありmaximumEntries=3 | Manifestを返しregionを1件列挙する | exact-budget JUnit |
| SC-BUDGET-02 | 同じ入力でmaximumEntries=2 | IOExceptionで中断し部分Manifestを返さない | below-budget JUnit |
| SC-BUDGET-03 | 走査対象に非権威backupファイルが多数あり、maximumEntriesが訪問数を下回る | backupは資産として計上しないが訪問予算を消費し、IOExceptionで中断する | unrelated-backup JUnit |
| SC-ORDER-01 | 異なるworld rootのMap挿入順を入れ替える | worldFilesはworld、path、kindの順にソートされ、requiredSourceIdsは安定した集合となる | reverse-map JUnit |

## 適用境界
これは現行ソースから抽出した検証候補であり、規範仕様のConfirmed昇格・process-closure・model-conformance・Paper実機検証を意味しない。予算上限の適切な運用値、並行変更中の一貫スナップショット、実ファイル欠損時のポリシーは別の未確定事項である。production sourceは変更しない。
