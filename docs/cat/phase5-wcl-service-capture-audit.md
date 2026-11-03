---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase5.wcl-service-capture-audit
status: draft
process: simplecoins.phase5
source_refs:
  - https://drive.google.com/file/d/1sn-FXbU7nFDr6yc6YxPAe2V6U6mV2CZD/view
  - https://github.com/surumeneco/SimpleCoins/blob/develop/docs/verification/phase5-wcl-provider-contract.md
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5 WCL service capture structural audit (CAT Draft)

## Targets

| Ref | Aspect |
| --- | --- |
| simplecoins.phase5.wcl-capture | Independently exported, caller-supplied JSON describing the Paper service registrations |
| simplecoins.phase5.owner-settlement | No settlement decision or live-server attestation is made by this offline tool |

## Rules

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| WCL-AUDIT-01 | WCL/WWC/WWWが有効、JAR SHA-256と版が記録され、wwc/wwwの登録・owner・primary-thread・全件取得フラグが揃う | structurally_completeを返す。ただしPaper実機証明とは称さない | valid |
| WCL-AUDIT-02 | 必須プラグインまたはwwc/www登録が欠ける | incompleteを返し、ゼロ資産やFREEを推定しない | missing |
| WCL-AUDIT-03 | 重複provider ID、重複plugin名、不正な型・フラグ・capture時刻 | invalidを返す | malformed |
| WCL-AUDIT-04 | providerの登録所有者が期待するWWC/WWWと一致しない、無効状態、または固定WCLサービス型と一致しない | incompleteを返す | owner |
| WCL-AUDIT-05 | primary-thread/全登録取得を確認できない、またはJARハッシュが欠落する | incompleteを返す | provenance |
| WCL-AUDIT-06 | 解析対象が不正・大容量・読込不能 | invalidとして機械可読な終了コードを返す | cli |

## Process / PI

Input: JSON evidence independently exported by an authorized Paper-side probe. The script reads at most 1 MiB and checks the declared schema and provenance fields. It does not contact Paper, inspect running services, validate the truth of caller-supplied fields, resolve entity UUIDs, inspect inventories, or change server state. An attacker can fabricate structurally valid JSON. A separate trusted runtime capture and source-completeness check remain mandatory.

## TestModel / TCE

- Valid structural example, missing provider, missing plugin, disabled plugin, wrong owner, wrong service interface, duplicate provider.
- Unverified primary thread, incomplete registration enumeration, missing SHA-256, malformed flags.
- Timestamp ordering/timezone and CLI status/exit-code boundaries.

Status: **Draft**. This is an independent offline diagnostic helper, not a Confirmed CAT specification or an approved owner-settlement contract. Formal process-closure, TestModel generation, model-conformance, vectors, Queue/current-item, implementation-conformance and actual Paper runtime Gates are **not-run**.
