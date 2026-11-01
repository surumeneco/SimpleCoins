---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.owner.external-owner-ban-evidence
status: draft
process: simplecoins.owner
source_refs:
  - https://drive.google.com/file/d/1sn-FXbU7nFDr6yc6YxPAe2V6U6mV2CZD/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5: external ownership and BAN evidence completeness (CAT Draft)

Status: Draft, not Confirmed. Source: forGPT/XPlayServer/OriginalPlugins/SimpleCoins/02B_データ収集・決算処理.md, 05_1.0.0_実装計画.md.


## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.owner | 外部owner/commanderの権威証拠とUNKNOWNの保持 |
| simplecoins.ban | Paper・IP・外部pluginの必須BAN source集約 |
| simplecoins.blocklocker | 物理的に付属したsign・保存/liveテキストの所有権判定 |
| simplecoins.snapshot | 保存済みとloaded sourceの完全性・時点整合 |
| simplecoins.settlement | 未確定証拠からの決算公開禁止 |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| owner-evidence.owner.evidence | 外部所有権の検証済み状態をsettlement stagingへ渡すとき | UNKNOWNをFREE/PERSONALやゼロ資産と混同せず、BAN証拠の来歴を伴う検証済み状態だけ受け入れる | OWNER-EVIDENCE-01, OWNER-EVIDENCE-02, OWNER-EVIDENCE-03, OWNER-EVIDENCE-04, OWNER-EVIDENCE-05, OWNER-EVIDENCE-06 |
| owner-evidence.ban.evidence | 複数BAN sourceを一つのUUIDについて集約するとき | 必須source一覧を独立に宣言し、欠損・重複・UNKNOWNをCLEARへ昇格せずBANNEDの優先を維持する | BAN-EVIDENCE-01, BAN-EVIDENCE-02, BAN-EVIDENCE-03, BAN-EVIDENCE-04 |
| owner-evidence.companion.owner | WWC/WWW/WCL等のcompanion所有者・commanderを解決するとき | 検証済みcommander優先・owner fallbackを守り、未ロードや不明をabsenceと推定しない | COMPANION-OWNER-01, COMPANION-OWNER-02, COMPANION-OWNER-03, COMPANION-OWNER-04, COMPANION-OWNER-05, COMPANION-OWNER-06 |
| owner-evidence.blocklocker.agg | 物理的に付属したPRIVATE/MORE_USERS signから所有権を集約するとき | primaryとEveryone、double chestの相互関係を検証し、矛盾・未解決primaryをUNKNOWNとして扱う | BLOCKLOCKER-AGG-01, BLOCKLOCKER-AGG-02, BLOCKLOCKER-AGG-03, BLOCKLOCKER-AGG-04, BLOCKLOCKER-AGG-05, BLOCKLOCKER-AGG-06 |
| owner-evidence.paper.ban | Paper profile BANを外部BAN集合の一sourceとして取得するとき | 単独のCLEARを総合CLEARと誤認せず、不明なprofileはUNKNOWNとする | PAPER-BAN-01, PAPER-BAN-02, PAPER-BAN-03 |
| owner-evidence.paper.ip | Paper IP BANをUUIDへ結び付けるとき | UUIDとアドレスの権威ある完全対応を要求し、未知のアドレスをCLEARとみなさない | PAPER-IP-01, PAPER-IP-02, PAPER-IP-03 |
| owner-evidence.blocklocker.text | 保存済みBlockLocker signの可視テキスト/PDCを照合するとき | 4行・runtime alias・verified UUID・Everyoneの意味を独立に確認し、不正入力や未解決primaryを拒否する | BLOCKLOCKER-TEXT-01, BLOCKLOCKER-TEXT-02, BLOCKLOCKER-TEXT-03, BLOCKLOCKER-TEXT-04, BLOCKLOCKER-TEXT-05, BLOCKLOCKER-TEXT-06, BLOCKLOCKER-TEXT-07 |
| owner-evidence.blocklocker.mount | 保存済みsign paletteの壁・立看板のmountを判定するとき | 正当なvanilla向き・rotationのみ受け入れ、物理的付属関係を推測で補わない | BLOCKLOCKER-MOUNT-01, BLOCKLOCKER-MOUNT-02, BLOCKLOCKER-MOUNT-03 |
| owner-evidence.companion.www | loaded WWW wolfのpublic APIから所有者を読むとき | commander/owner Optionalを検証し、API不在や未ロードをUNKNOWNとする | COMPANION-WWW-01, COMPANION-WWW-02, COMPANION-WWW-03 |
| owner-evidence.blocklocker.live | loaded Paper Signから現在のBlockLocker証拠を読むとき | 物理位置・向き・前面4行を検証し、未ロードsignや完全snapshotを推定しない | BLOCKLOCKER-LIVE-01, BLOCKLOCKER-LIVE-02, BLOCKLOCKER-LIVE-03 |
| owner-evidence.companion.wwc | loaded WWC chickenのPDC follow_targetを読むとき | canonical UUIDのpositive-only証拠だけ返し、欠損をFREEとみなさない | COMPANION-WWC-01, COMPANION-WWC-02, COMPANION-WWC-03 |
| owner-evidence.blocklocker.snapshot | callerが保存済みsign候補集合を渡すとき | 完全・一貫したpalette/text証拠を要求し、未列挙候補や空集合をFREEとみなさない | BLOCKLOCKER-SNAPSHOT-01, BLOCKLOCKER-SNAPSHOT-02, BLOCKLOCKER-SNAPSHOT-03 |
| owner-evidence.ban.collect | 必須外部BAN pluginとPaper sourceの結果を収集するとき | 権威source inventoryと個別結果を照合し、欠損・不明がある場合CLEARを宣言しない | BAN-COLLECT-01, BAN-COLLECT-02, BAN-COLLECT-03 |

