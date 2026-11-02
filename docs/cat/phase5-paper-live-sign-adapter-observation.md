---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.phase5.paper-live-sign-adapter-observation
status: candidate
process: simplecoins.owner-blocklocker-evidence
source_refs:
  - https://github.com/surumeneco/SimpleCoins/blob/d71683ad7f9e29de24af2f7c6de4663909990b38/src/main/java/co/surumene/simplecoins/PaperBlockLockerSignAdapter.java
refs: []
semantic_contract: markdown-v2
language: ja
---

# Phase 5: Paper実行時BlockLocker看板アダプター（code-to-spec Candidate）

## 証拠と適用境界

固定ソース `PaperBlockLockerSignAdapter.java`（Git blob `c8439b8f320a84f3802515e6bd1b4e14baf4e289`）の既存分岐を、Paper APIのインターフェースを模したテストで観測する。現行コードのcharacterizationであり、規範CAT Confirmedや実機検証ではない。ライブ看板のブロック状態とフロント4行を呼出側が一貫したスナップショットとして取得する義務は変わらない。

## 対象

| 参照 | 観点 |
| --- | --- |
| `PaperBlockLockerSignAdapter.readPaper` | Paper SignのFRONTテキストとBlockDataの読取境界 |
| `BlockLockerProtectionResolution.single` | 検証済み壁・立看板のコンテナ支持位置への集約 |

## 規則

以下は固定コードの**観測条件**であり、規範仕様としての新しい要求ではない。

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| LIVE-SIGN-01 | NORTH向き壁看板・PRIVATE・既知の主所有者 | 支持先コンテナにPERSONALを渡す既存分岐 | wallAndStandingSignGeometryRetainVerifiedOwner |
| LIVE-SIGN-02 | 立看板・PRIVATE・既知の主所有者 | 直下コンテナにPERSONALを渡す既存分岐 | wallAndStandingSignGeometryRetainVerifiedOwner |
| LIVE-SIGN-03 | FRONT行数不足 | IOExceptionで拒否する既存分岐 | malformedFrontOrUnsupportedStateFailsClosed |
| LIVE-SIGN-04 | 未対応BlockData | IOExceptionで拒否する既存分岐 | malformedFrontOrUnsupportedStateFailsClosed |
| LIVE-SIGN-05 | Paper Sign APIの実行時例外 | IOExceptionで拒否する既存分岐 | malformedFrontOrUnsupportedStateFailsClosed |
| LIVE-SIGN-06 | BlockLocker見出しと不一致 | 候補なし、identity API非呼出の既存分岐 | unrelatedSignDoesNotResolvePrimaryIdentity |
| LIVE-SIGN-07 | PRIVATE看板のEveryone表示、既知/不明の主所有者 | 主所有者が既知ならFREE、不明ならUNKNOWNの既存分岐 | liveEveryoneNeedsResolvedPrimaryBeforePublicOwnership |

## 未検証・非目標

テスト用Paperインターフェースの動的Proxyは実サーバーの`Sign`、`SignSide`、BlockLocker 1.14.1の有効な保護状態を証明しない。旧看板のUUID検証済み不在（OE-03）とAPI読取失敗（OE-04）の未解決な規範差異を変更しない。実機でのWorld/Chunk整合性、正式CAT Gate、Provider版の照合、全件決算の完了は別工程。
