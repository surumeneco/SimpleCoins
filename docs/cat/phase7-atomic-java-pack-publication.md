# Phase 7: atomic Java resource-pack publication (CAT Draft)

Status: **Draft**, not Confirmed. Leaf TASK-20261101-006.

## Process / PI

The generated Java resource-pack ZIP may replace a previously served ZIP only through a filesystem-supported atomic replacement. A non-atomic fallback can expose a partially replaced archive to an HTTP server while it is being served, producing a mismatch with the SHA-1 advertised to clients. If `ATOMIC_MOVE` is unavailable, propagate the checked I/O failure and keep the existing pack intact; never silently retry with a non-atomic move. The caller must still verify the generated archive and hosted URL, and must not advertise an uncommitted pack.

## TCE / vectors

- PACK-ATOMIC-01 (Red): an injected `AtomicMoveNotSupportedException` is propagated after exactly one attempted atomic move, with no second non-atomic move.
- PACK-ATOMIC-02: a supported atomic move replaces the archive with the exact candidate bytes.
- PACK-ATOMIC-03: absent/non-regular temporary archive fails before destination mutation.

Formal CAT machine Gates, real HTTP serving race and JE client verification: **not-run**. No merge or deployment.
