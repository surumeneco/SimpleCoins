---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase5.discovery-order-budget-verification
status: draft
process: simplecoins.world-storage-discovery
source_refs:
  - https://drive.google.com/file/d/173sQAGgr2JuL36ixCPtJ23daGfoOYNYQ/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5: WorldStorageDiscovery の順序・予算境界（検証用 Draft）

## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.world-discovery | 入力順とworld sourceの同一性 |
| simplecoins.world-scope | 除外世界のplayerdataと全訪問予算 |

## 位置付け
既存 `WorldStorageDiscovery.discover` の現行挙動を、独立した verification Work として固定する。参照ソースは SimpleCoins PR #34 HEAD `b88cee565e78376f0161965cc9fdf7ad3b196915` の `WorldStorageDiscovery.java`（blob `9050e638d5d10c6b4173ae400f62daca5b3edde7`）。これは規範として承認された仕様ではなく、既存コードの検証候補である。

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| SC-ORDER-01 | 同じ2世界を逆順のLinkedHashMapで渡す | worldFiles、playerDataFiles、requiredSourceIds | 値が同一で、worldFilesとsource IDの列挙順が安定する |
| SC-ORDER-02 | 同じr.0.0.mca座標を持つ独立した2世界 | 世界名を含むSourceFile | 座標一致だけで異世界資産を統合せず、2つのsource IDを維持する |
| SC-BUDGET-01 | 対象外の世界に大量の無関係ファイルがある | 全訪問数を数えるmaximumEntries | 対象外ファイルも訪問予算に算入し、超過時はIOExceptionで部分結果を公開しない |
| SC-ROOT-01 | 異なる世界名が同一の物理rootを指す | root重複チェック | 入力順に関係なくIOExceptionで拒否する |

## 実行境界
- Java 25 / JUnit 5で現行ソースに対する実行結果を確認する。CAT Draft構文lintは規範Gateと区別する。
- `WorldScope` が除外するのはworld-owned region/entityであり、playerdataは独立した候補として残る。SC-ORDER-01ではこの差を含める。
- `maximumEntries` の仕様は「全訪問ファイル・ディレクトリ数」であり、計測に基づく上限選定は別の運用責務。予算超過でゼロ資産と解釈しない。
- 本Workは既存コードの独立検証であり、production変更、CAT Confirmed昇格、Paper実機受入、決済公開を行わない。

## 未実施
Formal CAT Process/PI/TCE Gate、Paper/Multiverse実機の一貫スナップショット、ロード中エンティティとの突合は本Draftの結果からは推論しない。
