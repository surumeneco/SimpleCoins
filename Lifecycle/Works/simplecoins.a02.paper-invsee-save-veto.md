---
cat_version: '0.2'
kind: work
id: simplecoins.a02.paper-invsee-save-veto
status: draft
process: simplecoins.grants.paper-save-commit
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/478
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

# A02 — real Paper InvSee++ cooperative writer veto, not a global writer lock

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/478 |
| https://github.com/Jannyboy11/InvSee-plus-plus/blob/722def99e663561a1e8e4cbc865003af003cdb0e/InvSee++_Common/src/main/java/com/janboerman/invsee/spigot/api/event/SpectatorInventorySaveEvent.java |
| https://github.com/Jannyboy11/InvSee-plus-plus/blob/722def99e663561a1e8e4cbc865003af003cdb0e/InvSee++_Platforms/Impl_Paper_26_2/src/main/java/com/janboerman/invsee/paper/impl_26_2/InvseeImpl.java |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.grants.paper-save-commit | simplecoins.a02.paper-invsee-save-veto | simplecoins.a02.paper-invsee-actual-offline-writer;simplecoins.a02.verify-to-ack-writer-gap |

## Technologies

| Technology |
| --- |
| Official Paper 26.2 build 134; actual distributed InvSee++ 0.31.22; Java 25; cancellable SpectatorInventorySaveEvent; real playerdata and grant readback; independent two-JVM isolated CI |

## Repositories

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a02.paper-invsee-save-veto.md;tests/paper-invsee-writer-probe/src/main/java/co/surumene/simplecoins/a02invsee/InvseeWriterProbe.java;.github/workflows/paper-invsee-writer-gap-probe.yml | — | tests/paper-invsee-writer-probe/src/main/java/co/surumene/simplecoins/a02invsee/InvseeWriterProbe.java;.github/workflows/paper-invsee-writer-gap-probe.yml |

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
| ci | not-run | — |
| merge | not-run | — |
| deployment | not-run | — |
| real-use | not-run | — |

## Independent verification strategy

**Unmitigated control** (existing #478) runs an actual offline InvSee++ API save *after* a previously valid SimpleCoins proof, removes the actual saved 5000C while retaining receipt, and demonstrates stale low-level journal ACK. **Mitigation candidate** registers a separate `SpectatorInventorySaveEvent` listener under exactly the same isolated Paper/InvSee++ versions, cancels only the selected synthetic offline UUID **before invoking the API save**, and verifies `SaveResponse.isSaved() == false`, no saved item mutation, the original PDC receipt + unrelated storage intact and fresh production SavedGrantDeliveryFileReadback still valid. The grant must remain pending because this is a veto experiment, NOT product issuance. Run both cases in separate fresh Paper JVM/world folders and verify independent marker sequences.

InvSee++'s published `SpectatorInventorySaveEvent` implements Bukkit `Cancellable`; its Paper 26.2 implementation calls the event before it constructs a fake player/save task and returns `SaveResponse.notSaved` when cancelled. Real runtime must confirm this, not just trust the source. Source upstream HEAD is independent from the deployed JAR, which is pinned by SHA256 from XPlayServer at 90aa2d0.

**Limits**: This is a *cooperative single-plugin veto* available only while the listener is active and InvSee++ uses its documented API. It does **not** cover administrative filesystem editing, Paper's own saved data async flush, all other plugins, non-cooperating plugins, missed plugin lifecycle, in-flight save already past its event, or global accounting-epoch atomicity. The G3 coordinator and event listener are NOT installed in product. It is not permission to issue, acknowledge, publish, admit or declare CAT Confirmed/Gates. The test demonstrates one implementable component of the required full writer boundary; the remaining writer set still requires closure and proof.

## Acceptance

- Existing unmitigated real InvSee++ writer-gap test remains successful on one isolated JVM.
- A second isolated Paper server with the same pinned distribution InvSee++.jar produces a cancelling event for the same offline UUID, returns notSaved and preserves the exact original saved proof and pending ledger.
- Fail-closed under missing/inactive InvSee++ or unexpected save success; no product code modification or standalone server data leakage.
- CI success and develop readback are verification evidence, not formal CAT Model Gates.
