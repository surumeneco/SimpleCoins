---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.snapshot.manifest-source-presence
status: draft
process: simplecoins.snapshot
source_refs:
  - https://drive.google.com/file/d/173sQAGgr2JuL36ixCPtJ23daGfoOYNYQ/view
  - https://github.com/surumeneco/SimpleCoins/pull/34
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5 — manual manifest source presence (CAT Draft)

A caller-created `WorldStorageDiscovery.Manifest` may contain paths not returned by `discover()`. The existing `requiredSourceIds()` checks lexical duplicates only. This candidate contract asks whether a missing, non-regular, or symbolic mandatory source can be used as evidence of a complete snapshot. It does **not** claim to establish snapshot coherence or to parse Anvil/NBT contents.

## Targets

| Ref | Aspect |
| --- | --- |
| manifest-source-presence | Fail closed when a required manifest source is not a regular, non-symlink file at closure evaluation |

## Rules

| ID | Condition | Requirement | Verification |
| --- | --- | --- | --- |
| SC-PRESENCE-01 | A manually constructed manifest contains a region source path that does not exist | Reject closure with IOException; absence is not evidence of zero assets | missingRegionCannotCloseManifest |
| SC-PRESENCE-02 | A playerdata source path is a directory rather than a regular file | Reject closure with IOException | directoryPlayerdataCannotCloseManifest |
| SC-PRESENCE-03 | A region source path is a symbolic link to a regular file | Reject closure with IOException, consistent with discover() non-symlink policy | symlinkRegionCannotCloseManifest |
| SC-PRESENCE-04 | Region and playerdata sources are separate existing regular files | Preserve both source IDs | existingRegularSourcesRemainEligible |
| SC-PRESENCE-05 | A source discovered successfully is deleted before requiredSourceIds() is evaluated | Reject stale closure with IOException; discovery alone is not a snapshot lock | deletedAfterDiscoveryCannotCloseManifest |

## Test vectors

| ID | Input | Required result |
| --- | --- | --- |
| SC-PRESENCE-01 | Manifest(REGION=world/region/r.0.0.mca) without creating the file | IOException |
| SC-PRESENCE-02 | Manifest(playerdata/UUID.dat) after Files.createDirectories at that path | IOException |
| SC-PRESENCE-03 | Manifest(REGION=world/region/r.0.0.mca) as Files.createSymbolicLink to a real source | IOException |
| SC-PRESENCE-04 | Files.write two separate regular sources, then construct Manifest | Two distinct canonical source IDs |
| SC-PRESENCE-05 | discover() returns a manifest for region/r.0.0.mca, then Files.delete removes that region | IOException at closure evaluation |

## Status and authority

**Draft, not Confirmed.** This is a proposed fail-closed closure invariant, not an approved production contract. The tests may intentionally fail against the current implementation. Formal CAT Process/PI/TCE confirmation and machine Gates, production Green, and Paper/Multiverse acceptance are **not-run**. No production changes, develop/main merge, or deployment are authorized by this Draft.
