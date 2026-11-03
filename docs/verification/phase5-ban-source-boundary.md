# Phase 5 — BAN / IP BAN evidence boundary

Status: **source-verified verification handoff** for `TASK-20261101-004`. This is not a Confirmed CAT specification, a deployed adapter, or Paper runtime acceptance. The source under inspection is the existing, unmerged Phase 5 owner-evidence branch (`feature/owner-evidence-20261101`); this document is independent of that branch.

## Verified implementation boundary

| Source | Git blob SHA | Verified responsibility |
| --- | --- | --- |
| `BanEvidence.java` (`develop` and owner-evidence branch) | `0e3f29c2a85fca1d711a928b9db7637193805299` | Combines caller-declared required sources; missing/UNKNOWN cannot prove CLEAR, known BANNED dominates; rejects malformed/duplicate/unexpected source IDs |
| `PaperBanEvidenceCollector.java` (owner-evidence branch) | `8f31bb08de458e6e30da74882aca9794dc889cb7` | Aggregates profile, IP and declared external-plugin results for one UUID; external plugin discovery and UUID-to-IP association remain caller responsibilities |
| `PaperIpBanEvidenceAdapter.java` (owner-evidence branch) | `32a6778481994c9f847cae08207867f59a9f7961` | Checks the supplied addresses against Paper's IP-ban list; does **not** discover addresses associated with an offline UUID |
| `PaperBanEvidenceAdapter.java` (owner-evidence branch) | `1aa58790ec9cf9e4841817d270879624681a35ba` | Reads the profile BAN result for the requested UUID; does not establish external-plugin or IP-ban completeness |

The `addressMappingComplete` and `externalInventoryComplete` booleans are **caller assertions**, not independently validated proof objects. A `CLEAR` returned by the pure collector is usable for settlement only when the caller has separately established the complete, authoritative source inventory, current UUID/address associations, lookup freshness, and a consistent snapshot. No static test of these methods can establish those external facts.

## Decision and failure matrix

| Evidence | Source-verified result | Required downstream interpretation |
| --- | --- | --- |
| Complete profile/IP/external source set, all lookups CLEAR | `CLEAR` | Eligible for later owner classification **only if** completeness assertions are independently justified |
| Incomplete external-plugin inventory, no positive BAN | `UNKNOWN` | Do not stage as an unbanned owner |
| Incomplete/empty UUID-to-address mapping, no positive BAN | `UNKNOWN` | Do not infer absence of IP bans |
| Null/failed external or IP lookup, no positive BAN | `UNKNOWN` | Preserve incomplete-source evidence |
| Confirmed BAN from any required source with others UNKNOWN/missing | `BANNED` | Positive BAN takes precedence; do not downgrade to UNKNOWN/CLEAR |
| Duplicate normalized external plugin ID, invalid source ID, unexpected observation | `IOException` | Reject the source set; do not retry by silently dropping evidence |
| Missing owner UUID or mismatched profile/IP source ID | `IOException` | Do not perform settlement classification |
| Null address within an otherwise complete address set | `UNKNOWN` unless another verified address is BANNED | The set is not a complete valid mapping |
| Provider lookup result `false` with incomplete discovery | `UNKNOWN` | A negative response from one plugin cannot prove all plugins clear |

### Unicode source-ID alias boundary (Draft; not a decided grammar)

The current `BanEvidence.combine` source (blob `0e3f29c2a85fca1d711a928b9db7637193805299`) uses `raw.isBlank()` for empty-ID rejection, then `raw.trim().toLowerCase(Locale.ROOT)` as its canonical key. Java `String.trim()` does not remove `U+2003` EM SPACE or `U+00A0` NO-BREAK SPACE. Thus `paper` and `paper` followed by `U+2003` (or preceded by `U+00A0`) can be counted as **two distinct declared sources** in the current pure-Java aggregator even if they refer to one intended provider. The aggregator has no external provider registry with which to prove that identity.

[Draft PR #115](https://github.com/surumeneco/SimpleCoins/pull/115) records two expected-Red alias tests and one Green undeclared-source control. On 2026-11-03, the exact `develop` `BanEvidence.java` source bytes were reproduced locally (`git hash-object` matched `0e3f29c2a85fca1d711a928b9db7637193805299`) and compiled with Java 21 (`javac --release 21`). A direct harness invoking the **actual** `BanEvidence.combine` yielded two expected Red observations (`paper` + U+2003 and U+00A0 + `paper` each returned `CLEAR` instead of rejecting a candidate alias), and three Green controls (undeclared U+2003 observation rejected, ASCII-whitespace duplicate rejected, ordinary single-source CLEAR). This is byte-identical **production-source direct execution**, but not a Gradle/JUnit run, CAT machine Gate or Paper runtime result. The proposed choice between rejecting noncanonical Unicode, normalizing a defined subset, or restricting IDs to an explicit grammar remains a **normative CAT decision**, not an implementation authorization. In particular, neither a set of distinct strings nor a returned `CLEAR` proves that the declared source inventory represents distinct authoritative providers.

## Reproduced direct checks

On 2026-11-02, Java 21.0.12.1 compiled the **exact Git blob bytes** of `BanEvidence`, `PaperBanEvidenceCollector`, and `PaperIpBanEvidenceAdapter`; `git hash-object` matched the three SHAs above. Only Paper-dependent `readPaper` entry points were stubbed; the actual `read`, `combine`, and `evaluate` implementations were executed.

- Collector/combine harness: **21/21 assertions passed** (complete/incomplete inventory, profile/IP/external UNKNOWN, BANNED precedence, normalized IDs, duplicate and invalid sources, missing owner, lookup null/exception).
- IP adapter harness: **11/11 assertions passed** (complete/empty/incomplete address sets, null address, null/throwing lookup, BANNED precedence).
- Total: **32/32 direct assertions passed**. These are standalone Java assertions, **not** JUnit, Gradle, CAT machine Gates, or Paper integration results.

## Remaining independent acceptance evidence

Before treating a real owner as `CLEAR`, obtain and record the authoritative source list (including installed/disabled external BAN plugins), the owner UUID-to-IP association source and its coverage period, the specific lookup results, plugin/API versions, and snapshot time. A missing source must remain `UNKNOWN`; the current branch does not supply a complete discovery or mapping implementation.

The live acceptance cases are `P5-OWNER-04` through `P5-OWNER-06` in the owner-evidence branch's `docs/verification/phase5-owner-ban-paper-acceptance.md`. Those cases, formal CAT Process/PI/TCE confirmation and machine Gates, and Paper 26.2 runtime integration are **not-run**. This document does not authorize or imply a production merge of the owner-evidence branch.
