# Phase 5: saved campfire inventory slot integrity (CAT Draft)

Status: **Draft** (not Confirmed). Parent TASK-20261031-SIMPLECOINS-100; leaf TASK-20261101-003.

## Process / PI

A saved vanilla campfire or soul campfire has four physical cooking item positions, indexed 0 through 3. The read-only settlement scanner must reject an `Items` entry at slot 4 or above rather than treating a fabricated fifth slot as additional saved currency. Slots 0 through 3 remain countable. Existing duplicate slot, item-value and chunk-coordinate checks remain in effect. The scanner does not publish settlement results.

## TCE / acceptance vectors

- SC-CAMPFIRE-01 (Red): `minecraft:campfire` slot 4 with a valid currency stack is rejected with IOException.
- SC-CAMPFIRE-02 (Red): `minecraft:soul_campfire` slot 4 is rejected.
- SC-CAMPFIRE-03 (Regression): both campfire variants at slot 3 are countable.
- SC-CAMPFIRE-04 (Regression): empty campfire inventory remains valid.

Formal CAT machine gates not-run; Draft is not Confirmed. No merge, deploy, settlement commit, or publication.
