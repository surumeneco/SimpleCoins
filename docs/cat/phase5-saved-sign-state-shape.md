---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.blocklocker.saved-sign-state-shape
status: draft
process: simplecoins.blocklocker
source_refs:
  - https://drive.google.com/file/d/1sn-FXbU7nFDr6yc6YxPAe2V6U6mV2CZD/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# DomainSpec: saved BlockLocker sign state shape (code-to-spec candidate)

## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.blocklocker | 保存済みAnvil paletteのName/Propertiesを物理的な看板mountへ変換する入力境界 |

## 観測した現行コードの振る舞い（規範未確定）
| ID | 条件 | 現行コードの期待結果 | 検証 |
| --- | --- | --- | --- |
| saved-sign.null | stateがnull | IOException | BL-SAVED-SHAPE-01 |
| saved-sign.name | Nameが欠落、null、非String、空文字、未知namespaceまたは未知block | IOException | BL-SAVED-SHAPE-02 |
| saved-sign.properties | Propertiesが欠落、null、Map以外、空Map | IOException | BL-SAVED-SHAPE-03 |
| saved-sign.unsupported | hanging signやunknown wood、看板ではないblock | IOException | BL-SAVED-SHAPE-04 |

## 適用境界
本書は既存の`BlockLockerSavedSignMount.fromPaletteState`を読んで作成したcode-to-spec **Draft** であり、ユーザー承認済みの仕様、実機での保存形式の完全性、正式CAT Gate通過を意味しない。PR #160のwood/facing/rotation/property値マトリクスとは異なり、トップレベルの入力形状・未対応block種別だけを特徴付ける。未知入力をFREEの所有権証拠へ昇格させない。正式TCE/PI・規範決定・Paper実機検証は未実施。
