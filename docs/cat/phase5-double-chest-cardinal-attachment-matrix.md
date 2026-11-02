---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.blocklocker.double-chest-cardinal-attachment-observation
status: draft
process: simplecoins.blocklocker
source_refs:
  - https://drive.google.com/file/d/1sn-FXbU7nFDr6yc6YxPAe2V6U6mV2CZD/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# DomainSpec: BlockLocker double-chest attachment geometry characterization

## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.blocklocker | 既存の `BlockLockerChestLinkage` と `BlockLockerSignAttachment` の方向・保管先境界 |

## 規則

この表の「要求」は既存コードの観測ベクトルであり、規範CAT仕様のConfirmedへの昇格を意味しない。

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| geometry.cardinal | NORTH/EAST/SOUTH/WEST、CHEST/TRAPPED_CHEST、両半分の相互検証 | 正しい reciprocal pair は左右いずれの順でも linked と判定 | SC-BL-GEOM-MATRIX-01 |
| geometry.support | 各半分に実際に支持された4方向の壁看板・立て看板 | 物理的に支持される看板だけが pair に attaches と判定 | SC-BL-GEOM-MATRIX-01 |
| geometry.decoy | 同一ワールド内の遠隔看板 | pair の所有権証拠として接続されない | SC-BL-GEOM-MATRIX-01 |
| geometry.invalid | 同じ half または異なる CHEST/TRAPPED_CHEST | linked は false、signAttachedToPair は IOException で fail-closed | SC-BL-GEOM-MATRIX-02 |

## 状態・境界

これは既存の Phase 5 Draft ソースを対象にした **code-to-spec 観測**であり、新しい規範的なCAT Confirmed仕様ではない。既存のテストで扱う単一方向・一部ケースを、方向×種類×支持先の交差行列で補完する。実行結果は実ソース・Git blob・JDK版と対応付ける。

実際の保護の存在、`[Everyone]`、primary UUID、同一時点のチャンク、BlockLocker v1.14.1との実機整合性は本テストからは証明できない。正式なProcess/PI/TCE GateとPaper実機受入は別工程である。
