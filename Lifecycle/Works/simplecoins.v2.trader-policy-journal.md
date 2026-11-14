---
cat_version: '0.2'
kind: work
id: simplecoins.v2.trader-policy-journal
status: draft
process: simplecoins.trader.policy-journal
source_refs:
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: implementation
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/6
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# Work: simplecoins.v2.trader-policy-journal

## Evidence

| URI |
| --- |
| https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view |
| https://github.com/surumeneco/SimpleCoins/pull/318 |
| https://github.com/surumeneco/SimpleCoins/issues/6 |

## Scope

Multiple IDs in one cell are separated by `;`.

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.trader.policy-journal | simplecoins.v2.trader-policy-journal | simplecoins.settlement.admission-barrier |

## Technologies

| Technology |
| --- |
| Java 25; Paper 26.2; JUnit/Gradle; YAML |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| simplecoins | ../.. | develop | feature/* | Lifecycle/Works/*;src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java;src/main/java/co/surumene/simplecoins/domain/Trader*;src/main/java/co/surumene/simplecoins/paper/Trader*;src/main/java/co/surumene/simplecoins/paper/WanderingTraderDropConfigReader.java;src/main/resources/config.yml;src/test/java/co/surumene/simplecoins/domain/Trader*;src/test/java/co/surumene/simplecoins/paper/WanderingTraderDropConfigReaderTest.java | src/main/java/co/surumene/simplecoins/SimpleCoinsPlugin.java;src/main/java/co/surumene/simplecoins/domain/Trader*;src/main/java/co/surumene/simplecoins/paper/Trader*;src/main/java/co/surumene/simplecoins/paper/WanderingTraderDropConfigReader.java;src/main/resources/config.yml | src/test/java/co/surumene/simplecoins/domain/Trader*;src/test/java/co/surumene/simplecoins/paper/WanderingTraderDropConfigReaderTest.java |

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
| ci | passed | https://github.com/surumeneco/SimpleCoins/actions/runs/43569953798 (Gradle/JUnit), https://github.com/surumeneco/SimpleCoins/actions/runs/43569953782 (guarded Paper), https://github.com/surumeneco/SimpleCoins/actions/runs/43569953840 (coin Paper), https://github.com/surumeneco/SimpleCoins/actions/runs/43569953786 (wallet Paper), https://github.com/surumeneco/SimpleCoins/actions/runs/43569953818 (static) |
| merge | not-run | — |
| deployment | not-run | — |
| real-use | not-run | — |

## Compilation

Remove this section when deterministic compilation is not used. Multiple TCE paths in the `TCE` cell are separated by `;` and are assembled before closure/model validation.

| Process | PI | TCE | Domain rule | Common rules | Domain specs | Model | Vectors | Queue | Binding | Tests | Obligations | Conformance binding | Allow draft |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
## Work scope and source authority

- Product design `03_運営設定・コマンド.md` records the user-directed 2026-11-12 change: initial trader death probability **0.1**, enabled true, exactly one coin when selected, all 9 denominations with weights `256/128/64/32/16/8/4/2/1` (sum 511). This supersedes the older `0.5` code/test expectation in unmerged PR #318.
- The current `develop` at issue intake `34f3bcca389b1366d2c9734fd90f95ff54a49370` has pure `WanderingTraderDropPolicy` and `TraderDropStackPlan` but no Paper config parser, no trader issuance journal, and no death-event listener registration.
- Restore/review existing candidate Paper config parser, issuance receipt journal and test cases, without declaring their historical branch production-ready; stage the event adapter as **unregistered source** only. Actively validate all supplied config fields on bootstrap, with defaults in `config.yml` consistent with the current user direction. Do **not** create physical currency while monetary settlement/normal mutation accounting are disconnected.
- Test positive/nonpositive config, nine weights, replay and crash receipt states, recovery fail-closed; full real-world death/event, physical spawn, and crash-resilient settlement reconciliation are separate blocked activities until all authoritative sources/owner evidence and admission gates are established.

## Observed test-first evidence

- On head `1a8274319e4ad1567b71f359332c351222c15a7f`, [run #43569737469](https://github.com/surumeneco/SimpleCoins/actions/runs/43569737469) failed `compileTestJava` as expected because `TraderIssuanceJournal` and `WanderingTraderDropConfigReader` were not yet present. This is a source-observed pre-implementation failure, not a formal CAT Red Gate.
- Production Java at `5b692e7275ff07287d8833a6f28fe745f3367770` restores the reviewed parser, read-only staged death-event adapter and crash-safe receipt journal. `config.yml` now carries **0.1** chance, initial one coin and the 9 denomination 511-total weight profile; the old 50% JUnit oracle was corrected to 10%. Main startup validates the policy and keeps the death listener unregistered.
- [JUnit/Gradle](https://github.com/surumeneco/SimpleCoins/actions/runs/43569953798), [guarded Paper bootstrap including corrupt monetary journal](https://github.com/surumeneco/SimpleCoins/actions/runs/43569953782), [coin registry](https://github.com/surumeneco/SimpleCoins/actions/runs/43569953840), [wallet registry](https://github.com/surumeneco/SimpleCoins/actions/runs/43569953786), [Phase6 static](https://github.com/surumeneco/SimpleCoins/actions/runs/43569953818) succeeded at the code head.
- There is no physical trader drop and no issuance accounting until all-world settlement; `TraderIssuanceJournal` EVENT_RECORDED is **not** evidence of saved world currency. Paper death/issuance real-use and product normative CAT Process/PI/TCE Gates remain **not-run**.

## Acceptance boundaries

1. Unit test strictly rejects invalid probabilities, bad weights, conflicting aliases and overflow; explicit existing config values must not be overwritten. Default 10% uses actual policy object with seeded RNG and checks all 9 denominations reachable.
2. Single-writer trader journal rejects corrupt/duplicate receipts, remembers positive PREPARED/EVENT_RECORDED across restart, preserves NO_DROP, and never marks event mutation as world persistence.
3. Plugin validates policy config after normal journal startup verification but deliberately does not register a trader death listener or grant admission. Missing/failed config must retain fail-stop.
4. Independent Java/Paper CI must be Green; the CAT Work remains Draft and product process-closure/model/implementation Gates remain not-run without a Confirmed normative decision.
5. User acceptance and `main` deployment are not part of this Work. Broader SimpleCoins 1.0.0 is not production-ready solely because these components pass.
