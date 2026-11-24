---
cat_version: '0.2'
kind: work
id: simplecoins.ip-ban-association
status: draft
process: simplecoins.accounting.ip-ban-association
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/issues/4
decision_ref: https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view#IP-BANのUUID帰属（2026-11-25確定）
refs: []
entry: confirmed-spec
mode: implementation
flow: spec-implementation
work_kind: implementation
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/4
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: confirmed
---

# Work: simplecoins.ip-ban-association

## Evidence

| URI |
| --- |
| https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view |

## Scope

Multiple IDs in one cell are separated by `;`.

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.accounting.ip-ban-association | simplecoins.accounting.ip-ban-association | — |

## Technologies

| Technology |
| --- |
| Java; Paper 26.2 |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | src/main/java/co/surumene/simplecoins/domain/IpBanAssociations.java;src/main/java/co/surumene/simplecoins/paper/PaperIpBanAssociations.java;src/main/java/co/surumene/simplecoins/paper/PaperProfileBanSource.java;src/main/java/co/surumene/simplecoins/paper/PaperOwnerStateReconciliation.java;src/main/java/co/surumene/simplecoins/paper/PaperStartupSettlementCoordinator.java;src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java;src/test/java/co/surumene/simplecoins/domain/IpBanAssociationsDriver.java;src/test/java/co/surumene/simplecoins/domain/IpBanAssociationsTest.java;src/test/java/co/surumene/simplecoins/domain/generated/GeneratedIpBanAssociationsTest.java;src/test/java/co/surumene/simplecoins/paper/ProfileBanLiveProbe.java;tests/paper-protected-settlement-runtime/src/main/java/co/surumene/simplecoins/a01probe/RuntimeSettlementProbe.java;docs/cat/confirmed/ip-ban-association-*.md;Lifecycle/Works/simplecoins.ip-ban-association.md;tests/cat/ip-ban-association-junit-binding.json;README.md;.github/workflows/paper-profile-ban-live-smoke.yml;.github/workflows/paper-protected-settlement-runtime.yml | src/main/java/co/surumene/simplecoins/domain/IpBanAssociations.java;src/main/java/co/surumene/simplecoins/paper/PaperIpBanAssociations.java;src/main/java/co/surumene/simplecoins/paper/PaperProfileBanSource.java;src/main/java/co/surumene/simplecoins/paper/PaperOwnerStateReconciliation.java;src/main/java/co/surumene/simplecoins/paper/PaperStartupSettlementCoordinator.java;src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java | src/test/java/co/surumene/simplecoins/domain/IpBanAssociationsDriver.java;src/test/java/co/surumene/simplecoins/domain/IpBanAssociationsTest.java;src/test/java/co/surumene/simplecoins/domain/generated/GeneratedIpBanAssociationsTest.java;src/test/java/co/surumene/simplecoins/paper/ProfileBanLiveProbe.java;tests/paper-protected-settlement-runtime/src/main/java/co/surumene/simplecoins/a01probe/RuntimeSettlementProbe.java |

## Checks

`Command` uses shell-like quoting only to express argv; execution never uses a shell. List cells use `;`.

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |

## Gates

External/semantic gates are reported evidence. `passed` or `not-applicable` requires a concrete evidence/rationale reference.

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

## Compilation

Remove this section when deterministic compilation is not used. Multiple TCE paths in the `TCE` cell are separated by `;` and are assembled before closure/model validation.

| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| @repo:SimpleCoins/docs/cat/confirmed/ip-ban-association-process.md | @repo:SimpleCoins/docs/cat/confirmed/ip-ban-association-pi.md | @repo:SimpleCoins/docs/cat/confirmed/ip-ban-association-tce.md | — | — | — | @work/.cat-flow/cache/ip-ban-model.json | @work/.cat-flow/cache/ip-ban-vectors.json | @work/.cat-flow/cache/ip-ban-queue.json | @repo:SimpleCoins/tests/cat/ip-ban-association-junit-binding.json | @repo:SimpleCoins/src/test/java/co/surumene/simplecoins/domain/generated/GeneratedIpBanAssociationsTest.java | @work/.cat-flow/cache/ip-ban-obligations.json | — | false |


## Scope and acceptance

Current-IP matching, manual override precedence, shared-IP matching, unknown association is not inferred, current list pardon and changed IP. Capture successful joins only and persist last IP separately from editable manual mappings, reload mappings from disk for current queries. Product startup and owner refresh must use the same source; Paper native admission policy is unchanged. Verify actual Paper profile/IP separation and persisted mapping round trip. No real player IP fixtures or external providers asserted.
