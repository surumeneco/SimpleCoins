---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.admin.asset-statistics-concentration-boundaries
status: draft
process: simplecoins.admin
source_refs:
  - https://drive.google.com/file/d/1KxmgwxYdE5dhBxW6mwzaYezxcRaSWx8o/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 7 — asset concentration and arithmetic boundaries (CAT Draft)

Status: Draft, not Confirmed. Regression test preparation for the already integrated `AssetStatistics` pure-domain calculation, not an implemented admin query.

## Targets

| Ref | Aspect |
| --- | --- |
| simplecoins.admin.asset-statistics | Existing complete-snapshot arithmetic, registered population and concentration boundaries |

## Rules

| ID | Condition | Requirement | Verification |
| --- | --- | --- | --- |
| STATS-BOUNDARY-01 | Free assets dominate an ordinary total | Free is a concentration subject but not a registered player | freeAssetsAreAConcentrationSubjectButNotARegisteredPlayer |
| STATS-BOUNDARY-02 | First subject owns exactly half of total | Inclusive half threshold selects one subject | exactHalfThresholdIncludesTheFirstSubject |
| STATS-BOUNDARY-03 | Fractional averages with three registered players | Arithmetic rounds HALF_UP to eight decimals without division by zero | fractionalAveragesAndConcentrationUseEightDecimalHalfUp |
| STATS-BOUNDARY-04 | Personal and free assets exceed signed 64-bit range | BigInteger arithmetic remains exact and dead stays separate | hugeValuesRemainExactAndDeadAssetsStaySeparate |
| STATS-BOUNDARY-05 | Registered population is empty | Undefined population metrics are null, not fabricated zero | absentRegisteredPopulationHasNoInventedAverages |
| STATS-BOUNDARY-06 | Map or amount is null, or registered value is null | Malformed input is rejected before report publication | malformedPopulationAndAmountsAreRejected |

## Test vectors

| ID | Input | Expected |
| --- | --- | --- |
| STATS-BOUNDARY-01 | Two registered players hold 1 each, free assets hold 8 | Ordinary total 10; free counts as one concentration subject; half-share subject count 1, percentage 50% using registered population N=2; max-world percentage 10% |
| STATS-BOUNDARY-02 | Exactly half of ordinary total held by one player | Threshold `2 * accumulated >= total` is inclusive; count 1, not 2 |
| STATS-BOUNDARY-03 | Three registered zero-asset players and one free unit | Shared average and concentration rate round HALF_UP to eight decimals; no division by zero; registered N is denominator |
| STATS-BOUNDARY-04 | Assets greater than signed 64-bit range | BigInteger world and dead accounting do not overflow or silently include dead assets in ordinary total |
| STATS-BOUNDARY-05 | No registered players, positive free assets | World total remains available, but population-dependent averages/max/min/concentration are undefined (null), not fabricated zero |
| STATS-BOUNDARY-06 | Null map, null amounts, or null registered-player value | Reject malformed inputs rather than publishing a partial report |

These tests do not prove complete authoritative asset collection, owner/BAN classification, admin permission checks, location/reason queries, JE/BE rendering, or Paper runtime acceptance. Normative CAT machine Gates and live acceptance remain not-run.
