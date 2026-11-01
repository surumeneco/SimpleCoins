# Phase 6: settlement asset world identity (CAT Draft)

Status: **Draft** (not Confirmed). Parent TASK-20261031-SIMPLECOINS-100; leaf TASK-20261101-005.

## Process / PI

A staged settlement asset has either an explicit nonblank world identity or `null` for world-independent playerdata. A blank or whitespace-only world name is not a valid source of world-scoped assets. Accepting it would silently apply WorldScope's default rule instead of a known world and could misclassify currency from an excluded world. Reject it at Asset construction before any accumulator can publish results.

Keep `null` valid because playerdata/Ender Chest are world-independent, and preserve explicit world names including custom Multiverse names. This validation does not establish a consistent world snapshot, recover a journal, or authorize settlement publication.

## TCE

- SC-WORLD-01 (Red): Asset with `world=""` is rejected.
- SC-WORLD-02 (Red): Asset with `world="   "` is rejected.
- SC-WORLD-03 (Regression): Asset with `world=null` remains valid for world-independent playerdata.
- SC-WORLD-04 (Regression): a named world remains eligible for WorldScope inclusion/exclusion logic.

Formal CAT machine Gates not-run; Draft not Confirmed. No merge, release, or deployment.
