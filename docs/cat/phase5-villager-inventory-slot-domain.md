# Phase 5: Villager saved-inventory physical slot bound (CAT Draft)

Status: **Draft, not Confirmed**. Sources: `forGPT/XPlayServer/OriginalPlugins/SimpleCoins/02B_データ収集・決算処理.md` (full inventory accounting, physical slot uniqueness) and `05_1.0.0_実装計画.md` (Phase 5, G0, fail-closed).

## Process / PI

- A vanilla villager has eight physical inventory slots. Its saved `Inventory` NBT list is positional rather than carrying a `Slot` tag.
- PI-1: positions 0 through 7 are valid. A ninth saved entry cannot be counted as an additional physical slot.
- PI-2: an oversized villager list raises `IOException` before settlement can publish a result. No partial value is accepted as a successful source.
- PI-3: normal villager inventory entries remain countable and require authoritative ownership separately.

## Test conditions / TCE

| ID | Input | Expected |
| --- | --- | --- |
| ESI-01 | villager with nine coin entries in `Inventory` | IOException |
| ESI-02 | villager with eight coin entries | eight unique positions, each counted once |

Formal CAT machine Gates: **not-run**. This Draft is not Confirmed and does not establish full Entity/WWC/WWW or live-world snapshot coverage.
