# Phase 5: saved BlockLocker sign-state completeness (CAT Draft)

Status: **Draft**, not Confirmed. Parent: TASK-20261031-SIMPLECOINS-100; leaf: TASK-20261101-004.

## Process / PI

For the supported modern vanilla standing/wall sign block states, the saved palette `Properties` must provide the complete orientation and waterlogged state. A partial property map can be fabricated by a broken source adapter and must not be accepted as a verified physical mount.

Accept exactly the applicable orientation (`rotation` 0–15 or cardinal `facing`) plus `waterlogged` (`true`/`false`). Reject missing `waterlogged`, unexpected properties, malformed values and hanging sign variants. This verifies palette completeness only; it does not authenticate the sign, owner, BlockLocker localization or coherent neighborhood.

## TCE / acceptance vectors

- SC-SIGN-STATE-01 (Red): standing sign with valid rotation but missing `waterlogged` fails closed.
- SC-SIGN-STATE-02 (Red): wall sign with valid facing but missing `waterlogged` fails closed.
- SC-SIGN-STATE-03 (Regression): full modern standing/wall states remain accepted.
- SC-SIGN-STATE-04 (Regression): invalid waterlogged values, unexpected keys and unsupported hanging signs remain rejected.

## Gate status

Formal CAT machine Gates **not-run**. This Draft does not prove BlockLocker 1.14.1 runtime sign matching, complete neighborhood, ownership or live Paper integration.
