---
cat_version: '0.3'
kind: process
id: simplecoins.currency.station-result-local-decision
status: draft
process: simplecoins.currency.station-result-local-decision
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
  - https://github.com/surumeneco/SimpleCoins/pull/529
decision_ref: pending-user-approval:a12-local-decision-process
refs: 
  - simplecoins.currency.pi.station-result-local
semantic_contract: markdown-v2
language: ja
---

# Process: Merchant・Cartography結果取得イベントの局所取消判定（提案）

## 契約
閉世界: 真

## 境界
| PI |
| --- |
| simplecoins.currency.pi.station-result-local |

## 仕様状態
| ID | Domain |
| --- | --- |

## 観測能力
| ID | 情報源 | Domain |
| --- | --- | --- |

## 作用能力
| ID | 対象 | Domain |
| --- | --- | --- |

## 起点
| ID | 由来 |
| --- | --- |
| 判定 | simplecoins.currency.pi.station-result-local/evaluate |

## 境界と採否判断（未承認）

本Processは、**すでにPaperアダプターにより正規化された3つの真偽値**から、当該イベントに対してSimpleCoinsが追加で取消を要求するかを決定する**純粋な判定責務だけ**を提案する。入力の有限組合せは下記TCEで閉じられるため、この局所関数の閉世界を真とする提案である。

既存Confirmed `simplecoins.currency.station-consumption-exclusion` の **閉世界: 偽** を真に変更する提案ではなく、全ての資産変更経路が保護されたという主張でもない。

入力の取得、`InventoryClickEvent` と結果raw slot 2の識別、Merchant/Cartographyの入力枠の実ItemStack/PDCの取得、Paperイベントの実取消、他Pluginによる後段変更、直接NMS操作、ネットワーク/JE/BE操作、貨幣保存/G0/G3は**独立した技術保証**とし、本Processの局所閉包に紛れ込ませない。入力情報を作るアダプターについては既存Confirmed A12 Processおよび実Paperの試験証拠で別途検査する。

本Draftの正式Confirmedへの昇格には利用者の承認を要する。本Processで形式モデルが閉包したとしても、既存Confirmed A12のProcess ClosureをPassedと判定しないし、製品コードの書込み権限を付与するものでもない。
