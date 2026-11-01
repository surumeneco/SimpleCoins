# Phase 5: Physical block-inventory slot bounds (CAT Draft)

Status: **Draft, not Confirmed**. Sources: `forGPT/XPlayServer/OriginalPlugins/SimpleCoins/02B_データ収集・決算処理.md` (full physical-source accounting and unique slots), `05_1.0.0_実装計画.md` (G0, Phase 5, fail-closed source validation).

## Process / PI

- A saved block entity with a vanilla physical inventory exposes slots defined by its block type. A fabricated slot outside that physical inventory is not an asset location.
- PI-1: a single chest (including either half of a double chest) has physical slots 0–26; a hopper has slots 0–4.
- PI-2: an invalid physical slot must fail the source with `IOException` before any settlement result can be published. Do not silently ignore or count it as legitimate currency.
- PI-3: valid boundary slots continue to contribute their value once. This decoder alone does not prove snapshot consistency or BlockLocker ownership.

## Test conditions / TCE

| ID | Saved block entity | Expected |
| --- | --- | --- |
| BSI-01 | chest with currency in slot 27 | IOException |
| BSI-02 | chest with currency in slot 26 | accepted once |
| BSI-03 | hopper with currency in slot 5 | IOException |
| BSI-04 | hopper with currency in slot 4 | accepted once |

## Verification boundary

CAT machine process-closure, TestModel, model-conformance, vectors, Queue, current-item and implementation-conformance: **not-run** pending the authorized scripts/Confirmed Artifact. This Draft is not a machine Gate result or a release claim. A feature-branch CI Red followed by Green is required.
