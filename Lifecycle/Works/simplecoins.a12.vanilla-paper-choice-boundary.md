---
cat_version: '0.2'
kind: work
id: simplecoins.a12.vanilla-paper-choice-boundary
status: draft
process: simplecoins.currency.crafting-exclusion
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://drive.google.com/file/d/1Py55wN7KsfMqIWfD9P2IwSDwXSzUBqeR/view
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

# A12 — Paper 26.2でのPAPER貨幣材料候補と既存クラフトガード観測

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/CoinExchangeRecipes.java |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/paper/CoinItemCodec.java |
| https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.currency.crafting-exclusion | simplecoins.a12.vanilla-paper-choice-boundary | simplecoins.v0.coin-recipe-registration |

## Technologies

| Technology |
| --- |
| Java 25; Paper 26.2 build 134; live Bukkit recipe registry, event handler inspection |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a12.vanilla-paper-choice-boundary.md;tests/paper-vanilla-choice-probe/*;.github/workflows/paper-vanilla-choice-probe.yml | — | tests/paper-vanilla-choice-probe/*;.github/workflows/paper-vanilla-choice-probe.yml |

## Checks

`Command` uses shell-like quoting only to express argv; execution never uses a shell. List cells use `;`.

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

## Source-backed verification scope

- Game behavior is already defined by 01/04: all denominations are PAPER but can only be consumed by equal-value SimpleCoins exchange, never ordinary crafting. This Work **does not revise** the normative rule.
- The current `CoinExchangeRecipes` source registers 20 shapeless exchanges and listeners for `PrepareItemCraftEvent`, `CraftItemEvent`, and `CrafterCraftEvent`; their existence is not proof of exhaustive compatibility or event delivery in a real server.
- On isolated official Paper 26.2 with admission default-deny, create an authentic PDC/model currency item using the product `CoinItemCodec`. Enumerate actual registered vanilla recipes (namespace `minecraft`), inspect shaped/shapeless ingredient choices and establish whether the vanilla `RecipeChoice.MaterialChoice` recognizes this exact modified PAPER stack.
- Independently verify actual registration of the current plugin's three craft/crafter listeners on Paper; do not fabricate a successful cancellation event. A vanilla material choice matching the coin demonstrates risk *without the listener*, not proof of successful player crafting or a bypass.
- Log exact matching recipe keys and count, codec value and event listener presence. Unknown API behaviors, zero matches or missing listeners fail the bounded probe. Do not modify or remove any production recipe, registered guards, or actual player inventory.
- For remaining A12 integration independently exercise actual JE crafting matrix, shift result, Crafter, trading, third-party recipe, and disconnect/save accounting under controlled test clients. Those are **not** certified by this smaller observable registry probe.

## Authority

Verification-only `issue-work` Draft. Any behavioral source change requires normative Confirmed Process/PI/TCE and official Gates. CI green is not normative confirmation, completed consumption coverage, G0 or admission approval.
