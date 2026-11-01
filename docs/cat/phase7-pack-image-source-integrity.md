# Phase 7: generated Java pack image source integrity (CAT Draft)

Status: **Draft**, not Confirmed. Parent: TASK-20261031-SIMPLECOINS-100; leaf: TASK-20261101-006.

## Process / PI

The operator-controlled `images/` directory and its PNG files are read into the generated Java resource-pack ZIP. Neither a symbolic-link directory nor a symbolic-link PNG may redirect the builder to an unrelated file or filesystem location. Generated default images must not overwrite an existing symbolic-link destination.

Validate all existing ancestors before and after directory creation. Resolve a single basename-only PNG path inside the validated image directory and reject symbolic or non-regular image inputs. For a generated default, require that the target is absent (including a dangling symlink), and write only into the validated real directory. An unrelated image must not be included in the archive solely because it is present in the folder.

## TCE / acceptance vectors

- SC-PACK-IMAGE-01 (Red): an `images/` symlink to another directory is rejected before any archive or default image is generated.
- SC-PACK-IMAGE-02 (Red): a selected `.png` symlink to an external file fails closed, not copied into the archive.
- SC-PACK-IMAGE-03 (Red): a default output filename that already exists as a symbolic link cannot be overwritten.
- SC-PACK-IMAGE-04 (Regression): a real PNG inside a real image directory is accepted; a nonexistent safe default path is allowed for generation.

## Gate and live integration

CAT Draft only; formal machine gates and Paper/Geyser/JE/BE client pack-delivery tests **not-run**. The helper validates local source paths but does not authenticate public HTTP hosting or prove the Java and Bedrock packs are published as one transaction.
