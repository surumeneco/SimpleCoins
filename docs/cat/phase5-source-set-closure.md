# Phase 5: saved-source set closure at traversal boundary (CAT Draft)

Status: **Draft**, not Confirmed. Parent: TASK-20261031-SIMPLECOINS-100; leaf: TASK-20261101-003.

## Process / PI

A saved-source manifest lists the region/entity files and canonical playerdata files discovered at one point in time. Even if every listed file is individually unchanged, a newly created authoritative file after discovery can be omitted from the settlement candidate.

For a caller-supplied fixed set of authoritative world roots and scope, expose a bounded re-discovery comparison. Before and after the read-only traversal, require the exact same authoritative file identities (world name, source kind, path, player UUID). Reject added, removed or renamed authoritative sources, non-regular paths and discovery-budget exhaustion. Do not return a successful traversal Report if either closure check fails.

The caller must supply the authoritative world-root inventory and prevent changes between the final check and publication. This comparison is **best-effort**, not a filesystem transaction, not a loaded-memory reconciliation and not a Paper world freeze. It must not be treated as a settlement publication gate by itself.

## TCE / acceptance vectors

- SC-CLOSURE-01 (Red): a playerdata source added after manifest discovery but before traversal aborts before any visitor callback.
- SC-CLOSURE-02 (Red): a new playerdata source added during the final visitor callback aborts after traversal, with no successful Report.
- SC-CLOSURE-03 (Regression): unchanged source inventory returns the normal traversal Report.
- SC-CLOSURE-04 (Regression): excluded-world region files remain excluded from the source-set comparison; playerdata remains world-independent.
- SC-CLOSURE-05 (Regression): invalid paths and an exhausted bounded discovery budget propagate failure.

## Gate status

Formal CAT machine gates **not-run**. This Draft does not establish a coherent live-world snapshot, complete Paper source list, ownership, merge, deploy or settlement publication.
