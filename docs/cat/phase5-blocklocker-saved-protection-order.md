---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.blocklocker.saved-protection-order
status: draft
process: simplecoins.blocklocker
source_refs:
  - https://drive.google.com/file/d/1sn-FXbU7nFDr6yc6YxPAe2V6U6mV2CZD/view
  - https://github.com/surumeneco/SimpleCoins/blob/d71683ad7f9e29de24af2f7c6de4663909990b38/src/main/java/co/surumene/simplecoins/domain/BlockLockerSavedProtectionResolver.java
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5: saved BlockLocker protection aggregation order (CAT Draft)

## Targets

| Ref | Aspect |
| --- | --- |
| simplecoins.blocklocker.saved-protection | Caller-supplied coherent saved sign and palette evidence |
| simplecoins.settlement | Read-only owner evidence, not settlement authorization |

## Rules

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| BL-ORDER-01 | 同一コンテナにPRIVATEとEveryone付きMORE_USERSが物理的に接続し、PRIVATEのUUIDを正常確認できる | 保存済みsign mapの列挙順によらずFREEを返す | BL-ORDER-01 |
| BL-ORDER-02 | PRIVATEのidentityがUNKNOWNでEveryone付きMORE_USERSが接続している | 列挙順によらずUNKNOWNのままにする | BL-ORDER-02 |
| BL-ORDER-03 | PRIVATEが2枚接続している | 両方の列挙順でIOExceptionを返し、恣意的な先勝ちをしない | BL-ORDER-03 |
| BL-ORDER-04 | PRIVATEに接続していないEveryone付きMORE_USERSが混入する | 列挙順によらず検証済みPRIVATEの所有者を維持する | BL-ORDER-04 |

## Process / PI (source observation)

Input: a caller-proven complete/coherent saved-sign map and a matching map of authenticated block palette states. `BlockLockerSavedProtectionResolver.single` builds verified signs, then `BlockLockerProtectionResolution.single` checks physical attachment and aggregates ownership. These tests characterize the pinned existing source only; they do not prove that the caller actually acquired a coherent snapshot, resolved identities authoritatively, or loaded BlockLocker.

## TestModel / TCE

- BL-ORDER-01: insert PRIVATE then MORE_USERS and reverse; both yield FREE.
- BL-ORDER-02: repeat with unresolved PRIVATE; both yield UNKNOWN.
- BL-ORDER-03: two attached PRIVATE signs in either order; both throw checked IOException.
- BL-ORDER-04: insert a detached MORE_USERS before/after a verified PRIVATE; both yield PERSONAL with the same UUID.

Status: **Draft / code-to-spec observation**, not Confirmed. No new product behavior or ownership policy is authorized. Formal CAT process-closure, TestModel generation, model-conformance, vectors, Queue, current-item, implementation-conformance and Paper runtime gates are **not-run**.