# Phase 5: world-root identity closure (CAT Draft)

Status: **Draft**, not Confirmed. Parent: TASK-20261031-SIMPLECOINS-100; leaf: TASK-20261101-003.

## Process / PI

The caller supplies a bounded set of authoritative world roots. Matching the saved-file manifest before and after traversal does not establish that the same root directories were observed: an empty world root can be replaced while a different world's player callback runs, leaving the saved-source list unchanged.

At the traversal boundary, capture each world-root directory's physical identity before discovery. Verify that every supplied root is still the same non-symbolic directory after the preflight discovery and again after the postflight discovery. A root that disappears, becomes a symbolic link, changes identity or has no verifiable physical file key must invalidate the staged report. The caller must still provide a complete authoritative world-root inventory; this check does not prove completeness, freeze Paper memory or authorize settlement publication.

## TCE / acceptance vectors

- SC-ROOT-01 (Red): replacing an empty second world root during another world's playerdata callback must abort even if the discovered source-file set is unchanged.
- SC-ROOT-02 (Regression): unchanged roots and source files return the existing successful traversal Report.
- SC-ROOT-03 (Red): two differently spelled world roots resolving to the same physical directory must fail before any callback, even if both are empty.
- SC-ROOT-04 (Regression): a missing or symbolic world root fails closed before any visitor callback.

## Gate status

Formal CAT machine gates **not-run**. This Draft is a conservative root-identity check, not an atomic filesystem snapshot, complete live-world reconciliation or publication gate.
