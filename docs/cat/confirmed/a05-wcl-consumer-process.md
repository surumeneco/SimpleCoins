---
cat_version: '0.3'
kind: process
id: simplecoins.external-storage-provider-integration
status: confirmed
process: simplecoins.external-storage-provider-integration
source_refs:
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/issues/4
  - https://github.com/surumeneco/SimpleCoins/blob/develop/docs/verification/phase5-wcl-provider-contract.md
decision_ref: user-chat:2026-11-18:A05-A12-drafts-approved
refs:
  - simplecoins.external-storage-provider-integration.pi.capture
semantic_contract: markdown-v2
language: ja
---

# Process: WWC/WWW収納の根拠付き決算候補への取込み

## 契約
閉世界: 真

## 境界
| PI |
| --- |
| simplecoins.external-storage-provider-integration.pi.capture |

## 仕様状態
| ID | Domain |
| --- | --- |

## 観測能力
| ID | 情報源 | Domain |
| --- | --- | --- |
| 観測.必要な収納提供者の登録 | 現に有効なWWC/WWWとWCL共通Reader登録 | 真偽 |
| 観測.個体と保存元の同一性 | LIVE個体又は別途確定したSAVED Entityソース | 真偽 |
| 観測.読取結果と関係者 | 完全性を照合したWCL StorageSnapshot又は明示的不成功 | 真偽 |
| 観測.同一世代の取込み | 上位G0が要求するソース世代と全体の重複排除 | 真偽 |

## 作用能力
| ID | 対象 | Domain |
| --- | --- | --- |
| 作用.候補保留 | 未公開の決算候補 | 真偽 |
| 作用.個体の正規化候補 | 上位決算へ渡す未公開の保管実体 | 真偽 |

## 起点
| ID | 由来 |
| --- | --- |
| 収納照合 | simplecoins.external-storage-provider-integration.pi.capture/reconcile |

## 正本要求と未確定の技術接続

02Aは、WWC/WWW個体の貨幣・財布を指揮者→飼い主→フリーの優先順と現在BAN/playerdataによってP/F/Dへ帰属させると確定している。02Bは、LIVE・SAVEDの双方と入れ子収納を一度だけ走査し、未読取や競合をゼロとして公開しないことを要求している。本Processはその**収納読取の候補取込み**だけを扱う。G0のグローバルなwriter静止、発行G3、統計の原子的公開は本Processで勝手に成立させない。

現行WCL `WonderfulStorageReader` の `readLoaded/readStored` と WWC `follow_target`、WWW `commander/owner`、slot付き serialized ItemStack は既存の提供者機能として観測済み。WCL側のAPI再開発は対象外。提供者の登録全件、必要な配備構成、Entity UUID/world UUID/providerId、保存元の世代、既存収納との重複、独立した帰属根拠を検証してから候補とする。読取不能・NotFound・曖昧さを「資産0」へ変換しない。単一個体の空のSuccessはその読取先が空である根拠に限る。

本Artifactの候補採用・保留・対象外判定の意味は利用者が明示承認済み。正式CAT Gateの通過、実Paperの製品結線、未確認の全writer排他・グローバルな決算公開を宣言しない。下位サービスの呼出しはPaperのメインスレッド制約に従うが、実装クラスの構成、サービス取得方式、snapshot生成手法をこの規範候補で不必要に固定しない。
