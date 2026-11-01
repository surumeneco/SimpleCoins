# Phase 7: read-only administrative settlement queries (CAT Draft)

Status: Draft, not Confirmed. Leaf TASK-20261101-006.

## Process / PI

An administrator may query balances and dead-asset locations only from a previously committed, coherent settlement result. A missing/unregistered UUID must be distinguishable from a registered zero-balance UUID. Dead-asset locations and reasons are read-only evidence, never an invitation to mutate stored assets. The query boundary must reject internally inconsistent statistics or incomplete dead-location totals rather than displaying misleading figures.

The caller must independently authenticate the admin permission and obtain an authoritative published generation. This pure query layer does not claim that a live command, world scan, or publication orchestration is connected.

## TCE

- SC-ADMIN-QUERY-01: a committed result exposes registered zero balance, personal/free/dead totals and dead locations with reason.
- SC-ADMIN-QUERY-02: unknown/unregistered UUID is absent, not a zero-balance registered account.
- SC-ADMIN-QUERY-03: null/invalid result, inconsistent statistics or dead-location totals fail closed.
- SC-ADMIN-QUERY-04: results returned to callers are immutable and cannot alter published totals.

Formal CAT machine Gates, Paper command permission integration and live-world data are not-run.
