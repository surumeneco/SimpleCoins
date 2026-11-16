---
cat_version: '0.2'
kind: work
id: simplecoins.a06.paper-ipban-profile-source-separation
status: draft
process: simplecoins.settlement.ownership-evidence
source_refs:
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
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

# A06 — PaperプロフィールBANとIP BANのソース分離検証

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/issues/4 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/PaperProfileBanSource.java |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.settlement.ownership-evidence | simplecoins.a06.paper-ipban-profile-source-separation | simplecoins.ban-source-coverage;simplecoins.profile-ban-source |

## Technologies

| Technology |
| --- |
| Java 25; actual official Paper 26.2 build 134; profile bans and IP BanList on loopback-only test server |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a06.paper-ipban-profile-source-separation.md;src/test/java/co/surumene/simplecoins/paper/ProfileBanLiveProbe.java;.github/workflows/paper-profile-ban-live-smoke.yml | — | src/test/java/co/surumene/simplecoins/paper/ProfileBanLiveProbe.java;.github/workflows/paper-profile-ban-live-smoke.yml |

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

## Test obligations and boundaries

- Boot a hash-verified official Paper 26.2 server on loopback, behind independent default-deny admission; insert an isolated **profile-BAN UUID** in `banned-players.json` and an unrelated fictional TEST-NET-3 IP (`203.0.113.42`) in `banned-ips.json`.
- On actual Paper main thread require `PaperProfileBanSource.observe` to report BANNED for the specific profile and CLEAR for an explicitly distinct profile UUID. Verify null/off-thread fail-close as prior CI.
- Independently enumerate actual Paper `BanList.Type.IP` entries and require one banned IP entry with the exact TEST-NET-3 address. A present IP ban may coexist with a CLEAR profile BAN for another UUID; these are different authorities and cannot be interchanged.
- Inspect that the IP-ban target is an IP string rather than an owner UUID. This test does **not** prove which offline account, if any, used that IP; a source-to-account mapping would require its own authenticated evidence and policy. Do not label the CLEAR UUID as IP-banned by inference.
- Missing IP evidence or unverified external BAN providers must not be reinterpreted as `CLEAR` in the complete BAN coverage coordinator; this Work does not implement the coordinator, inspect actual VPS bans, or decide which external sources are mandatory.
- No production source, real player/IP data, login access, main release or CAT formal Gates. The CAT verification Work remains Draft.
