---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase7.managed-resource-ancestor-integrity
status: draft
process: simplecoins.pack
source_refs:
  - https://drive.google.com/file/d/1KxmgwxYdE5dhBxW6mwzaYezxcRaSWx8o/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 7: managed Geyser pack root ancestry (CAT Draft)

Status: **Draft**, not Confirmed. Leaf TASK-20261101-006.


## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.pack | Geyser管理root/出力のancestor integrity |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| managed.ancestor | 管理rootより上のancestorがsymlinkのとき | 対象外へpack/mapping/manifestを書かず拒否する | GEYSER-MANAGED-ANCESTOR-01 |
| managed.normal | 通常のreal nested rootを使うとき | 管理対象のみinstall/update/removeする | GEYSER-MANAGED-ANCESTOR-02 |
| managed.output | 管理outputや直近parentがsymlinkのとき | 拒否して無関係ファイルを変更しない | GEYSER-MANAGED-ANCESTOR-03 |

## Process / PI

SimpleCoins may write or remove only its explicitly managed Geyser resource-pack and mapping files. Before creating a managed root or changing any resource, reject symbolic links at every existing component of the root and target ancestry, not merely the final file or immediate parent. A redirected ancestor must never cause a pack, mapping, or manifest to be written outside the intended real data folder. Recheck after creating directories. This is a best-effort local path integrity check, not an atomic protection against malicious concurrent filesystem replacement.

## TCE / vectors

- GEYSER-MANAGED-ANCESTOR-01 (Red): an ancestor symlink above the managed root rejects replacement before any file is written under the symlink destination.
- GEYSER-MANAGED-ANCESTOR-02: a normal real nested root still installs, updates and removes only managed outputs.
- GEYSER-MANAGED-ANCESTOR-03: a symlinked managed output or immediate parent remains rejected; unrelated files are untouched.

No Paper/Geyser/BE live verification, CAT machine Gate, merge or deployment is authorized. This Draft is not Confirmed.
