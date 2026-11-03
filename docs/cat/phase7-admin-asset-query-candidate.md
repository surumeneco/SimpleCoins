---
title: Phase 7 admin asset and dormant-property inquiry
status: Candidate
flow: code-to-spec
scope: O01 / TASK-20261101-006
source_revision: 01a06f9f0fb78c8f941f2aa2e1c3df5e8951d153
normative_confirmation: decision-required
---

# Phase 7 — administration inquiry (CAT Candidate)

This is a **Candidate for normative review**, not a Confirmed Process/PI/TCE, a generated TestModel, or implementation authorization. It distinguishes observed code from the user-defined product requirement and unresolved presentation choices.

## Observation — current implementation

- At the source revision above, `CoinCommand.onCommand` accepts the `give` subcommand and otherwise returns usage. `onTabComplete` offers `give` but does not expose a distinct statistics, balance, or dormant-asset inquiry subcommand.
- The command checks `simplecoins.admin` before processing arguments. This observation is scoped to `src/main/java/co/surumene/simplecoins/CoinCommand.java` (blob `ad45f0e630f3ddf1446983b58b1ddba2a09201c4`), **not** a proof that no other query service exists anywhere in the repository.
- The current `give` implementation is not evidence that the O01 inquiry requirement has been implemented.

## Requirement authority and proposed Process

Sources: `forGPT/XPlayServer/OriginalPlugins/SimpleCoins/03_運営設定・コマンド.md` (Drive ID `1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE`), and `05_1.0.0_実装計画.md` (Drive ID `1fSsHEn1tFX_DvnzxDK3t5FjDrIuvMrnt`).

The project specifies an OP-default administrative capability to inspect an arbitrary player's held value and aggregated statistics, and to inspect dormant assets **D** independently of the ordinary total **T = P + F**. Dormant detail must identify value, owner UUID, world/coordinates or logical location, storage kind and applicable reason (currently banned or missing playerdata); world-independent data should use a logical location. These are product requirements; command grammar and pagination are not decided here.

Proposed Process (for later normative confirmation):
1. Check the authorized administrative sender before resolving or displaying private location/owner data.
2. Obtain the authoritative, complete and generation-consistent asset/ownership evidence required for the requested report. Distinguish incomplete, unknown, stale or failed reads from a proven zero value.
3. Construct the player balance/statistics or D summary and location-level details without merging D into T.
4. Return a complete report, or a **non-success/no-assertion** outcome with sufficient diagnostic context; never silently replace unread sources with zero.
5. Keep query operations read-only: they must not reserve issuance, deliver currency, or mutate authoritative asset storage.

## Candidate PI / acceptance vectors

| ID | Input condition | Required observable property | Status |
| --- | --- | --- | --- |
| O01-ADMIN-01 | Sender lacks required admin permission | No owner/location/amount information disclosed; no inventory/data lookup before permission check | Candidate; implementation not yet tested |
| O01-ADMIN-02 | Complete consistent evidence for ordinary assets | Player value/statistics reflect evidence; T excludes D | Candidate |
| O01-ADMIN-03 | Current BAN or missing playerdata protects dormant assets | D is separate, with value, UUID, reason and physical/logical location | Candidate |
| O01-ADMIN-04 | World-independent source | Logical storage identifier instead of invented world coordinates | Candidate |
| O01-ADMIN-05 | One required source absent, unreadable, or owner undecidable | Non-success/unknown rather than publishing false zero or partial total as complete | Candidate |
| O01-ADMIN-06 | Same completed snapshot queried twice | Read-only deterministic presentation, no `give` or journal side effects | Candidate |

These vectors are semantic review inputs, **not compiler-produced vectors or passing tests**. Do not mark CAT Gate or O01 acceptance as passed based on this document.

## Decisions required before tests-current / production implementation

- Exact command grammar, page size, filters, name/UUID lookup and tab completion for the admin reports.
- Permission node(s): reuse `simplecoins.admin` or adopt a read-only subpermission; both must preserve OP-default management access.
- Snapshot freshness and failure response when a completed consistent generation is unavailable.
- Display details for zero-balance registrants, protected locations, world-independent sources and very large reports.

## Handoff

After authorized semantic confirmation: create normative CAT Artifacts, run canonical process-closure/model-conformance/vectors/Queue/current test gates, then implement the selected current item with verified Red/Green and integration tests. The source revision and decisions must be rechecked before doing so.
