# Phase 5: saved sign display-text evidence (CAT Draft)

Status: Draft, not Confirmed. Sources: forGPT/XPlayServer/OriginalPlugins/SimpleCoins/02B_データ収集・決算処理.md and BlockLocker v1.14.1 SignParserImpl / ProfileFactoryImpl.

## Process / PI

Read-only decoding of the raw display-text evidence on a saved sign block entity. Modern Java signs have a `front_text` compound containing four `messages` (serialized text components) and may have a `back_text` compound. Legacy signs may instead contain `Text1` through `Text4`. Preserve the exact strings and the format; do **not** interpret the localized header, player identity, Everyone marker, or protection attachment from this step. BlockLocker gives display text precedence over mismatching saved PDC and accepts legacy text-only signs, so the future authoritative resolver must see both sources.

The decoder returns no evidence when neither recognized representation exists. A present but malformed/incomplete representation, or simultaneously present modern and legacy formats, fails closed. A missing back side is explicitly unknown, not proof of an empty backside. The returned collections are immutable snapshots.

## Test model / TCE

- BL-TEXT-01: modern front/back four-line text components are preserved without interpretation.
- BL-TEXT-02: a legacy text-only sign preserves Text1–Text4 even with no BlockLocker PDC.
- BL-TEXT-03: a missing optional back side remains unknown; missing both formats yields no evidence.
- BL-TEXT-04: malformed modern compounds, non-string lines, missing legacy lines, and ambiguous mixed formats throw IOException.
- BL-TEXT-05: returned lists cannot be modified through the decoder.

This unit cannot publish ownership or settlement. Text JSON interpretation, localized aliases, physical sign attachment, double-chest topology, live/disk consistency, and authoritative UUID resolution remain later stages. Formal CAT machine gates not-run; Draft is not Confirmed.

## Terrain candidate integration (additional Draft vectors)

The terrain adapter must expose display-text evidence from **every** sign with a recognized text representation, including signs with no BlockLocker PDC. The existing PDC-only hint extraction remains unchanged and is not an ownership classifier. Both evidence streams use validated chunk-local physical coordinates; duplicate sign positions abort the read.

- BL-TEXT-06 Red: a legacy text-only sign appears in `extractTextCandidates`, while `extract` still returns no PDC owner hint.
- BL-TEXT-07: modern front/back text is available at the same physical key as the PDC hint.
- BL-TEXT-08: duplicate physical sign entries and out-of-chunk coordinates fail closed before returning a text candidate.
