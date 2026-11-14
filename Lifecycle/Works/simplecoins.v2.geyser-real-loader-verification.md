---
cat_version: '0.2'
kind: work
id: simplecoins.v2.geyser-real-loader-verification
status: draft
process: simplecoins.presentation.geyser-real-loader-verification
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: verification
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/6
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# Work: Geyser-Spigot real-provider restart validation

## Evidence

| URI |
| --- |
| https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view |
| https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view |
| https://github.com/surumeneco/SimpleCoins/pull/407 |
| https://geysermc.org/wiki/geyser/custom-items/ |
| https://geysermc.org/wiki/api/downloads.geysermc.org/build-specific/ |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.presentation.geyser-real-loader-verification | simplecoins.v2.geyser-real-loader-verification | simplecoins.settlement.default-deny;simplecoins.currency.pdc-stable |

## Technologies

| Technology |
| --- |
| Java 25; Paper 26.2 build 134; authentic Geyser-Spigot JAR; GitHub Actions |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| simplecoins | ../.. | develop | feature/* | Lifecycle/Works/*;.github/workflows/paper-geyser-real-loader-smoke.yml | — | .github/workflows/paper-geyser-real-loader-smoke.yml |

## Checks

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |
| ci-geyser-real-restart | verification | GitHub Actions | . | .github/workflows/paper-geyser-real-loader-smoke.yml | real-provider-startup;staging;restart | official pinned Paper and Geyser JAR | action logs;owned pack;mapping;no-load-errors | 900 |

## Gates

| Gate | Status | Evidence |
| --- | --- | --- |
| process-closure | not-run | No legitimate Confirmed Process/PI/TCE authority |
| model-conformance | not-run | Verification-only issue-work |
| implementation-conformance | not-run | No production change |
| ci | not-run | Real-provider workflow outcome pending |
| merge | not-run | Pending CI |
| real-use | not-run | No Bedrock client or Nexo available |

## Verification contract

- Use an authentic Geyser-Spigot JAR from the official version/build metadata, not the test-only fake provider from PR #407. Verify its published SHA-256 before loading.
- Boot Paper with Geyser to let Geyser write its actual data folder and configuration. Enable `gameplay.enable-custom-content` in that configuration.
- With the independent admission guard, boot SimpleCoins: verify that the currency archives are generated, files are staged in the real Geyser data folder, original and destination bytes match, and admission stays closed.
- Restart the actual Geyser/Paper runtime: detect startup/config/mapping/pack load errors and verify the staged files remain. The test must fail rather than silently report success if Geyser cannot start or cannot accept the source file.
- Separate a real Geyser **startup/loader smoke** from actual Geyser custom-item registration and a Bedrock client receiving/rendering the resources. Without positive runtime proof, report the latter as **unverified**, even if startup succeeds.
- Keep publication disabled and avoid any edits to production Java code, Nexo install, main branch, or deployed server. Existing speculative CAT candidates are not normative Confirmed sources. This is a verification Work in issue-work, not a formal CAT conformance Gate.
