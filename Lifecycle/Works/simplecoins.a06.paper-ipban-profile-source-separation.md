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
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a06.paper-ipban-profile-source-separation.md;src/test/java/co/surumene/simplecoins/paper/ProfileBanLiveProbe.java;.github/workflows/paper-profile-ban-live-smoke.yml;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml | — | src/test/java/co/surumene/simplecoins/paper/ProfileBanLiveProbe.java;.github/workflows/paper-profile-ban-live-smoke.yml;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/WclServicesProbe.java;.github/workflows/paper-wcl-services-probe.yml |

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
| ci | passed | https://github.com/surumeneco/SimpleCoins/actions/runs/43799018718 (real Paper profile/IP source distinction, not normative CAT Gate) |
| merge | not-run | — |
| deployment | not-run | — |
| real-use | not-run | — |

## Real Paper evidence and limitation

- [Official Paper 26.2 build 134 CI #43799018718](https://github.com/surumeneco/SimpleCoins/actions/runs/43799018718) used exact upstream Paper with zero connected players, admission default-deny and a synthetic TEST-NET-3 address. The main-thread `PaperProfileBanSource.observe` returned `BANNED` for the explicit UUID `7bf1ac01-4b51-4d68-8c6e-982fae4c0001` and `CLEAR` for the different UUID `...0002`, while the separate actual `BanList.Type.IP` contained exactly one IP-string entry `203.0.113.42`. Source marker: `PAPER_A06_IP_BAN_DISTINCT_FROM_PROFILE_UUID_PASSED`.
- The CLEAR UUID remains profile-CLEAR even in the presence of a separate banned IP. This does **not** establish that the account has or had the banned IP, or that an IP-ban source may be ignored in settlement owner classification. An authenticated mapping and declared complete source set are separate A06 obligations.
- Original null UUID and off-main-thread profile observations still fail closed. Pinned Work structure `contract_valid:true`, `semantic_approval_verified:false`; no formal CAT normative Gate passed.

## Test obligations and boundaries

- Boot a hash-verified official Paper 26.2 server on loopback, behind independent default-deny admission; insert an isolated **profile-BAN UUID** in `banned-players.json` and an unrelated fictional TEST-NET-3 IP (`203.0.113.42`) in `banned-ips.json`.
- On actual Paper main thread require `PaperProfileBanSource.observe` to report BANNED for the specific profile and CLEAR for an explicitly distinct profile UUID. Verify null/off-thread fail-close as prior CI.
- Independently enumerate actual Paper `BanList.Type.IP` entries and require one banned IP entry with the exact TEST-NET-3 address. A present IP ban may coexist with a CLEAR profile BAN for another UUID; these are different authorities and cannot be interchanged.
- Inspect that the IP-ban target is an IP string rather than an owner UUID. This test does **not** prove which offline account, if any, used that IP; a source-to-account mapping would require its own authenticated evidence and policy. Do not label the CLEAR UUID as IP-banned by inference.
- Missing IP evidence or unverified external BAN providers must not be reinterpreted as `CLEAR` in the complete BAN coverage coordinator; this Work does not implement the coordinator, inspect actual VPS bans, or decide which external sources are mandatory.
- No production source, real player/IP data, login access, main release or CAT formal Gates. The CAT verification Work remains Draft.

## Actual Paper profile BAN → same BlockLocker/WCL SAVED candidate (bounded independent verification)

- In the already integrated **real Paper 26.2 / BlockLocker 1.15 / WGL/WCL/WWC/WWW** isolated save/restart test, with SAVED candidate inspection and near-Entity reloading in separate Paper JVMs, seed `banned-players.json` with the exact saved WCL commander's fixture UUID and `banned-ips.json` with a distinct TEST-NET-3 IP address. The online player set stays empty; installed service/guard/default-deny and real saved candidate traversal stay unchanged. Do **not** substitute a synthetic `new Source(id, CLEAR)` for the two known owners.
- On the real Paper **primary thread**, use the existing production `PaperProfileBanSource.observe(UUID)` for both the protected BlockLocker/WWC fixture owner (CLEAR profile) and WWW commander (BANNED profile). Require exact source ID, expected UUID-specific status, one real IP entry without an authenticated UUID association, and distinct observed identities; record the verified input status before combining the **test-limited** `BanSourceCoverage`.
- Pass the actual observed profile evidence into the *same* existing `PaperCapturedHybridSettlement.stageCandidateWithCapturedBlockLockerAndSavedWclAndBanCoverage` read-only candidate. Retain physical money 56,000C with D 55,000C and F 1,000C because this fixture contains **no** playerdata: profile CLEAR without playerdata does not turn an owner into P. The earlier BlockLocker-absent 55,000C source regression must remain unchanged.
- Negative acceptance: the absent WWW commander profile sample must refuse the complete candidate; a declared additional `paper-ip` source without trustworthy UUID→IP evidence must **not** be represented as complete, and direct single-source Paper profile CLEAR must never be called all-BAN CLEAR. Failures may not be reported as a successful unblocked P/F/D generation. Recheck true saved file/witness unchanged, no chunk load, no G0 publication, no G3 ACK, no admission.
- A `BANNED` profile plus a distinct IP-ban entry proves only that two **separate Paper lists** were observed; it does not establish IP account attribution, exhaustive current provider roster, source-generation freezing or the full deployed BAN semantics. Do not change the adopted 02A all-BAN requirement, promote a Candidate to Confirmed, update product runtime, or install a plugin/Agent on the actual server. Work remains independent `issue-work` verification, not formal CAT Closure/Model/Implementation Conformance.
