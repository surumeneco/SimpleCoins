# Phase 6: dangling journal head is not an empty journal (CAT Draft)

Status: **Draft**, not Confirmed. Leaf: `TASK-20261101-005`.

## Process / PI

At journal startup, a dangling symbolic link at `head` is an existing but invalid durable record. Following symbolic links while testing for existence can misclassify that entry as absent and replace it with a fresh zero head, masking corrupted or redirected journal state.

Classify the `head` path with `NOFOLLOW_LINKS`. If the directory entry exists, the bounded `DurableFile.read` contract must validate it and reject non-regular/symbolic content. Only a genuinely absent head in a journal with no grant records may initialize sequence zero.

## TCE / acceptance vectors

- SC-JOURNAL-HEAD-01: dangling symlink at `head` and no `.grant` files -> constructor throws IOException, link remains unchanged.
- SC-JOURNAL-HEAD-02: absent head and empty journal -> sequence zero is initialized normally.
- SC-JOURNAL-HEAD-03: missing head with existing grant records -> fail closed.

This does not establish a complete historical journal audit or a Paper live recovery Gate.
