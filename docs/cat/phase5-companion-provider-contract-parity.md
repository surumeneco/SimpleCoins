---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.owner.companion-provider-contract-parity
status: draft
process: simplecoins.owner
source_refs:
  - https://drive.google.com/file/d/1sn-FXbU7nFDr6yc6YxPAe2V6U6mV2CZD/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5: WWC loaded/saved parity and WWW optional API boundary (CAT Draft)

## Scope and authoritative source observations

- WWC `WonderfulChickenStore` main branch, blob `28086b60eac4796c2e51123e76f41859bb8aaf5d`: marker `wonderful_chicken`, PDC `type`, `data_version` 2–4, and `follow_target` string are persisted under the plugin namespace. This observation does not prove that follow target is the economic owner.
- WWW `WhatAWonderfulWolfPlugin.loadedIndividuals()` blob `09b1d2a4719258118a93711b23bc575a8e43606f`, `WonderfulWolfLoadedIndividuals.find(UUID)` blob `e0b31006c841f14c19831fc51eb1efdc3c7d9eed`, and `WonderfulWolfIndividual.ownerId()/commanderId()` blob `ad2bd722d6e3ba7e5ddaec457f17e834b9b560a9` establish the current public reflection method shapes. `find` only exposes loaded wolves.
- SimpleCoins PR #8 currently provides positive-only `PaperWonderfulChickenOwnerAdapter.read`, `SavedWonderfulChickenOwnerEvidence.read`, and optional `PaperWonderfulWolfOwnerAdapter.readPlugin`. No saved/unloaded WWW identity catalog is established.

## Proposed test contract (not a confirmed ownership specification)

| TCE | Given | Expected |
| --- | --- | --- |
| PROVIDER-PARITY-01 | Same canonical WWC marker, supported version 2/3/4, and UUID in loaded and saved representations | Identical positive commander evidence; no inferred economic ownership |
| PROVIDER-PARITY-02 | Invalid marker, version or target, one defect at a time | Both views return UNKNOWN, never FREE |
| PROVIDER-PARITY-03 | Wrong saved NBT type or entity ID | Saved view returns UNKNOWN |
| PROVIDER-WWW-01 | WWW's current public loadedIndividuals/find/Optional/record accessors | Known commander is returned for a loaded record |
| PROVIDER-WWW-02 | Absent loaded record, null provider, missing/changed method shape or provider invocation exception | UNKNOWN; never interpret API failure as verified ownerlessness |

## Scope exclusions and Gate status

This is an independent regression specification and test-only increment. It does not add a provider implementation, mutate WWC/WWW, verify actual Paper server classloading, enumerate unloaded WWW/WCL individuals, resolve external BAN/IP provenance, or settle assets. It does not authorize a production change or a `develop`/`main` merge.

**Draft / not Confirmed.** Normative CAT Process/PI/TCE confirmation, process-closure, TestModel, model-conformance, vectors, Queue, current-item and implementation-conformance Gates are not-run. Any local direct Java harness result is narrower than Gradle/JUnit and live Paper integration. The source observations above are pinned to exact Git blobs; upstream changes require revalidation.
