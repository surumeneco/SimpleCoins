# Phase 5 — BlockLocker localized runtime alias contract

Status: **source-verified integration evidence and an open Red defect**, not a Confirmed CAT specification or Paper runtime acceptance. This document records the common source contract for existing Draft Red PRs [#49](https://github.com/surumeneco/SimpleCoins/pull/49), [#53](https://github.com/surumeneco/SimpleCoins/pull/53), and [#68](https://github.com/surumeneco/SimpleCoins/pull/68) without merging their test-only changes or modifying their branches.

## Source-of-truth distinction

BlockLocker **v1.14.1** (tag `v1.14.1`) separates **displayed sign text** from **runtime translation aliases**:

| Source | Exact Git blob SHA | Contract |
| --- | --- | --- |
| `impl/profile/ProfileFactoryImpl.java` | `19ad9bdc4f6af6746df83dc3cd571768e2ad97b5` | `TAG_EVERYONE` is obtained through `translator.getAllWithoutColor`, then wrapped in brackets. `fromDisplayText` strips the displayed line's colors before comparing |
| `impl/ConfigTranslator.java` | `6667932d76e6dad1aa4a54f5dfee56877266377d` | A translation stores original, colored, and uncolored forms. `getAllWithoutColor` returns only uncolored aliases, including localized alternatives |
| `impl/ChestSettingsImpl.java` | `88264cd547e03ca6366992cfb60fb8791df032fb` | `getSimpleLocalizedHeaders(PRIVATE/MORE_USERS)` delegates to `getAllWithoutColor` |
| SimpleCoins `BlockLockerEveryoneText.java` (`develop`) | `8e8900c3c26126de082fbbffa3e30e9712123cc2` | Currently strips displayed text colors, but accepts malformed color-coded runtime alias input without rejecting the entire alias set |
| SimpleCoins `BlockLockerHeaderMatcher.java` (`develop`) | `804f9987c616222ae81b10ce57ab31236f259337` | Currently strips colors from both displayed header **and runtime aliases**, permitting a color-coded runtime alias to be treated as valid |

The upstream contract does **not** mean that colored display text is invalid. A color-coded **displayed** `[Everyone]` or `[Private]` can match a valid uncolored runtime alias. Conversely, the runtime alias collection supplied to SimpleCoins must be independently verified as an uncolored, complete BlockLocker translation snapshot. It is not safe to normalize malformed runtime aliases into valid evidence or to ignore one malformed entry while trusting the rest.

## Reproduced behavior (JDK 21, no Paper)

The exact SimpleCoins `develop` Git blobs above were reproduced and matched using `git hash-object`, compiled on Java 21.0.12.1, and executed in a standalone direct harness:

| Case | Result |
| --- | --- |
| Colored displayed Everyone line + valid uncolored runtime alias | Green |
| Unrelated profile text | Green |
| Bracketed malformed Everyone runtime alias | Green (throws `IOException`) |
| Colored displayed PRIVATE line + valid uncolored runtime alias | Green |
| Valid MORE_USERS header | Green |
| Colliding PRIVATE/MORE_USERS aliases | Green (throws `IOException`) |
| Colored runtime Everyone alias `§aEveryone` | **Expected Red:** malformed alias not rejected |
| Colored runtime PRIVATE alias `§a[Private]` | **Expected Red:** malformed alias normalized into a valid header match |
| Formatting-only Everyone alias alongside valid runtime alias | **Expected Red:** malformed alias set accepted on positive match |
| Whitespace and formatting-only Everyone alias alongside valid runtime alias | **Expected Red:** malformed alias set accepted on negative match |

The original direct harness reproduced 6 Green controls and 2 expected Red cases. A separate exact-source Java probe for PR #68 reproduced 1 Green control and 2 additional expected Red cases. Exact-head Actions run 41393085051 passed CAT Draft syntax lint and completed 63 JUnit tests with exactly 2 intended Red failures. These are not normative CAT Gates or Paper runtime acceptance; all related test branches remain independent.

## Implementation and acceptance boundary

A production fix requires an authorized/Confirmed CAT Process/PI/TCE contract and the formal pre-implementation machine Gates. A Green increment must reject any malformed runtime alias set before performing ownership or Everyone classification, while continuing to accept legitimately colored displayed sign text. The change must preserve ambiguity rejection and verify localized aliases with BlockLocker v1.14.1 on an isolated Paper server.

Header/Everyone text matching **alone** is not proof of attached protection, a primary owner, double-chest consistency, saved-PDC identity, or a complete world snapshot. These remain separate Phase 5 acceptance obligations. No production source change, CAT promotion, Paper test, or deployment is claimed here.