## Process / PI

An external owner adapter must distinguish three states: a verified personal owner UUID, verified free ownership, and **unknown** because the external source/API, snapshot or identity resolution is unavailable. Unknown is not equivalent to null UUID/free ownership or a zero-value inventory. Only a verified owner/free decision can be converted into an existing BlockEntity/Entity staging ownership record; unknown fails closed.

For a known personal owner, current BAN status may be established by multiple required sources (Paper profile bans, supported external adapters, and any resolvable IP-ban mapping). Each source reports BANNED, CLEAR or UNKNOWN for that UUID. Any verified BANNED suffices for dead-asset classification; all required sources must be CLEAR to classify as unbanned. An UNKNOWN without a definitive BANNED remains UNKNOWN and cannot be silently treated as CLEAR. Duplicate/missing source identities are invalid. This pure evidence layer does not claim that an external BAN plugin, IP-to-UUID mapping or WWC/WWW/WCL API has actually been connected.

## Test model / TCE

- OWNER-EVIDENCE-01 Red: UNKNOWN external ownership cannot become free or personal staging ownership.
- OWNER-EVIDENCE-02: verified FREE and PERSONAL convert to the corresponding staging ownership without inventing an owner.
- OWNER-EVIDENCE-03: missing or unknown BAN evidence blocks classification of a verified personal owner.
- BAN-EVIDENCE-01 Red: all CLEAR sources produce CLEAR; any BANNED produces BANNED; UNKNOWN without a BANNED remains UNKNOWN.
- BAN-EVIDENCE-02: empty, duplicate, malformed or null source evidence fails closed.

This is a contract boundary, not an authoritative world snapshot or completed external adapter. Formal CAT machine gates and Paper/WWC/WWW/BAN integration tests remain not-run; Draft is not Confirmed.

## Staging boundary integration (additional Draft vector)

- OWNER-EVIDENCE-04: an inventory containing a real denomination coin, when resolved as UNKNOWN external owner, causes `BlockEntitySettlementImport.stageChunk` to fail and `SettlementAccumulator.commit` to remain unavailable. A verified FREE decision may be staged as Free. This distinguishes missing ownership evidence from zero currency.

## Canonical BAN source identity (additional Draft vector)

- BAN-EVIDENCE-03: source IDs differing only by ASCII case or surrounding whitespace are one logical source, not two independent confirmations. Duplicate normalized IDs must fail closed.

