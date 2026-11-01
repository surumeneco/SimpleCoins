# Phase 5: cross-layout saved chunk identity (CAT Draft)

Status: Draft, not Confirmed. Sources: forGPT/XPlayServer/OriginalPlugins/SimpleCoins/02B_データ収集・決算処理.md; Minecraft 26.1 storage layout migration; WorldStorageDiscovery and WorldSnapshotTraversal.

## Process / PI

A stable snapshot may contain both legacy and modern dimension directory layouts after migration. Different physical Anvil files are not necessarily distinct economic sources: `world/region/r.0.0.mca` and `world/dimensions/minecraft/overworld/region/r.0.0.mca` can expose the same logical chunk. The same applies to `DIM-1` vs `dimensions/minecraft/the_nether`, and `DIM1` vs `dimensions/minecraft/the_end`. During read-only traversal, reject a second chunk with the same logical world, canonical dimension, source kind (terrain vs entity), and x/z chunk coordinates, even if its file path and inode differ.

Do not conflate different worlds, dimensions or terrain/entity kinds: each is a separate physical source. This check complements, but does not replace, stable snapshot acquisition, source completeness and memory/disk reconciliation. A failed traversal cannot publish settlement totals.

## Test model / TCE

- SC-CHUNK-01 Red: legacy overworld and modern overworld files with the same chunk coordinate and same logical world/kind fail closed.
- SC-CHUNK-02: legacy DIM-1 and modern minecraft:the_nether duplicate coordinates fail closed.
- SC-CHUNK-03: overworld and nether at the same coordinate remain separate; terrain and Entity chunks at the same coordinate remain separate.
- SC-CHUNK-04: different logical world names at the same coordinate remain separate.

Formal CAT machine gates not-run; Draft is not Confirmed. No release/settlement publication is authorized.

## Additional cross-kind and custom-dimension vectors

- SC-CHUNK-05: `DIM1/entities` and `dimensions/minecraft/the_end/entities` for the same world and chunk coordinates are one logical Entity chunk, so the second must fail.
- SC-CHUNK-06: distinct custom namespaces such as `dimensions/alpha/sky` and `dimensions/beta/sky` are separate dimensions even at the same x/z.
