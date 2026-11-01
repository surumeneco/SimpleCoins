# Phase 7: offline typed coin grant reservation (CAT Draft)

Status: **Draft** (not Confirmed). Leaf: TASK-20261101-006.

## Process / PI

An administrator with `simplecoins.admin` may issue `/simplecoins give <known-player> <denomination> <count>` even when that player is offline. The command must resolve exactly one previously known player by canonical UUID or case-insensitive name and durably reserve the selected denomination/count using the existing grant journal. It must not create physical items or schedule an offline player. Online recipients continue to be scheduled after reservation; selectors remain online-only and an empty selector is rejected. Wallet creation remains online-only.

A name matching multiple distinct known UUIDs or an unknown player fails before any reservation. The recipient resolution function is deterministic and does not select the first ambiguous match.

## TCE / acceptance vectors

- SC-ADMIN-OFFLINE-01 (Red): a known offline recipient can be selected uniquely for a typed denomination grant.
- SC-ADMIN-OFFLINE-02 (Red): unknown or ambiguous offline recipients are rejected.
- SC-ADMIN-OFFLINE-03 (Regression): case-insensitive known names and canonical UUIDs resolve to the same recipient.
- SC-ADMIN-OFFLINE-04 (Regression): offline wallet creation and empty online selectors remain rejected.
- SC-ADMIN-OFFLINE-05 (Integration): command reserves typed coins before any optional online schedule; full Paper integration remains not-run until a test harness is available.

## Gate status

Formal CAT machine gates: **not-run**. Draft is not Confirmed. This does not authorize merge, deployment, or real-server issuance.
