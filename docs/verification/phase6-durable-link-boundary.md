# Phase 6 — durable journal link-boundary verification

Status: **source-verified fault-injection evidence; known uncorrected write-path defects**. This is not a Confirmed CAT specification, a crash/restart acceptance, or a release sign-off. Existing Draft Red PRs [#35](https://github.com/surumeneco/SimpleCoins/pull/35) (ancestor link) and [#56](https://github.com/surumeneco/SimpleCoins/pull/56) (target link) remain independent and unmerged.

## Exact implementation inspected

SimpleCoins `develop` `src/main/java/co/surumene/simplecoins/domain/DurableFile.java`, Git blob `ae1b732c9a3482e0cee549566ae3069fc9e836c8`, Java 21.

The **read path** uses `NOFOLLOW_LINKS`, a bounded byte budget, SHA-256 checksum verification and a regular-file check. The **write path** creates its parent directories, writes a checksummed temporary file, calls `Files.move(..., ATOMIC_MOVE, REPLACE_EXISTING)`, then forces the parent directory. It does **not** reject an existing symbolic link at the target or a symbolic-link ancestor before the write. Atomic replacement is not a substitute for validating the destination's trust boundary.

## Direct Linux fault-injection results

The exact `DurableFile.java` bytes were reproduced and matched with `git hash-object`, compiled using Java 21.0.12.1, and executed against a disposable temporary directory:

| Probe | Observed result | Required acceptance |
| --- | --- | --- |
| Ordinary checksummed write/read | Green: exact payload roundtrip | Preserve |
| Read through symbolic link | Green: `IOException` | Preserve |
| Corrupted checksum read | Green: `IOException` | Preserve |
| Existing live symlink as write target | **Expected Red:** link replaced by a journal record | Reject without replacing link or touching referent |
| Existing dangling symlink as write target | **Expected Red:** link replaced by a journal record | Reject without creating a record at that path |
| Symlink in ancestor directory | **Expected Red:** write follows link and creates a record through it | Reject before any journal write |
| External referent sentinel after target-link write | Green: referent unchanged | Preserve, but this does not make link replacement acceptable |

Total: **7 passing direct controls, 3 reproduced expected Red link-boundary cases**. The temporary directory and its `outside/` subdirectory were both isolated test fixtures; no server files were modified. These are direct JDK observations, not a JUnit/Gradle/CI result or a crash-consistency proof.

## Fix and integration boundary

The current implementation cannot be treated as fail-closed for untrusted journal paths. An authorized CAT Process/PI/TCE decision must establish the write-path link policy and the required machine Gates before a production Green fix. The fix must preserve atomic replacement and directory durability for ordinary files while rejecting target and ancestor links without a non-atomic fallback.

The acceptance suite must also exercise power-loss/crash recovery, stale receipts, replay idempotency, corrupt/missing non-tail records, concurrent writer constraints, and the final publication gate. In particular, successful `DurableFile.read` checks do not establish the correctness of `GrantLedger.wasApplied` (separate Draft Red PR #58). No production code, CAT promotion, Paper runtime, or deployment is claimed by this document.
