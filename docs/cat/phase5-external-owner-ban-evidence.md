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
