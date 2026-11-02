---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.blocklocker.saved-sign-state
status: draft
process: simplecoins.blocklocker
source_refs:
  - https://drive.google.com/file/d/1sn-FXbU7nFDr6yc6YxPAe2V6U6mV2CZD/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# DomainSpec: BlockLocker保存済み看板paletteの完全性

## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.blocklocker | vanilla standing/wall signの物理mount検証 |
| simplecoins.snapshot | 保存済みpaletteの権威性と近傍完全性の別検証 |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| blocklocker.sign.standing | modern standing signのpalette Propertiesを検証するとき | rotation 0〜15とwaterlogged true/falseの完全な組を要求する | SC-SIGN-STATE-01, SC-SIGN-STATE-03 |
| blocklocker.sign.wall | modern wall signのpalette Propertiesを検証するとき | cardinal facingとwaterlogged true/falseの完全な組を要求する | SC-SIGN-STATE-02, SC-SIGN-STATE-03 |
| blocklocker.sign.missing-waterlogged | standingまたはwall signのwaterloggedが欠損したとき | mountをverifiedとみなさずfail-closedで拒否する | SC-SIGN-STATE-01, SC-SIGN-STATE-02 |
| blocklocker.sign.invalid | waterloggedが不正、予期しないProperties、またはhanging signであるとき | 対応対象外として拒否する | SC-SIGN-STATE-04 |
| blocklocker.sign.scope | palette stateが形式上完全なとき | 物理mount形式のみを検証し、BlockLocker所有者・ローカライズ・近傍完全性を推定しない | SC-SIGN-STATE-03 |

## 境界と状態

既存SC-SIGN-STATE-01〜04を意味変更せず構造化したTASK-20261101-004のCAT Draft。palette完全性だけではBlockLocker 1.14.1 runtime sign matching、所有権、近傍全件、live Paper snapshotを証明しない。正式CAT Process/PI/TCE・機械Gate・実機検証はnot-run。Confirmed・マージ・決算公開を意味しない。
