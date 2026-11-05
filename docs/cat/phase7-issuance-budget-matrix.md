---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase7.issuance-budget-matrix
status: draft
process: simplecoins.admin-give
source_refs:
  - https://drive.google.com/file/d/1KxmgwxYdE5dhBxW6mwzaYezxcRaSWx8o/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# IssuanceBudget: global overflow-safe budget matrix

## Scope and authority

TASK-20261101-006. Code-to-spec characterization of the *existing*
`IssuanceBudget.java` (blob `607b004cfab076143e2add6dd8f470a622abb0c1`)
on `develop` commit `c317b2488d342c8827335043aed5ae736e85e3ea`.

**Draft, not Confirmed**: no production Java changes are proposed. The tests
preserve existing command-global upper-bound behavior and do not authorize
online/offline give implementation, resource-pack delivery, or Paper deployment.

## Observations and validation vectors

| ID | Observed property | Direct validation |
| --- | --- | --- |
| GIVE-BUDGET-MATRIX-01 | `requireBudget(stacks, recipients)` accepts exactly when both positive and their mathematical product is at most 1024 | 17 positive stack counts × 16 positive recipient counts, compared to `BigInteger` multiplication (272 combinations) |
| GIVE-BUDGET-MATRIX-02 | `coinStacks(count)` is ceiling(count / 64) without overflow, including `Long.MAX_VALUE` | 12 representative boundaries checked by unbounded arithmetic |
| GIVE-BUDGET-MATRIX-03 | Zero and negative stack counts/recipients and coin counts are invalid | six explicit rejected cases |

The present implementation compares `stacksPerTarget > MAX_STACKS_PER_COMMAND / targets`
rather than multiplying two bounded platform types, preventing an overflowing
product from bypassing the global issuance limit.

## Verification and limits

- Direct JDK 21 `javac`/`java` against exact-source SHA-verified Git blob:
  290/290 property checks passed.
- JUnit 5 matrix is a separate repository test; verify actual JDK25 Gradle
  execution on the PR, including tests executed without skips.
- This does not prove grant ledger idempotence, recipient permission, complete
  inventories, JE/BE parity or Paper live behavior. Normative CAT process/model
  Gates remain not-run; this Draft is only existing-code characterization.
