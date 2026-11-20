---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase7.exchange-recipes
status: draft
process: simplecoins.currency
source_refs:
  - https://drive.google.com/file/d/1KxmgwxYdE5dhBxW6mwzaYezxcRaSWx8o/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 7: denomination exchange recipe domain (CAT Draft)

Status: **Draft**, not Confirmed. Existing implementation originates in PR #2; this is an isolated extraction/test contract, not a new currency issuance feature.

## Acceptance vectors

| ID | Input | Expected |
| --- | --- | --- |
| SC-EXCHANGE-01 | `ExchangeRecipe.standard()` | Exactly 20 distinct immutable recipes |
| SC-EXCHANGE-02 | Each standard recipe | Input and output monetary value equal, counts positive and within declared bounds |
| SC-EXCHANGE-03 | Invalid null denomination, zero/negative/oversized count, or unequal monetary value | Constructor rejects without issuing currency |
| SC-EXCHANGE-04 | Each of the 4 supported decimal scales | Five distinct expected exchanges, no duplicate tuple |

This domain object is a value-preserving recipe description only. Bukkit recipe registration, item authenticity, JE/BE GUI, Nexo and server acceptance are separate. Formal CAT machine Gates not-run; Draft not Confirmed. No main/develop merge or release is implied.
