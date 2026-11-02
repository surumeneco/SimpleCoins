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

## 観測ベクトル

| ID | 固定ソース上の条件 | 期待される観測 |
| --- | --- | --- |
| LIVE-SIGN-01 | NORTH向きの壁看板・PRIVATE見出し・既知の主所有者 | 支持位置が一致するコンテナへPERSONALの主所有者証拠を渡す |
| LIVE-SIGN-02 | 立看板・PRIVATE見出し・既知の主所有者 | 直下のコンテナへPERSONALの主所有者証拠を渡す |
| LIVE-SIGN-03 | FRONTの行数不足 | `IOException`として拒否し、所有者を推定しない |
| LIVE-SIGN-04 | 壁看板でも立看板でもないブロック状態 | `IOException`として拒否する |
| LIVE-SIGN-05 | ライブ看板APIが実行時例外を返す | `IOException`として拒否する |
| LIVE-SIGN-06 | BlockLocker見出しと一致しない看板 | 候補なしを返し、主所有者APIを呼ばない |

## 未検証・非目標

テスト用Paperインターフェースの動的Proxyは実サーバーの`Sign`、`SignSide`、BlockLocker 1.14.1の有効な保護状態を証明しない。旧看板のUUID検証済み不在（OE-03）とAPI読取失敗（OE-04）の未解決な規範差異を変更しない。実機でのWorld/Chunk整合性、正式CAT Gate、Provider版の照合、全件決算の完了は別工程。
