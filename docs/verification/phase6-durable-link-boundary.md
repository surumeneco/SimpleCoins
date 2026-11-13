# Phase 6 — durable journal link-boundary verification

Status: **historical 2026-11-02 Red evidence; static link-boundary remediation is now covered by newer PRs**. This is not a Confirmed CAT specification, race-free namespace guarantee, crash/restart acceptance, or release sign-off. Historical Draft PRs [#35](https://github.com/surumeneco/SimpleCoins/pull/35) and [#56](https://github.com/surumeneco/SimpleCoins/pull/56) remain unmerged; their tests were rebased independently onto current `develop` in PR #379 and PR #380.

## Exact implementation inspected (historical 2026-11-02)

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

## Remediation and remaining boundary (2026-11-13)

- [PR #379](https://github.com/surumeneco/SimpleCoins/pull/379) merged the **existing final-target symlink** fail-closed check to `develop` (merge `8131d9a369cee804c403822beb55be7f4686a4b5`). Its latest feature-head CI `43391168221` was Green after two expected Red failures (`43390957912`).
- [PR #380](https://github.com/surumeneco/SimpleCoins/pull/380) carries the separate **ancestor directory symlink** checks in `DurableFile.write`, `writeNew` and `read` with a symlink-free nested-directory control. Red CI `43391542772` reported 511 tests, exactly two expected failures. Implementation commit `a6bd1a171db82672fe0be1583338b8b876f0d398` is on the feature branch; merge and Green are verified separately.
- These are best-effort static namespace checks. Concurrent link swaps (TOCTOU) remain possible without a trusted directory handle or exclusive root control; do not represent this as race-free path integrity.
- Both CAT documents remain **Draft**. Process/PI/TCE normative confirmation and formal CAT process/model/implementation-conformance Gates are **not-run**.

Final release acceptance still requires crash/power-loss recovery, stale receipts, replay idempotency, corrupt/missing non-tail records, concurrent writer exclusion, journal/root identity, full publication/admission gating and Paper/Geyser on-server verification. In particular, a checksummed record read alone does not prove `GrantLedger.wasApplied` correctness. No `main` merge or production deployment is authorized by this historical report.
