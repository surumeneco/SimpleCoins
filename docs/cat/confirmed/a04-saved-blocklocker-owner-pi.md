---
cat_version: '0.3'
kind: pi
id: simplecoins.blocklocker.saved-owner.pi.classify
status: confirmed
process: simplecoins.blocklocker.saved-owner
source_refs:
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
decision_ref: https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# PI: SAVED収納の保護帰属候補

## 境界
| 相手Process | 方向 |
| --- | --- |
| simplecoins.settlement | 双方向 |

## 項目
| ID | 方向 | Domain | 存在 |
| --- | --- | --- | --- |
| 入力.保存地形ソース同定済 | 受信 | 真偽 | 必須 |
| 入力.全関連看板照合済 | 受信 | 真偽 | 必須 |
| 入力.物理収納重複排除済 | 受信 | 真偽 | 必須 |
| 入力.保存源局所世代整合済 | 受信 | 真偽 | 必須 |
| 入力.保護適用を検証済 | 受信 | 真偽 | 必須 |
| 入力.非保護を検証済 | 受信 | 真偽 | 必須 |
| 入力.主所有者UUID検証済 | 受信 | 真偽 | 必須 |
| 入力.Everyone有効を検証済 | 受信 | 真偽 | 必須 |
| 入力.Everyone不在を検証済 | 受信 | 真偽 | 必須 |
| 入力.矛盾あり | 受信 | 真偽 | 必須 |
| 出力.所有者候補 | 送信 | 真偽 | 必須 |
| 出力.フリー候補 | 送信 | 真偽 | 必須 |
| 出力.決算保留 | 送信 | 真偽 | 必須 |

## 操作
| ID | 方向 | 起点 |
| --- | --- | --- |
| reconcile | 受付 | 保存保護照合 |

## 制約
| 式 |
| --- |

## 入力の独立証拠

- 有効保護・積極的に検証済みの非保護・UNKNOWNを区別する。全看板走査なしに「看板が見当たらない」だけでは非保護とはしない。
- Everyone有効は、同一収納の適用可能な全看板・全プロフィールに実際に存在すると検証した場合。Everyone不在はその全てを検査して積極的に不在と検証した場合。両方偽ならUNKNOWN、両方真なら矛盾。
- Everyoneが検証済みであれば主所有者UUIDが不明でもFree。Everyoneが検証済みで不在なら有効PRIVATE主プロフィールの検証済みUUIDだけを所有者候補にする。全関連看板と有効PRIVATE主プロフィールを実際に照合でき、欠損・矛盾なしでUUID特定不能と判明した場合はFree。未読取・主看板欠損・複数主プロフィールの矛盾をUUID不明として扱わない。名前と未照合PDC hintからUUIDを推測しない。UUIDは上位で現在BAN・playerdataを検証しP/D判定する。
- 所有/Freeは局所の**未公開**候補のみで、全writerの同一会計世代・G0 publish・Admissionを一切示さない。保存源不一致、物理重複、未証明の空保護はHOLD。
