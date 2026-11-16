---
cat_version: '0.2'
kind: work
id: simplecoins.a04.blocklocker-saved-sign-paper-source
status: draft
process: simplecoins.settlement.ownership-evidence
source_refs:
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/pull/402
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: verification
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/4
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# A04 — BlockLocker 1.15保護看板の実Paper保存ソースを検査

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/pull/401 |
| https://github.com/surumeneco/SimpleCoins/pull/402 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/BlockLockerSavedSignCandidates.java |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.settlement.ownership-evidence | simplecoins.a04.blocklocker-saved-sign-paper-source | simplecoins.o0.blocklocker-115-live-owner-evidence |

## Technologies

| Technology |
| --- |
| Java 25; official Paper 26.2 build 134; pinned BlockLocker 1.15 SHA256; real terrain Anvil region |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a04.blocklocker-saved-sign-paper-source.md;tests/paper-blocklocker-smoke/OfflineBlockLockerSignProbe.java;.github/workflows/paper-blocklocker-115-live-smoke.yml | — | tests/paper-blocklocker-smoke/OfflineBlockLockerSignProbe.java;.github/workflows/paper-blocklocker-115-live-smoke.yml |

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

## Bounded evidence acceptance

- Use the existing **real** Paper 26.2 + verified BlockLocker v1.15 test (owner UUID, Everyone priority, double chest across chunks, back-side Everyone). Run its test plugin and allow clean Paper world save before inspecting the actual region file, without introducing a real player or touching any VPS world.
- In a separate standalone JVM with NO Paper instance or chunk loading, parse the actual saved `world/dimensions/minecraft/overworld/region/r.0.0.mca` using production `AnvilRegionReader.scan` with fixed compressed/inflated limits; dispatch the saved NBT to the existing production `BlockLockerSavedSignCandidates.extractEvidence` reader.
- Verify all five deliberately placed protection signs at X=6,9,12,15,21 and Z=5 within actual Chunks (0,0)/(1,0): front header `[Private]`, unambiguous UUID-backed owner text at X6/X15, explicit `[Everyone]` at X9 front and X21 back, and text-only unknown primary at X12. Check sign positions and persisted raw front/back text **without conflating a display name with a verified owner UUID**.
- A sign record's absence, malformed NBT, duplicate position or inconsistent Chunk coordinates must fail; missing protection evidence is not a zero asset or automatically Free. Do not extrapolate from a protected sign to a verified attached chest without validating terrain topology and applicable BlockLocker semantics.
- This test proves only the ability to discover the deliberately saved source hints and text and their Paper/BlockLocker physical persistence. The complete source-to-chest attachment, legacy signs, double-chest deduplication, changing protections and global saved/LIVE source epoch are separate remaining work.
- No product code, real VPS/players, money, admission or normative CAT gates are changed; Work stays Draft.
