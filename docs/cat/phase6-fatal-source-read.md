# Phase 6: fatal source-read failure closes admission (CAT Draft)

Status: **Draft**, not Confirmed. Leaf: TASK-20261101-005.

## Process / PI

A source may fail with an unchecked exception or JVM `Error` while the settlement barrier is staging. A partially read source must never be marked complete. Any failure in the bounded read callback invalidates the entire staging attempt, and no join permit may be issued. The original failure must propagate without being converted into a successful read.

This tests the existing in-memory barrier contract only; it does not establish a coherent Paper snapshot or durable delta journal.

## TCE

- SETTLEMENT-BARRIER-10: an unchecked exception during source read sets the barrier to FAILED and propagates the exception.
- SETTLEMENT-BARRIER-11: an `AssertionError` during source read sets the barrier to FAILED and propagates the Error.
- SETTLEMENT-BARRIER-12: a null source reader fails closed, without marking its source complete.
- SETTLEMENT-BARRIER-13: after failure, a subsequent successful callback cannot revive the same barrier.

## Gate status

Formal CAT machine gates and live Paper fault injection **not-run**. This Draft does not authorize settlement publication, join admission or merge.
