---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.blocklocker.saved-sign-text
status: draft
process: simplecoins.blocklocker
source_refs:
  - https://drive.google.com/file/d/1sn-FXbU7nFDr6yc6YxPAe2V6U6mV2CZD/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5: saved sign display-text evidence (CAT Draft)

## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.blocklocker | 保存済み看板の表示テキストとPDC hintの独立証拠 |
| simplecoins.terrain | terrain chunkのNBT形状・座標一意性 |
| simplecoins.settlement | 未検証テキストからの所有権確定禁止 |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| text.modern | modern front_text/back_textが存在するとき | 四つのraw messagesを解釈せず保存し、back側がない場合はunknownとして保持する | BL-TEXT-01, BL-TEXT-03 |
| text.legacy | Text1〜Text4だけが存在するとき | BlockLocker PDCがなくても四つのraw stringsを保持し、テキストのみの候補を捨てない | BL-TEXT-02, BL-TEXT-06 |
| text.malformed | modern/legacy形式が不完全・型不正・同時存在するとき | IOExceptionで拒否し、部分的な表示テキストを所有権証拠として確定しない | BL-TEXT-04 |
| text.immutable | 保存済みテキストを読み終えたとき | 外部から変更できないread-only snapshotを返す | BL-TEXT-05 |
| text.candidate | terrain chunkの看板テキスト候補を列挙するとき | PDC hintと同一物理座標で照合でき、重複位置・chunk外座標を拒否する | BL-TEXT-07, BL-TEXT-08 |
| text.raw-nbt | named-root NBTのmodern text-only看板を読み取るとき | 実NBT tag shapeから四行を保持し、存在しないPDC所有者を推測しない | BL-TEXT-09 |
| text.joined | PDCのみ・textのみ・双方の証拠を結合するとき | 座標keyのread-only recordに両streamのOptionalを保持し、片方欠損をFREEと扱わない | BL-TEXT-10 |
| text.joined-malformed | 結合対象の一方に不正な証拠があるとき | 他方が正常でも結合全体を拒否する | BL-TEXT-11 |
| text.joined-empty | どちらの証拠もない看板を結合するとき | 候補から除外し、無保護という判定を出さない | BL-TEXT-12 |

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

## Raw NBT integration (verification vector)

- BL-TEXT-09: a serialized named-root NBT terrain chunk with a text-only modern sign is decoded through `NbtReader` and `extractTextCandidates`. Preserve all four raw messages and return no saved-PDC owner hint. This tests the real NBT tag shape, not just hand-built Java maps.

## Joined saved-sign evidence (additional Draft vectors)

An intermediate caller should be able to obtain PDC hints and raw display-text evidence in one coordinate-keyed, read-only record. Include a sign when either stream has evidence, including legacy text-only signs. A missing stream remains `Optional.empty`; it is not an inferred owner or free-asset classification. Malformed present evidence in either stream aborts the combined read.

- BL-TEXT-10 Red: PDC-only, text-only and combined signs retain their respective evidence without silently dropping text-only signs.
- BL-TEXT-11: a malformed present text/PDC stream rejects the combined read rather than publishing the other stream alone.
- BL-TEXT-12: a sign with neither recognized evidence is omitted from the joined candidate map, not classified as unprotected.
