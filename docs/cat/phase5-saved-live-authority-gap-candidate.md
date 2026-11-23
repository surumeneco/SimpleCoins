# TASK003 — Saved/live settlement authority and implementation gap (Candidate)

Status: **Candidate / code-to-spec observation**, not a Confirmed Process/PI/TCE or CAT Gate.
Source snapshot: `surumeneco/SimpleCoins develop@f7ebdc33227eb279d44461e9d7e6bf2d86598b23`.
Normative source: `forGPT/XPlayServer/OriginalPlugins/SimpleCoins/02B_データ収集・決算処理.md`, Drive ID `1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8`, sections **起動時／停止時決算**, **決算中のプレイヤー管理とデータ静止条件**, **性能設計・データ形式**. This document contains both decided behavior and expressly unresolved implementation choices; this Candidate does not promote either.

## Observed implementation (source, not normative authority)

`src/main/java/co/surumene/simplecoins/domain/SettlementScanRunner.java`:
- `stage` consumes `WorldStorageDiscovery.Manifest`, `WorldScope`, BAN UUIDs and caller-supplied block/entity owner resolvers.
- `WorldSnapshotTraversal.traverse` visits disk region/entity chunks and playerdata; the visitor filters world chunks using `scope.includes`, but playerdata remains world-independent.
- `pending.completeSource` runs after traversal; exceptions prevent returning `Staged`.
- The returned `Staged` is pending only. No atomic publication, loaded-entity reconciliation, Paper startup lifecycle or consistent cross-source snapshot is established by this class.
- `SettlementScanRunnerTest` covers empty region/playerdata, BAN classification, explicit saved-entity ownership and failed reader/ownership cases. These are bounded unit tests, not live Paper evidence.

## Normative-to-code mapping and missing acceptance evidence

| 02B requirement | Existing boundary | Missing verification / next legitimate Work |
| --- | --- | --- |
| Every eligible saved container, item entity, equipment, player inventory and Ender Chest is counted | Disk manifest/traversal + importers | Paper 26.2 world placement and complete producer-to-manifest wiring, including plugin-specific stores |
| Loaded world and online state are authoritative, disk for unloaded/offline | Disk-only `SettlementScanRunner.stage` | Explicit saved/live selection policy and unique identity across both sources; no proof of overlap exclusion yet |
| No double count of disk/memory representations or double chest | Disk-only source staging | Identity and snapshot generation across live and saved adapters; exact collision cases and fail-closed result |
| All player connections blocked during settlement, zero online, saved state consistent | No lifecycle in runner | Paper startup adapter, join gate, save/freeze boundary, failure behavior and integration test |
| All-or-nothing replacement only after complete scan | Pending result, no publication in runner | Transactional commit boundary and failure injection across all sources |
| World ON/OFF and world-independent playerdata | Scope filter on chunk visitor, playerdata independent | Combined disk/live world-filter regression and configured default/ON/OFF conflicts |
| Detect drift and external plugin writes | Disk traversal with prior source-drift change #269 | Consistent snapshot/redo or freezing protocol; saved-source drift check alone is insufficient |

## Candidate Process/PI/TCE scope (not approved)

- **Process**: select one authoritative representation per asset identity for an epoch; reconcile saved and live data; validate completeness; atomically publish or fail closed.
- **PI candidates**: world inclusion, source freshness, entity/container identity, player UUID and current BAN/playerdata state, lifecycle gate, snapshot epoch.
- **TCE candidates**: duplicate saved+loaded entity, chunk transition during traversal, externally mutated inventory, disabled world, missing owner, incomplete file, late login, failed commit.

**Decision boundary**: The user has decided complete authoritative scanning, no partial publication, and live-over-disk precedence for loaded state. The precise freeze/replay mechanism, identity-key contract and Paper integration sequence require engineering evidence and, where semantics differ, normative approval. This artifact is an observation and gap map only. It does not claim a Green test, a CAT conformance Gate or Task003 completion.
