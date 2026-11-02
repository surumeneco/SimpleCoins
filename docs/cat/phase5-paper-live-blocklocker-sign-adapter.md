---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.blocklocker.paper-live-sign-adapter
status: draft
process: simplecoins.blocklocker
source_refs:
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1sn-FXbU7nFDr6yc6YxPAe2V6U6mV2CZD/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# DomainSpec: Paperのライブ看板からBlockLocker候補証拠を取得する境界（Draft）

## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.blocklocker | Paper Signの前面4行・ブロック位置・設置方向を検証済み候補へ変換 |
| simplecoins.owner | 文字列だけで帰属を確定せず、別途検証された主所有者と物理的な保護対象の照合を要求 |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| live-sign.front | サーバーの看板前面4行が存在し、PRIVATEヘッダーと正規の主所有者を照合できる | 正確な座標と設置方向の候補証拠を作り、別工程で実際のコンテナへの取り付けを照合する | BL-LIVE-01, BL-LIVE-06 |
| live-sign.everyone | PRIVATE看板の前面に有効なEveryoneがあり、主所有者を独立に検証済み | 接続済みコンテナでのみFREE判定へ進める。文字列だけでFREEと確定しない | BL-LIVE-02 |
| live-sign.unrelated | 前面のヘッダーが設定済みBlockLockerヘッダーに一致しない | 所有者照会を行わず候補なしを返す | BL-LIVE-03 |
| live-sign.invalid | 看板不在・未対応ブロック状態・前面欠損・Paper読取例外 | IOExceptionとして拒否し、UNKNOWNをゼロ資産/FREEと混同しない | BL-LIVE-04 |
| live-sign.identity | PRIVATEの主所有者照会が失敗する | 不完全な所有者証拠を確定しない | BL-LIVE-05 |

## 受入ベクトル（テスト準備）
- BL-LIVE-01: NORTH向き壁看板、前面PRIVATE、AliceのUUIDを検証したとき、座標とmountを保持し単一チェストに帰属する。
- BL-LIVE-02: 同じ検証済みPRIVATEの前面にEveryoneがあるとき、保護対象に取り付けられた場合のみFREE。
- BL-LIVE-03: 一般看板の前面はOptional.empty、identity resolverの呼出しなし。
- BL-LIVE-04: null Sign、未対応BlockData、4行未満、Paper API読取例外はIOException。
- BL-LIVE-05: identity resolverのIOExceptionを保持し、PERSONAL/FREEを捏造しない。
- BL-LIVE-06: 4方向の壁看板と直立看板のmountを区別し、別の方向へ暗黙変換しない。

## Gateと証拠の境界
本書は既存02A/02Bと現行コードから抽出した**Candidate/Draft**であり、Confirmedではない。正式CAT Process/PI/TCE Gateは未実行。モックによるPaper API契約テストはPaper 26.2・BlockLocker 1.14.1実機での取付け、保護、非同期安全性、保存データの完全性を証明しない。
