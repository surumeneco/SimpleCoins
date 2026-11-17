---
cat_version: '0.2'
kind: work
id: simplecoins.a01.installed-writer-roster
status: draft
process: simplecoins.world-source-discovery
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1Py55wN7KsfMqIWfD9P2IwSDwXSzUBqeR/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/477
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: verification
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/423
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# A01/G0・A02/G3 — 導入JAR構成の一時点整合検査

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/477 |
| https://github.com/surumeneco/SimpleCoins/pull/478 |
| https://github.com/surumeneco/SimpleCoins/pull/479 |
| https://github.com/surumeneco/SimpleCoins/issues/423 |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.world-source-discovery;simplecoins.grants.paper-save-commit | simplecoins.a01.installed-writer-roster | simplecoins.a01.packaged-writer-entrypoints;simplecoins.grants.paper-save-commit |

## Technologies

| Technology |
| --- |
| Python standard library; deterministic SHA-256 and Linux O_NOFOLLOW/inode/ctime witness; actual pinned MineCraftXPlayServer plugin JARs; GitHub Actions; canonical CAT issue-work validation |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a01.installed-writer-roster.md;tools/verify_writer_inventory.py;tests/test_verify_writer_inventory.py;.github/workflows/a01-installed-writer-roster.yml | — | tools/verify_writer_inventory.py;tests/test_verify_writer_inventory.py;.github/workflows/a01-installed-writer-roster.yml |

## Checks

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |

## Gates

| Gate | Status | Evidence |
| --- | --- | --- |
| process-closure | not-run | — |
| model-conformance | not-run | — |
| implementation-conformance | not-run | — |
| semantic-review | not-run | — |
| test-oracle-review | not-run | — |
| red-review | not-run | — |
| pr-review | not-run | — |
| ci | not-run | — |
| merge | not-run | — |
| deployment | not-run | — |
| real-use | not-run | — |

## Motivation

[#477](https://github.com/surumeneco/SimpleCoins/pull/477) audited 24 JARs from pinned XPlayServer `90aa2d0e0a5ea80c705fd67d717f463decaf2a32` and saved one JSON per-JAR SHA256/size/call-reference report. That is a **static reference set**. Without a separate installed-roster check, a new plugin or different JAR can silently be installed, rendering writer-audit evidence inapplicable. G0/G3 cannot correctly claim that all current producers have even been enumerated without distinguishing the audited and installed compositions.

Implement a **standalone read-only preflight** with `--reference-json`, `--installed-plugins` and `--report-json`. It compares the complete top-level `*.jar` roster against the original audited bytes; refuses missing, unexpected, same-size modified, symlinked, nonregular, case-colliding or unstable JARs, and malformatted/duplicate reference rows. For Linux it uses no-follow descriptor opening and checks source inode, size, mtime/ctime before and after SHA256; a changed file during the interval is not reported as stable. Return exit 2 on mismatch; diagnostic JSON includes exact names and classification. Ordinary plugin data directories are not treated as executable top-level JARs. Do not edit any installed files or Java sources.

Run against the **actual pinned 24-JAR server distribution** in CI, using existing `tools/audit_writer_surface.py` as the reference generator, and upload the reference and measured roster reports. In a detached copy of that distribution deliberately corrupt `InvSee++.jar` and require a nonzero exit plus an `altered` classification. Synthetic standard-library tests exercise the other conditions. This is an independent verification Work; don't create a TaskQueue or claim a normative Gate.

## Limits and next dependency

The result is **only a point-in-time comparison of the top-level plugin-JAR byte list**. It does not prove which plugins Paper loads, disable new downloads/reloads, freeze runtime classes, bound Paper's own writers, cover operator-selected JARs absent from this pinned historical distro, protect against post-check changes, or establish the G0/G3 critical section. The pinned distribution does not automatically become the production 1.0.0 deployment manifest; refresh source inventory from the **actual** intended server build (including SimpleCoins and its independent guard) before using the preflight on the real VPS. Do not change `main`, deployment, admission or journal ACK, and do not claim the user has already executed the script.

Following work must combine observed writer identities with a real enforcement mechanism (Paper main-thread serialization, writer-specific veto or durable generation proof) through LIVE capture→saved staging→atomic publication and saved receipt readback→durable ACK, tested under real Paper. Fail-closed when that boundary is not proven. CAT Draft/Confirmed and actual release gates remain separate.

## Acceptance

- Original audited distribution JSON plus exact installed 24-JAR equality is reproducibly accepted with SHA256 and inodes stable at the instant of measurement.
- Actual distribution copy with one modified JAR and synthetic missing/extra/symlink/alias/bad-reference cases is rejected.
- No write to actual distribution, no production Java, no admission change; all CI results and merged develop readback recorded.
