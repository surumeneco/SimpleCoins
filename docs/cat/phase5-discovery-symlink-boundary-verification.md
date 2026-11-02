---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase5.discovery-symlink-boundary-verification
status: draft
process: simplecoins.world-storage-discovery
source_refs:
  - https://drive.google.com/file/d/173sQAGgr2JuL36ixCPtJ23daGfoOYNYQ/view
  - https://github.com/surumeneco/SimpleCoins/pull/34
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5: 保存済みソース走査のシンボリックリンク境界（検証用 Draft）

## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.world-discovery | `WorldStorageDiscovery.discover` の NOFOLLOW_LINKS / walkFileTree の現在の振る舞い |
| simplecoins.world-scope | world scope の対象外でもシンボリックリンクを通じた未検証の走査をしないこと |

## 位置付け
PR #34 HEAD `b88cee565e78376f0161965cc9fdf7ad3b196915` の既存実装について、実装が宣言する「bounded, non-symlink traversal」を実際の Linux ファイルシステム上で回帰検証する独立した verification Work。CAT Draft は承認済み規範ではなく、テストが Green でも正規 Process/PI/TCE Gate の代わりにはならない。手動 Manifest の `requiredSourceIds()` に関する PR #71 の候補契約とは対象を分離する。

## 規則
| ID | Condition | Requirement | Verification |
| --- | --- | --- | --- |
| SC-LINK-01 | world root 自体がシンボリックリンク | IOException で manifest 非公開 | symbolicWorldRootIsRejected |
| SC-LINK-02 | 正規 region/r.0.0.mca が通常ファイルへのシンボリックリンク | IOException で manifest 非公開 | symbolicAuthoritativeRegionIsRejected |
| SC-LINK-03 | playerdata/UUID.dat が通常ファイルへのシンボリックリンク | IOException で manifest 非公開 | symbolicPlayerdataIsRejected |
| SC-LINK-04 | world root 内のバックアップディレクトリがシンボリックリンク | バックアップを辿らず IOException で中断 | symbolicBackupDirectoryIsRejected |
| SC-LINK-05 | scope 対象外 world にシンボリックリンクがある | scope 除外をリンク走査の許可としない | excludedWorldDoesNotBypassSymbolicLinkRejection |
| SC-LINK-06 | root/region ディレクトリ自体がシンボリックリンク | リンク先を走査せず IOException で中断 | symbolicRegionDirectoryIsRejected |

## 実行境界
- `@EnabledOnOs(OS.LINUX)` を用い、シンボリックリンク生成可能な Linux runner 上で実測する。Gradle/JUnit の Green と CAT Draft syntax lint を区別して記録する。
- この Draft は **root の祖先ディレクトリ**に含まれるリンクの取り扱いを規範化しない。祖先経由のエイリアスは別途仕様判断が必要であり、これらのテストが通っても防げると断定しない。
- 本 Work は既存ソースの検証のみ。production の変更、CAT Confirmed への昇格、Paper/Multiverse 実機検証、`develop`/`main` マージ、公開は行わない。

## 未実施
Formal CAT Process/PI/TCE Gate、実運用の世界構成・一貫スナップショット・ロード中エンティティとの突合は not-run。
