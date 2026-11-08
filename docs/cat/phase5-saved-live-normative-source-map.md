# Phase 5 saved/live settlement — normative source-to-CAT intake map

Status: **Candidate / issue-work intake**. This is a source mapping, not a Confirmed Process/PI/TCE, a CAT Gate pass, or an implementation authorization.

## Evidence and baseline

- TaskQueue leaf: [TASK-20261101-003](https://drive.google.com/file/d/173sQAGgr2JuL36ixCPtJ23daGfoOYNYQ/view).
- Design source 02A: [ownership and asset statistics](https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view), section **所有者の定義（確定事項）**.
- Design source 02B: [data collection and settlement](https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view), sections **データ収集・キャッシュ**, **起動時／停止時決算**, **決算中のプレイヤー管理とデータ静止条件**, **同期／非同期と公開開始**.
- Observed source baseline: `develop@f7ebdc33227eb279d44461e9d7e6bf2d86598b23`. This SHA is not a frozen future implementation baseline.
- CAT runtime: upstream `6d5ab948c66442d8543dab65647ab361ddf819f1`, subtree `a444b36dd494a65778a852af5c2dd7fc72f283fa`. Existing Work `simplecoins.phase5.saved-live-intake` is Candidate; its structural validation does not verify semantic approval.

## Process / PI / TCE source correspondence

| Candidate identifier | Source-defined invariant or effect | Observed implementation boundary | Missing proof or unresolved implementation |
| --- | --- | --- | --- |
| `PI.SETTLEMENT.COMPLETE_INPUT` | 02B: every eligible world/playerdata source must be traversed; unreadable or missing input must not become zero or published totals | `WorldSnapshotTraversal.traverse` and `SettlementScanRunner.stage` stage disk inputs and throw on failures | No verified loaded-world reconciliation, Paper admission sequencing, or full cross-source completeness |
| `PI.SETTLEMENT.WORLD_SCOPE` | 02A/02B: default ON, ON/OFF override, overlap falls back to default; world-independent playerdata always included | `SettlementScanRunner.stage` checks `WorldScope.includes` for world chunks, traverses playerdata separately | Need actual Paper world-list configuration binding and coverage of loaded/offline sources |
| `PI.SETTLEMENT.UNIQUE_ASSET` | 02A: each coin/wallet contributes to exactly one owner; 02B: no saved/live or container/entity double count | `WorldSnapshotTraversal` detects duplicate saved entity UUIDs across chunks and physical file aliases | Saved UUID uniqueness is **not** a proof of saved/live uniqueness, double-chest identity, or ownership reconciliation |
| `PI.SETTLEMENT.OWNERSHIP` | 02A: player inventory ownership, BlockLocker Everyone→free, banned/missing playerdata→dead, WWC/WWW commander→owner→free | `SettlementScanRunner` takes `currentlyBanned`, `BlockOwnerLookup`, `EntityOwnerLookup` as external inputs | Full BAN-source coverage, BlockLocker live sign resolution, and plugin-specific live owner adapters require separate evidence |
| `PI.SETTLEMENT.ATOMIC_PUBLISH` | 02B: build a new generation; publish only after all sources verified; partial failures must not replace committed statistics | `SettlementScanRunner.Staged` explicitly does not publish; publisher exists separately | End-to-end startup wiring, admission gating, fail-closed state, and actual atomic cutover remain unverified |
| `TCE.SETTLEMENT.MUTATION` | 02B: no-player condition does not freeze world; changes during scan must be frozen/reconciled, including external plugin writes | `WorldSnapshotTraversal` compares source metadata before/after; comments explicitly limit this to best effort | Metadata equality does not prove snapshot coherence. Need freeze/generation/replay contract and actual concurrency tests |

## Distinguish confirmed user decisions from engineering decisions

The 02A **確定事項** ownership and scope semantics and 02B explicitly specified fail-closed/full-scan requirements are established design inputs. Their existence **does not automatically promote this AI-authored mapping to Confirmed CAT artifacts**. The concrete unresolved design decision is the coherence protocol for saved-vs-loaded entities and writes during traversal (freeze, generation/replay, or another proven equivalent), including the precise point at which publication is permitted. The 02B document expressly identifies this as an implementation-time validation/selection requirement.

## Safe next execution boundary

Continue the canonical `issue-work` Process/PI/TCE draft and provenance mapping, then obtain genuine normative confirmation before `spec-implementation` process closure/model/TDD Red–Green and production wiring. Alternatively use independent `code-to-spec` Candidate or behavior-preserving `refactor` with unchanged-oracle baseline and regression. Do not use JUnit Green or the structural `cat_flow.py validate` result as CAT Gate evidence.