## Explicit required-source completeness (additional Draft vector)

- BAN-EVIDENCE-04 Red: callers must pass the authoritative required-source ID set separately from observations. A missing required source yields UNKNOWN (or BANNED if another required source definitively bans), never CLEAR. Unknown/unexpected source IDs fail closed. An empty required-source declaration is invalid. The list-only convenience form must not remain the production decision API because it cannot prove source completeness.

## Required BAN source to owner-decision boundary

- OWNER-EVIDENCE-05: a verified personal UUID with Paper CLEAR but a missing required external BAN source remains UNKNOWN and cannot be converted into publishable personal ownership. The same UUID with all required sources CLEAR may be converted.

## Evidence provenance at the settlement boundary (Draft vector)

- OWNER-EVIDENCE-06: personal ownership conversion must not accept a caller-supplied `BanEvidence.Status.CLEAR` as proof. A status is valid only when wrapped in an immutable decision produced by `BanEvidence.evaluate(requiredSources, observations)`. Missing required sources produce UNKNOWN and block personal settlement. The legacy enum-only conversion path fails closed for PERSONAL; verified FREE remains independent of a player's BAN evidence. This is a domain contract, not proof that Paper or external BAN adapters are connected.

## Companion ownership precedence (Draft vectors)

- COMPANION-OWNER-01: a verified commander UUID takes precedence over the tamed owner's UUID, including when both are present and differ.
- COMPANION-OWNER-02: a verified absence of commander permits a known owner; verified absence of both permits FREE.
- COMPANION-OWNER-03: an unavailable/unloaded/unknown commander must not silently fall back to an owner or FREE. If commander absence is verified but owner state is unknown, the result is UNKNOWN.
- COMPANION-OWNER-04: null or internally inconsistent identity evidence is invalid. The resolver consumes already verified WWC/WWW/WCL evidence; it does not claim an external plugin API is connected or that unloaded entities were scanned.

## Verified protection-sign aggregation (Draft vectors)

- BLOCKLOCKER-AGG-01: only a physically attached PRIVATE sign can establish the primary ownership decision. Unattached signs and MORE_USERS-only evidence cannot assign ownership.
- BLOCKLOCKER-AGG-02: any physically attached MORE_USERS/PRIVATE sign with verified Everyone overrides a verified personal owner to FREE; an unattached Everyone sign has no effect.
- BLOCKLOCKER-AGG-03: a reciprocal double chest accepts a sign attached to either half and produces one ownership decision. Invalid/nonreciprocal chest geometry is an error, not two independently owned containers.
- BLOCKLOCKER-AGG-04: duplicate attached PRIVATE signs or inconsistent sign evidence fail closed. A verified legacy owner-unresolvable FREE decision is distinct from UNKNOWN due to unreadable/unavailable sign data.

This aggregation layer consumes caller-verified header, text/PDC reconciliation and physical block states. It is not a complete saved-sign parser, live BlockLocker API adapter or settled-world snapshot.

## Paper profile BAN source (Draft vectors)

- PAPER-BAN-01: a UUID-resolved Paper offline profile's current profile-ban flag maps to the mandatory `paper-profile` source as BANNED or CLEAR, never to a final cross-source BAN decision by itself.
- PAPER-BAN-02: a null/failed/unavailable profile lookup maps to UNKNOWN, not CLEAR. Invalid null UUID input is rejected.
- PAPER-BAN-03: a verified Paper CLEAR with a missing required external or IP BAN source still produces UNKNOWN at the combined settlement boundary. This adapter must not claim to detect IP bans or external plugins.

The adapter may call Paper APIs only in an appropriate server execution context. Real-server Paper 26.2 API behavior remains an integration gate.

- BLOCKLOCKER-AGG-05: a real saved coin inventory resolved through an attached Everyone protection stages as FREE; missing primary sign yields UNKNOWN and aborts staging/commit instead of silently counting zero. This checks the existing settlement staging boundary without claiming that raw sign text/PDC has been authoritatively decoded.

## Paper IP BAN evidence with verified UUID/address mapping (Draft vectors)

