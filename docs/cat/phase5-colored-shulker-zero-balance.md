# Phase 5: Zero-balance ownership of colored shulker boxes (CAT Draft)

Status: Draft, not Confirmed. Requirements: SimpleCoins 02A (protected containers retain eligible zero-balance registered owners) and 02B/05 (all block storage in full settlement).

## Process
When a saved colored shulker box has no `Items` tag, it is still a valid physical container. Resolve its protection ownership once, stage a zero-value physical container marker, and retain an eligible registered owner without inventing currency. A colored shulker box with currency continues to stage both the marker and each valued slot. Other unrelated block entities must not become implicit protected containers.

## Test model
- Red: `minecraft:red_shulker_box` with no `Items`, protected by a non-banned player with playerdata, must preserve that player with balance zero.
- Existing green: an empty ordinary chest retains the owner; free/everyone ownership does not register the owner.
- Verify duplicate physical container markers still fail closed.

Formal CAT machine gates: not-run; Chat plugin/Skill unavailable. This Draft is not a Confirmed artifact.
