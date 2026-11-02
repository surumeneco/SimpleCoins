---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.snapshot.authoritative-source-file-types
status: draft
process: simplecoins.snapshot
source_refs:
  - https://drive.google.com/file/d/173sQAGgr2JuL36ixCPtJ23daGfoOYNYQ/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5: authoritative saved-source file type integrity (CAT Draft)

Status: **Draft** (not Confirmed). Parent: TASK-20261031-SIMPLECOINS-100; leaf: TASK-20261101-003.


## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.snapshot | 正規の保存source形状・ファイル種別 |
| simplecoins.world | WorldScopeによる保存sourceの対象判定 |
| simplecoins.snapshot.source-identity | 保存sourceと外部sidecarの物理同一性 |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| source-type.nonregular | 権威region/entity/playerdataパスが通常ファイルでないとき | 不正sourceとして全体走査を拒否し、資産0として成功扱いしない | SC-SOURCE-TYPE-01, 02, 05, 06 |
| source-type.scope | 無関係backupやWorldScope除外worldにregion形状のpathがあるとき | 対象外のsourceを誤って必須資産として計上しない | SC-SOURCE-TYPE-03, 07 |
| source-type.physical-alias | region/playerdata間または外部sidecar間にhardlinkがあるとき | 同一物理sourceを独立した権威入力として二重計上しない | SC-SOURCE-TYPE-08, 09 |
| source-type.regular | 正規の保存sourceと別実体の外部sidecarがあるとき | 正当なsourceを引き続き列挙・読取し、訪問予算を維持する | SC-SOURCE-TYPE-04, 10 |

## Process / PI

WorldStorageDiscovery enumerates only regular, authoritative playerdata and Anvil region/entity files. A directory or other non-regular filesystem object at a path shaped like a mandatory source (for example `world/region/r.0.0.mca` or `world/playerdata/<canonical UUID>.dat`) is an invalid snapshot input, not proof that the source contains zero assets.

During bounded, non-symlink discovery, reject a non-regular entry at an authoritative source path before constructing a manifest. Unrelated directories (including backup directories) must not be classified as authoritative data. An empty world or a missing authoritative source directory is not addressed by this change; completeness of the expected source set and loaded-vs-disk reconciliation remain independent release gates.

## TCE / acceptance vectors

- SC-SOURCE-TYPE-01 (Red): a directory at `world/region/r.0.0.mca` must throw `IOException` rather than yield a zero-region manifest.
- SC-SOURCE-TYPE-02 (Red): a directory at `world/playerdata/<UUID>.dat` must throw `IOException` rather than yield a zero-player manifest.
- SC-SOURCE-TYPE-03 (Green): an unrelated backup directory named `r.0.0.mca` under `world/backups` does not become a mandatory source.
- SC-SOURCE-TYPE-04 (Regression): regular canonical Anvil/playerdata paths are still discovered and the global visit budget is still enforced.
- SC-SOURCE-TYPE-05: the modern `dimensions/minecraft/the_nether/entities/r.0.0.mca` path must also reject a directory.
- SC-SOURCE-TYPE-06: legacy `players/data/<UUID>.dat` must reject a directory.
- SC-SOURCE-TYPE-07: a region-shaped directory in a world excluded by WorldScope remains outside the world asset scan.

- SC-SOURCE-TYPE-08 (Red): a hard link shared by an Anvil region file and a canonical playerdata file is a single physical source masquerading as two different authority categories; reject it even though each category has no internal duplicate.
- SC-SOURCE-TYPE-09 (Red): two distinct referenced external chunk `.mcc` paths that are hard links to one physical payload cannot be treated as independent authoritative inputs during snapshot traversal.
- SC-SOURCE-TYPE-10 (Regression): two genuinely distinct external sidecars remain readable and count as two independent chunks.

## Gate status

Formal CAT machine gates: **not-run**. This Draft does not authorize a settlement commit, merge, deployment, or publication. GitHub CI may validate the Java/JUnit contract independently.
