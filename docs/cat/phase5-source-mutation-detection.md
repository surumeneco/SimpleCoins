# Phase 5: saved snapshot source mutation detection (CAT Draft)

Status: **Draft**, not Confirmed. Parent: TASK-20261031-SIMPLECOINS-100; leaf: TASK-20261101-003.

## Process / PI

The read-only saved-asset traversal must not return a successful Report when an authoritative region/entity or playerdata file changes while that file is being traversed or its decoded data is delivered to the visitor. Capture a no-follow file identity/metadata stamp immediately before reading each file, and compare it after the read and callback(s). A mismatch (file type, physical file key, size, creation time or last-modified time) raises IOException. A failure yields no successful Report and cannot authorize publication.

This is a **best-effort mutation detector**, not a stable-snapshot protocol. An attacker or concurrent writer can restore size and timestamps, and a live Paper server may mutate world memory without modifying the saved file. The caller must still establish a consistent authoritative snapshot and reconcile loaded/disk state; this Draft does not authorize settlement commit, merge or deploy.

## TCE / regression vectors

- SC-SOURCE-STABILITY-01 (Red): a region file changed during its chunk visitor must cause traverse to throw IOException, not return a successful Report.
- SC-SOURCE-STABILITY-02 (Red): a playerdata file changed during its player visitor must likewise throw IOException.
- SC-SOURCE-STABILITY-03 (Regression): an unchanged region and unchanged playerdata file traverse successfully with correct counts.
- SC-SOURCE-STABILITY-04: replacement or disappearance of a source must fail closed rather than being silently counted as empty.

- SC-SOURCE-STABILITY-05 (Red): a referenced external `.mcc` sidecar changed during the decoded chunk callback must cause the Anvil reader to throw IOException.
- SC-SOURCE-STABILITY-06 (Regression): a valid unchanged external sidecar is still readable.
- SC-SOURCE-STABILITY-07 (Red): an external sidecar that was already decoded is changed by a *later chunk* callback in the same region; the reader must revalidate all previously observed sidecars before successful return.
- SC-SOURCE-STABILITY-08 (Red): after a region containing an external sidecar has finished scanning, a later playerdata callback mutates that sidecar; the full traversal must reject its final report.

External sidecars are physical payloads referenced by the region header. They must receive the same best-effort no-follow metadata check. This does not detect all same-size/same-timestamp races or establish a consistent Paper snapshot.

Formal CAT machine Gates: **not-run**. This is a new Draft and is not Confirmed.
