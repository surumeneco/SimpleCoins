---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase5.root-ancestor-symlink-alias
status: draft
process: simplecoins.world-storage-discovery
source_refs:
  - https://drive.google.com/file/d/173sQAGgr2JuL36ixCPtJ23daGfoOYNYQ/view
  - https://github.com/surumeneco/SimpleCoins/pull/34
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5: World root 祖先のシンボリックリンク別名（Red候補 Draft）

## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.world-discovery | `WorldStorageDiscovery.discover` が検証する world root の物理的な到達経路 |
| simplecoins.world-identity | 複数world名が同一の物理rootを参照した場合の重複計上防止 |

## 観測と未確定仕様
PR #34 HEAD `b88cee565e78376f0161965cc9fdf7ad3b196915` は `Files.isDirectory(root, NOFOLLOW_LINKS)` と `Files.walkFileTree(root)` を用いる。JDK21/Linuxの独立実測では、rootそのものではなく**rootの祖先**にシンボリックリンクがある場合、rootは通常ディレクトリとして判定され、walkFileTreeの属性にもリンクが現れない。これを「物理rootを検証できないので非公開」とするかは未承認の候補契約である。

## 規則
| ID | Condition | Requirement | Verification |
| --- | --- | --- | --- |
| SC-ANCESTOR-01 | `alias/world` の `alias` が `real` へのリンクで、`world` は実ディレクトリ | `discover` は IOException を返し、manifest を公開しない | symlinkedAncestorOfWorldRootMustFailClosed |
| SC-ANCESTOR-02 | 物理的に同一の空worldを実pathと祖先リンク経由で2つのworld名として登録 | 0件のsourceでも物理root別名を拒否する | emptyWorldAliasThroughAncestorMustNotBeIndependent |
| SC-ANCESTOR-03 | リンクを含まない実world root と通常regionファイル | 既存の正常な発見動作を維持する | ordinaryRootWithoutLinksRemainsDiscoverable |

## Redの位置付け
最初の2件は現行実装に対する**失敗を期待するテスト**であり、単なるCI失敗をCAT Red Gate通過とみなさない。3件目は正常系。現行ソースの変更・自動修正・仕様確定は行わない。祖先パスを常に拒否する運用上の可否、rootのrealpathによる正規化の範囲、Multiverseの許容構成は別途規範確認が必要。

## 未実施
正式CAT Process/PI/TCE確認と機械Gate、production Green、Paper/Multiverse実機受入、`develop`/`main` マージは not-run。
