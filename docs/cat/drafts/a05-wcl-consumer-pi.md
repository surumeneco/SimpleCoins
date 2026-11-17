---
cat_version: '0.3'
kind: pi
id: simplecoins.external-storage-provider-integration.pi.capture
status: draft
process: simplecoins.external-storage-provider-integration
source_refs:
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/blob/develop/docs/verification/phase5-wcl-provider-contract.md
decision_ref: ''
refs: []
semantic_contract: markdown-v2
language: ja
---

# PI: WCL保管証拠の取込み境界（Draft）

## 境界
| 相手Process | 方向 |
| --- | --- |
| simplecoins.world-source-discovery | 受信 |
| simplecoins.settlement | 送信 |

## 項目
| ID | 方向 | Domain | 存在 |
| --- | --- | --- | --- |
| 入力.必要提供者検証済 | 受信 | 真偽 | 必須 |
| 入力.対象個体確認済 | 受信 | 真偽 | 必須 |
| 入力.対象外検証済 | 受信 | 真偽 | 必須 |
| 入力.読取成功 | 受信 | 真偽 | 必須 |
| 入力.個体と提供者の同一性一致 | 受信 | 真偽 | 必須 |
| 入力.帰属根拠確認済 | 受信 | 真偽 | 必須 |
| 入力.同一世代かつ重複排除済 | 受信 | 真偽 | 必須 |
| 入力.矛盾あり | 受信 | 真偽 | 必須 |
| 出力.候補採用 | 送信 | 真偽 | 必須 |
| 出力.対象外継続 | 送信 | 真偽 | 必須 |
| 出力.決算保留 | 送信 | 真偽 | 必須 |

## 操作
| ID | 方向 | 起点 |
| --- | --- | --- |
| reconcile | 受付 | 収納照合 |

## 制約
| 式 |
| --- |

## 入力と結果の意味

- `必要提供者検証済`: 必要なWWC/WWW保管readerの集合が、実際の有効Plugin/JARとの対応を含めて揃い、同一IDの重複や偽の登録がないこと。WCLの`ServicesManager.load`で一件だけ選んだ状態は不十分。現行で実配備されていないproviderを「空の収納」と置換しない。**登録結果そのものは全Entityの発見証拠ではない**。
- `対象個体確認済`: 出所が特定されたLIVE/SAVEDの実個体を、このReaderの対象だと積極的に同定できたこと。`対象外検証済`: その実個体がこのReaderの処理対象でないことを、正本の種別・identityにより検証できたこと。両者とも偽なら未確定。対象個体確認済と対象外検証済が同時に真であれば矛盾。
- `読取成功`: 対象の特定個体について、適正な登録済み提供者から `StorageReadResult.Success` 相当のsnapshotを受け取れたこと。`NotFound`と`Failure`、供給元不明と「成功でitemsが空」は別の観測であり、前二者から資産0を導かない。
- `個体と提供者の同一性一致`: 結果のEntity UUID、world UUID、providerId、取得元と登録主体が対象と一致すること。未ロードの場合は別途確定した保存済みPDCソースを提供し、読取によってchunkをロードしたと偽らない。
- `帰属根拠確認済`: `commander`（WWW）又は`follow_target`（WWC）の証拠、tame owner、両者の不在や不明の区別が確定し、02Aの優先規則に従った帰属候補を得られること。**単に関係キーが欠けたという理由だけでFREEを確定しない**。判定したUUIDのBAN/playerdata現在状態は上位の別責務で照合する。
- `同一世代かつ重複排除済`: 上位のG0契約に従ってLIVE/SAVED競合、同一個体/slot、バニラ装備・保管物、入れ子のシュルカー/バンドルを二重計上せず、取得から候補切替までの同一会計世代が保証できること。個体単独の保存ファイルhashが安定しただけでは真にできない。
- `候補採用`: 上位決算へ渡せる**未公開**の収納/帰属候補の構成を許可する。集計値の最終確定や入室開放を指示しない。`対象外継続`は他ソースの探索を継続するだけ。`決算保留`は必要証拠不足・不一致なら公開不可を維持する。
- 未取得の観測を「偽と証明済み」とみなさない。この真偽の入力写像を確実にする仕組みは別途技術・承認対象であり、Draft表のGreenから断定しない。
