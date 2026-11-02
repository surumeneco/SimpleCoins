---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase5.manifest-physical-identity
status: candidate
process: simplecoins.snapshot
source_refs:
  - https://github.com/surumeneco/SimpleCoins/blob/b88cee565e78376f0161965cc9fdf7ad3b196915/src/main/java/co/surumene/simplecoins/domain/WorldStorageDiscovery.java
  - https://github.com/surumeneco/SimpleCoins/blob/b88cee565e78376f0161965cc9fdf7ad3b196915/src/test/java/co/surumene/simplecoins/domain/WorldStorageDiscoveryTest.java
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5: 手動Manifestの物理ソース同一性（code-to-spec Candidate）

## Targets

| Ref | Aspect |
| --- | --- |
| simplecoins.snapshot.manifest-identity | 公開ManifestコンストラクターとrequiredSourceIds()の境界 |
| simplecoins.snapshot.cross-category-alias | region/entity/playerdataの同一実体の重複計上防止 |

## Rules

| ID | Condition | Requirement | Verification |
| --- | --- | --- | --- |
| SC-MPHYS-01 | 手動構築したManifestのworldFilesに別名の同一inodeが二度現れる | 別パスを独立した保存元として確定せずIOExceptionで拒否する | hardLinkedWorldSourcesCannotBecomeTwoRequiredIds |
| SC-MPHYS-02 | 手動構築したManifestでregionとplayerdataが同一inodeを指す | カテゴリが異なっても二重の必須保存元として扱わない | hardLinkedWorldAndPlayerdataCannotBecomeTwoRequiredIds |
| SC-MPHYS-03 | 手動構築したManifestが同じ実ファイルを通常パスとsymlinkで参照する | パス表記の違いを根拠に重複計上しない | symlinkAliasCannotBecomeSecondRequiredId |
| SC-MPHYS-04 | 異なる実ファイルからなるManifestを手動構築する | すべての必要なIDを返し、返却集合は変更不可 | distinctPhysicalSourcesRetainIndependentRequiredIds |

## Evidence and limits

- PR #34の`discover()`はファイルキーと`Files.isSameFile`で物理重複を拒否するが、公開`Manifest`コンストラクターを経由した`requiredSourceIds()`は現行コード上で絶対正規化パス文字列のみを比較する。これは実装の観測に基づく検証候補であり、既存のConfirmed規範を新設するものではない。
- 本文はcode-to-spec Candidate。正当な仕様決定とCAT正式Gateを経るまでproduction修正は行わない。
- 物理ファイル同一性のテストはローカルファイルシステム上のhardlink/symlinkのみを対象とする。Paper稼働中のスナップショット整合性、保存元カタログ完全性、settlement公開権限は対象外。
