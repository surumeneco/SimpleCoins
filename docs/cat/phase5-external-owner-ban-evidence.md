# Phase 5: external ownership and BAN evidence completeness (CAT Draft)

Status: Draft, not Confirmed. Source: forGPT/XPlayServer/OriginalPlugins/SimpleCoins/02B_データ収集・決算処理.md, 05_1.0.0_実装計画.md.

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

## Paper tameable owner evidence (Draft vectors)

- COMPANION-OWNER-05: a currently loaded, verified untamed Paper Tameable supplies ABSENT owner evidence; a tamed entity with a non-null AnimalTamer UUID supplies KNOWN. Null entity, failed API call, missing owner or missing UUID supplies UNKNOWN.
- COMPANION-OWNER-06: Paper Tameable owner evidence is not WWC/WWW/WCL commander evidence. Without a separately verified commander absence or commander UUID, a known Paper owner cannot be published as the companion's final owner.

## Optional WWW loaded-individual contract (Draft vectors)

- COMPANION-WWW-01: an enabled WhatAWonderfulWolf plugin's public `loadedIndividuals().find(wolfUuid)` returns a loaded individual whose `commanderId()` and `ownerId()` are Optional UUIDs. A verified commander takes precedence; verified absence of commander permits owner fallback.
- COMPANION-WWW-02: missing/disabled plugin, unloaded wolf (empty find), invalid reflection contract, malformed Optional or lookup exception yields UNKNOWN, not FREE or an inferred commander absence.
- COMPANION-WWW-03: the adapter uses the current public WWW API without a mandatory build-time dependency, and is read-only on the Paper server thread. Saved/unloaded WWW identity, WWC/WCL APIs and full settlement integration remain separate acceptance gates.
