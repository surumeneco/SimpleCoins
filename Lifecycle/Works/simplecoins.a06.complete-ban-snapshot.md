---
cat_version: '0.2'
kind: work
id: simplecoins.a06.complete-ban-snapshot
status: draft
process: simplecoins.settlement.ownership-evidence
source_refs:
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: verification
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/4
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# Complete declared BAN lists for startup settlement

Use the approved all-BAN semantics without a new ownership rule. An exhaustive authoritative list for each declared provider covers UUIDs discovered later in the saved scan, unlike a partial per-UUID sample. No unobserved provider or unmapped nonempty IP list is treated as clear. Keep the existing per-subject capture contract intact. Paper reads its actual standard lists on the primary thread; any external lists must be independently established by their adapter. This is not an assertion of the actual deployment's complete provider roster.

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.settlement.ownership-evidence | simplecoins.a06.complete-ban-snapshot | simplecoins.ban-source-coverage;simplecoins.profile-ban-source |

## Technologies

| Technology |
| --- |
| Java 25; Paper 26.2 |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a06.complete-ban-snapshot.md;src/main/java/co/surumene/simplecoins/domain/BanSourceCoverage.java;src/main/java/co/surumene/simplecoins/paper/PaperProfileBanSource.java;src/test/java/co/surumene/simplecoins/domain/BanSourceCoverageTest.java;src/test/java/co/surumene/simplecoins/paper/ProfileBanLiveProbe.java | src/main/java/co/surumene/simplecoins/domain/BanSourceCoverage.java;src/main/java/co/surumene/simplecoins/paper/PaperProfileBanSource.java | src/test/java/co/surumene/simplecoins/domain/BanSourceCoverageTest.java;src/test/java/co/surumene/simplecoins/paper/ProfileBanLiveProbe.java |

## Checks

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |

## Gates

| Gate | Status | Evidence |
| --- | --- | --- |
| process-closure | not-run | — |
| model-conformance | not-run | — |
| implementation-conformance | not-run | — |
| ci | not-run | — |
| merge | not-run | — |

## Acceptance

- Complete lists: union of banned UUIDs, immutable, missing or duplicate source rejected; existing partial per-UUID capture remains fail-closed.
- Isolated actual Paper: active unmapped IP ban refuses capture; after fixture IP pardon the profile-banned UUID is banned, unrelated/discovered-later UUIDs clear; missing declared external source rejected.
- No admission, deployment, provider-roster assumption or IP-to-UUID inference. Nonempty IP lists still require authenticated mapping.