- PAPER-IP-01: when the caller supplies a complete, authoritative set of addresses associated with the target UUID, the Paper IP ban list yields BANNED if any address is banned, otherwise CLEAR only when every address lookup succeeds and is clear.
- PAPER-IP-02: absent/incomplete address mapping, no known addresses, null/failed address lookup or unavailable IP ban list yields UNKNOWN unless a known associated address is definitively banned (BANNED dominates UNKNOWN).
- PAPER-IP-03: this adapter does not infer UUID/address associations, query historical addresses or treat the current online IP as a complete identity history. A Paper profile-ban CLEAR and IP-ban UNKNOWN cannot be published as globally CLEAR.

## Verified visible-sign reconciliation boundary (Draft vectors)

- BLOCKLOCKER-TEXT-01: a physically verified sign with four caller-decoded visible front lines and runtime-verified localized aliases can be converted into a verified sign only when its PRIVATE primary identity has been independently resolved. Missing identity evidence remains UNKNOWN, even when an Everyone profile is displayed.
- BLOCKLOCKER-TEXT-02: a saved PDC UUID is only a hint. If a present PDC first-owner UUID disagrees with an independently verified displayed-name identity, return UNKNOWN rather than silently using either owner. Text-only legacy signs may resolve when an authoritative identity source verifies the displayed owner.
- BLOCKLOCKER-TEXT-03: an attached MORE_USERS sign with a verified visible Everyone tag can override an independently verified PRIVATE owner, but PDC Everyone markers without a visible Everyone tag cannot do so. Header mismatches must not promote a stale PDC owner.
- BLOCKLOCKER-TEXT-04: malformed/incomplete decoded lines, missing runtime aliases, null evidence or failed identity lookup must fail closed. This boundary does not parse raw JSON, authenticate physical block states or connect the BlockLocker API; those remain separate integration gates.
- BLOCKLOCKER-AGG-06: an attached PRIVATE sign with unresolved primary identity must remain UNKNOWN even if a verified Everyone tag is also present; a public-ownership decision requires a resolved primary sign.

## Saved sign block-state mount decoding (Draft vectors)

- BLOCKLOCKER-MOUNT-01: a verified vanilla wall-sign palette state with north/east/south/west facing maps to the corresponding physical support direction, without trusting PDC or displayed text as a source of geometry.
- BLOCKLOCKER-MOUNT-02: a verified standing-sign palette state with a valid 0–15 rotation maps to STANDING support beneath the sign; waterlogged properties do not alter the support position.
- BLOCKLOCKER-MOUNT-03: missing/invalid facing or rotation, unsupported wood/block kinds, hanging-sign states, and contradictory wall/standing properties fail closed. The caller must still establish the sign block position and coherent world snapshot; this is not a region-palette reader or live BlockLocker adapter.

## Saved sign evidence handoff (Draft vectors)

- BLOCKLOCKER-TEXT-05: a coordinate-keyed saved sign evidence pair must pass all four raw front components through a caller-supplied verified text-component decoder before applying visible-sign identity rules. A text-only legacy sign is eligible for authoritative identity lookup; PDC-only evidence is not sufficient to classify ownership.
- BLOCKLOCKER-TEXT-06: missing raw text, null decoder output or a decoder error blocks owner classification. An evidence pair containing neither stream is not a protection sign. This handoff does not itself implement a JSON component decoder, verify the sign's palette index or establish a world snapshot.
- BLOCKLOCKER-TEXT-07 (Red): a runtime-localized visible Everyone tag with legacy color/reset formatting and outer whitespace must be normalized consistently with BlockLocker header matching; valid visible `[Everyone]` is not silently treated as a different personal profile. Nonmatching tags still do not grant public ownership.

## Paper tameable owner evidence (Draft vectors)

- COMPANION-OWNER-05: a currently loaded, verified untamed Paper Tameable supplies ABSENT owner evidence; a tamed entity with a non-null AnimalTamer UUID supplies KNOWN. Null entity, failed API call, missing owner or missing UUID supplies UNKNOWN.
- COMPANION-OWNER-06: Paper Tameable owner evidence is not WWC/WWW/WCL commander evidence. Without a separately verified commander absence or commander UUID, a known Paper owner cannot be published as the companion's final owner.

