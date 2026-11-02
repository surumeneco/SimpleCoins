---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.snapshot.world-root-ancestor-symlink
status: draft
process: simplecoins.snapshot
source_refs:
  - https://drive.google.com/file/d/173sQAGgr2JuL36ixCPtJ23daGfoOYNYQ/view
  - https://github.com/surumeneco/SimpleCoins/pull/34
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5 — world-root ancestor symlink integrity (CAT Draft)

The saved-source discovery contract rejects symlinked roots and avoids treating uncertain paths as empty assets. This Draft isolates an ancestor-path gap: Java NIO `Files.isDirectory(root, NOFOLLOW_LINKS)` checks the final path component, but a symlink in a parent directory can still be followed. The proposed behavior is **not Confirmed** and does not authorize a production change.

## Targets

| Ref | Aspect |
| --- | --- |
| simplecoins.snapshot.world-root-ancestor | Reject untrusted world-root ancestry before discovering authoritative saved sources |

## Rules

| ID | Condition | Requirement | Verification |
| --- | --- | --- | --- |
| SC-SOURCE-ANCESTOR-01 | A parent component of an otherwise regular world root is a symlink | Reject discovery with IOException, not a successful manifest | symlinkInAncestorOfOtherwiseRegularWorldRootMustBeRejected |
| SC-SOURCE-ANCESTOR-02 | The world root itself is a symlink | Preserve existing fail-closed rejection | symlinkAsWorldRootItselfIsRejected |
| SC-SOURCE-ANCESTOR-03 | World root and ancestors are ordinary directories | Preserve regular Anvil source discovery | ordinaryWorldRootStillDiscoversSavedRegion |
| SC-SOURCE-ANCESTOR-04 | World-specific assets are excluded but playerdata may still be present beneath an ancestor symlink | Do not bypass source-root trust checks based on world exclusion | excludedWorldPlayerdataStillRequiresTrustedAncestorPath |

## Test vectors

| ID | Input | Required result |
| --- | --- | --- |
| SC-SOURCE-ANCESTOR-01 (expected Red) | Symlink alias to a parent folder, regular world root and region file beneath it | IOException before a manifest is returned |
| SC-SOURCE-ANCESTOR-02 | Root itself is a symlink | IOException |
| SC-SOURCE-ANCESTOR-03 | Direct regular world root with region/r.0.0.mca | One REGION source |
| SC-SOURCE-ANCESTOR-04 (expected Red) | Excluded world reached through a symlinked ancestor, with independent playerdata | IOException; not an empty successful source inventory |

## Reproduced current behavior and scope

A direct Linux/JDK21 filesystem probe confirmed that `Files.isDirectory(alias/world, NOFOLLOW_LINKS)` returns true and `Files.walkFileTree(alias/world)` succeeds when `alias` is a symlink and `world` is a regular directory. The project JUnit tests were committed but have not yet executed in an actual JUnit runner; the two expected Red outcomes remain **unverified by JUnit**. PR #62 is stacked on PR #34, whose code is unchanged.

This Draft does not establish a complete Paper/Multiverse world catalog, an authoritative offline playerdata root, loaded/disk reconciliation, or settlement publication authority. Normative Process/PI/TCE are not Confirmed; formal CAT machine Gates and Paper live acceptance are not-run. No production guard, develop/main merge, deployment or release is authorized by this document.
