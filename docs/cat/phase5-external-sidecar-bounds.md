# Phase 5: Bounded external Anvil sidecar read (CAT Draft)

Status: Draft, not Confirmed. Sources: SimpleCoins 02B and 05_1.0.0_実装計画.md, G0.

## Process
External Anvil chunks (`.mcc`) are untrusted snapshot inputs. A prior `Files.size` check is not a byte-budget guarantee if the file changes before or during reading. Read through a no-follow stream with an explicit byte limit; reject input that grows beyond the budget or whose actual byte count differs from the size observed before opening. Never publish partial chunk callbacks on an unreadable sidecar. A stable, correctly sized sidecar remains readable.

## Test model
- A bounded sidecar stream with N bytes at the limit is accepted.
- A stream with N+1 bytes is rejected with IOException before allocating an unbounded array.
- Existing external Anvil sidecar decoding and compressed/inflated limits remain green.
- This does not claim to establish a consistent live-world snapshot; external snapshot coordination remains required.

Formal CAT machine gates: not-run; dedicated Chat plugin/Skill unavailable. Draft is not Confirmed.