## Optional WWW loaded-individual contract (Draft vectors)

- COMPANION-WWW-01: an enabled WhatAWonderfulWolf plugin's public `loadedIndividuals().find(wolfUuid)` returns a loaded individual whose `commanderId()` and `ownerId()` are Optional UUIDs. A verified commander takes precedence; verified absence of commander permits owner fallback.
- COMPANION-WWW-02: missing/disabled plugin, unloaded wolf (empty find), invalid reflection contract, malformed Optional or lookup exception yields UNKNOWN, not FREE or an inferred commander absence.
- COMPANION-WWW-03: the adapter uses the current public WWW API without a mandatory build-time dependency, and is read-only on the Paper server thread. Saved/unloaded WWW identity, WWC/WCL APIs and full settlement integration remain separate acceptance gates.

## Live Paper BlockLocker sign evidence (Draft vectors)

- BLOCKLOCKER-LIVE-01: a currently loaded Paper Sign exposes a verified physical block position, wall-sign facing or standing-sign support, and the four front-side Adventure text components. These components are converted to plain visible text and passed to the independently verified identity/Everyone resolver.
- BLOCKLOCKER-LIVE-02: unsupported hanging-sign block data, null/malformed sign state, unavailable front text or invalid orientation fails closed. The caller must still prove sign attachment and use the configured BlockLocker localization aliases and authoritative name-to-UUID lookup.
- BLOCKLOCKER-LIVE-03: the adapter reads current Paper state only and does not load missing chunks, inspect offline signs, infer absent BlockLocker protection, or claim an atomic world snapshot. Real Paper/BlockLocker 1.14.1 server behavior remains an integration gate.

## Optional WWC loaded chicken follow-target evidence (Draft vectors)

- COMPANION-WWC-01: a loaded chicken carrying the current WhatAWonderfulChicken marker and a canonical UUID in its `follow_target` PDC may supply positive commander/follow-target identity evidence. An absent or malformed follow target is UNKNOWN, never proof of FREE ownership.
- COMPANION-WWC-02: unavailable/disabled WWC plugin, unsupported data version or unreadable PDC yields UNKNOWN. The adapter does not infer a vanilla tame owner for chickens, enumerate unloaded entities or claim that every WWC chicken has a commander.
- COMPANION-WWC-03: this is a current-version PDC evidence adapter, not an external plugin's stable API guarantee. A real WWC/Paper integration test and full settlement owner policy remain required.

## Coherent saved-sign candidate assembly (Draft vectors)

- BLOCKLOCKER-SNAPSHOT-01: a caller-supplied complete, coherent set of coordinate-keyed saved sign evidence and matching verified palette states can be decoded into physical mount/position and authoritative visible ownership evidence before single-container aggregation.
- BLOCKLOCKER-SNAPSHOT-02: a verified reciprocal double chest aggregates signs on either half once, including visible Everyone overrides; duplicate/malformed/missing palette state or noncanonical coordinate keys fail closed.
- BLOCKLOCKER-SNAPSHOT-03: the assembly does not enumerate neighboring chunks, prove candidate-set completeness, freeze the world or supply a name-to-UUID authority. The caller must establish all of these before using its result for settlement; an empty candidate set is UNKNOWN rather than an inferred FREE.

## Complete BAN source collection boundary (Draft vectors)

- BAN-COLLECT-01: a declared complete inventory of required external BAN plugins, Paper profile bans and Paper IP bans is combined for one owner UUID. Only all verified CLEAR sources can yield CLEAR; any verified BANNED dominates, and missing/unavailable evidence yields UNKNOWN.
- BAN-COLLECT-02: an unverified external-plugin inventory cannot establish CLEAR even if every currently supplied source is clear. Each required external plugin must have an explicit lookup result or UNKNOWN; duplicate normalized plugin IDs and collisions with built-in source names fail closed.
- BAN-COLLECT-03: the caller must independently prove UUID-to-address completeness and authoritative external-plugin discovery. The collector does not infer IP history, scan plugins, or establish a world snapshot.
