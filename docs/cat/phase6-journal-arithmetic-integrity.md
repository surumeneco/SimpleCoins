# Phase 6: corrupted issuance journal arithmetic (CAT Draft)

Status: **Draft** (not Confirmed). Leaf TASK-20261101-005.

## Process / PI

A checksummed journal record may still be semantically invalid. During restart/replay, an impossible denomination × coin-count product that overflows signed 64-bit arithmetic must be reported as a checked `IOException`, like other malformed persisted records. No partially recovered journal is returned, and no new grant may be issued from that state.

This change affects replay error classification only; valid typed reservations, journal sequence checks, and receipt semantics remain unchanged.

## TCE

- SC-JOURNAL-ARITH-01 (Red): a checksummed typed grant with `coinCount=Long.MAX_VALUE` and denomination 10000 fails constructor replay with `IOException` rather than escaping `ArithmeticException`.
- SC-JOURNAL-ARITH-02 (Regression): valid typed reservation survives restart and preserves denomination/count.

Formal CAT machine Gates not-run; Draft not Confirmed. No merge, deployment, or issuance.
