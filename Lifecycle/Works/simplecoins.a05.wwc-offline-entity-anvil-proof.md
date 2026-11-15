---
cat_version: '0.2'
kind: work
id: simplecoins.a05.wwc-offline-entity-anvil-proof
status: draft
process: simplecoins.external-storage-provider-integration
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
decision_ref: ''
refs: []
entry: issue
mode: shadow
flow: issue-work
work_kind: verification
parent: ''
depends_on: []
issue_ref: https://github.com/surumeneco/SimpleCoins/issues/4
project_entry_ref: https://drive.google.com/file/d/1oyuQJfvjho_RsWIrlPI_XCmLlc4Df0dl/view
spec_status: unknown
---

# A05 — Chunkをロードせず保存Entity MCAを直接読む検証

## Evidence

| URI |
| --- |
| https://github.com/surumeneco/SimpleCoins/pull/441 |
| https://github.com/surumeneco/SimpleCoins/issues/423 |
| https://github.com/surumeneco/SimpleCoins/blob/develop/src/main/java/co/surumene/simplecoins/domain/AnvilRegionReader.java |

## Scope

| Process IDs | Changed IDs | Preserved IDs |
| --- | --- | --- |
| simplecoins.external-storage-provider-integration | simplecoins.a05.wwc-offline-entity-anvil-proof | simplecoins.a05.wwc-paper-entity-save-reload;simplecoins.settlement.admission-barrier |

## Technologies

| Technology |
| --- |
| Java 25; official Paper 26.2 build 134; pinned WWC WCL distributed JAR; AnvilRegionReader and NbtReader |

## Repositories

`Allowed paths` uses `;` between path patterns.

| Name | Path | Base ref | Branch pattern | Allowed paths | Production paths | Test paths |
| --- | --- | --- | --- | --- | --- | --- |
| SimpleCoins | ../.. | develop | feature/* | Lifecycle/Works/simplecoins.a05.wwc-offline-entity-anvil-proof.md;tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/OfflineEntityRegionProbe.java;.github/workflows/paper-wcl-services-probe.yml | — | tests/paper-wcl-services-probe/src/main/java/co/surumene/simplecoins/a05probe/OfflineEntityRegionProbe.java;.github/workflows/paper-wcl-services-probe.yml |

## Checks

`Command` uses shell-like quoting only to express argv; execution never uses a shell. List cells use `;`.

| ID | Stage | Runner | CWD | Command | Test IDs | Inputs | Outputs | Timeout |
| --- | --- | --- | --- | --- | --- | --- | --- | ---: |

## Gates

| Gate | Status | Evidence |
| --- | --- | --- |
| process-closure | not-run | — |
| model-conformance | not-run | — |
| implementation-conformance | not-run | — |
| semantic-review | not-run | — |
| test-oracle-review | not-run | — |
| red-review | not-run | — |
| pr-review | not-run | — |
| ci | passed | https://github.com/surumeneco/SimpleCoins/actions/runs/43736143237 (actual Paper save, standalone offline MCA scan and restart; CAT formal Gates not-run) |
| merge | not-run | — |
| deployment | not-run | — |
| real-use | not-run | — |

## Bounded positive and rejection cases

- Use the exact server plugin JARs and official Paper 26.2 from the prior A05 verified Work. Paper process A produces a **real** Chicken Entity with WWC-readable typed PDC containing a ShulkerBox and genuine SimpleCoins 5000-unit coin ×2, and saves it in a legitimate Anvil `entities/r.0.0.mca` file.
- Once Paper process A has actually exited and **before** Paper process B loads the chunk, invoke a standalone JVM tool depending only on the already built SimpleCoins production JAR's **existing read-only** `AnvilRegionReader.scan` and Java NBT reader. No Paper bootstrap or chunk load in this inspection JVM.
- Scan only the known test region under strict compressed/inflated byte limits, select the fixture by exact unique scoreboard tag and UUID recorded by the first process, and require it to appear exactly once in the correct physical chunk (4,4). Reject missing/duplicated identities, incorrect NBT types, coordinates, absent typed PDC and mismatched authority bytes.
- Read the exact saved `BukkitValues` source keys, require `whatawonderfulchicken:type`, `data_version`, `follow_target` and serialized `equipment_shulker_box` byte[] to equal the authentic fixture established before saving. A second copy is not a separate monetary source.
- Only after the standalone offline inspection succeeds should Paper process B reload the same entity and validate its physical currency via the shipped WWC reader, as already established by PR #441.
- This proves a narrowly bounded physical `entities` MCA containing one WWC-type PDC, not arbitrary entity source discovery, item conversion to API `PersistentDataContainer`, full WWC/WWW storage coverage, cross-world owner classification or a frozen-world accounting snapshot. A test-generated WWC-type PDC is **not** a naturally generated WWC individual.
- Admission remains closed; no runtime product source, actual user data or VPS changes. Any NBT format mismatch is a test failure requiring inspection and technical correction, never silent source omission.

## Measured source fact

- [Official Paper 26.2 build 134 / CI #43736143237](https://github.com/surumeneco/SimpleCoins/actions/runs/43736143237) successfully executed Work contract validation, JDK25 build/tests, the packaged WWC/WWW regressions and three separate reading contexts.
- **Paper process A** saved the tagged Chicken carrying a genuine SimpleCoins 5000-unit coin ×2 in a serialized ShulkerBox, recording its exact Entity UUID and original serialized ItemStack bytes. The workflow verified the Entity Anvil source file exists.
- With Paper fully stopped and before process B, a **standalone non-Bukkit JVM** loaded `entities/r.0.0.mca` using production `AnvilRegionReader.scan` with compressed/inflated budget bounds. The callback found exactly one tagged Entity in Chunk 4,4 with the same UUID, `minecraft:chicken` type, `BukkitValues` keys `whatawonderfulchicken:type`/`data_version`/`follow_target` and **243 identical serialized ShulkerBox ItemStack bytes**. Markers: `PAPER_A05_OFFLINE_MCA_WWC_MONEY_PHYSICAL_SOURCE_PASSED` and `PAPER_A05_OFFLINE_MCA_SCAN_NO_CHUNK_LOAD_PASSED`.
- **Paper process B** independently loaded the same world and verified the original identity and nested genuine currency via both shipped WWC read APIs.
- Only one known test chunk/Entity was checked. This proves a valid saved Entity NBT input can be read **without an active Paper server or chunk load**, but does **not** prove complete Entity source discovery, item denomination parity in an offline-independent decoder, WCL `readStored` construction from NBT outside Paper, owner/BAN classification, simultaneous saved/LIVE generation or monetary admission.
- The pinned CAT structural validator reported `contract_valid:true` and `semantic_approval_verified:false`. All formal CAT conformance/semantic Gates remain `not-run`.

## Authority

Verification-only `issue-work` Draft, no CAT Process/PI/TCE Confirmed or formal conformance Gates.
