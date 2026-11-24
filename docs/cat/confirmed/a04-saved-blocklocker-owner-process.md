---
cat_version: '0.3'
kind: process
id: simplecoins.blocklocker.saved-owner
status: confirmed
process: simplecoins.blocklocker.saved-owner
source_refs:
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://github.com/surumeneco/SimpleCoins/issues/423
decision_ref: https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
refs:
  - simplecoins.blocklocker.saved-owner.pi.classify
semantic_contract: markdown-v2
language: ja
---

# Process: SAVED BlockLocker収納の有効保護分類

## 契約
閉世界: 真

## 境界
| PI |
| --- |
| simplecoins.blocklocker.saved-owner.pi.classify |

## 仕様状態
| ID | Domain |
| --- | --- |

## 観測能力
| ID | 情報源 | Domain |
| --- | --- | --- |
| 観測.保存元と地形接続 | 選択されたTerrain AnvilとBlockEntityおよび実際の支持先とダブルチェスト | 真偽 |
| 観測.有効保護判定 | BlockLocker 1.15の実導入版に一致する保護看板の設定・PDC・表示・表裏・ローカライズ | 真偽 |
| 観測.主所有者とEveryone | PRIVATE主プロフィールと追加看板全ての有効なアクセス許可プロフィール | 真偽 |
| 観測.重複と世代整合 | 選択したSAVED sourceの全看板・物理収納・slot・世代 | 真偽 |

## 作用能力
| ID | 対象 | Domain |
| --- | --- | --- |
| 作用.所有者候補 | 検証済みUUIDの未公開収納候補 | 真偽 |
| 作用.フリー候補 | Everyone有効・非保護・照合済み旧看板の所有者UUID特定不能の未公開収納候補 | 真偽 |
| 作用.保留 | 未確認・矛盾・欠損・世代不整合で未公開を維持する候補 | 真偽 |

## 起点
| ID | 由来 |
| --- | --- |
| 保存保護照合 | simplecoins.blocklocker.saved-owner.pi.classify/reconcile |

## 仕様境界と根拠

02Aにより、適用されるBlockLocker保護内の有効な[Everyone]を確認すればFree、そうでなければ検証済み主所有者UUIDに帰属する。02Bの現行仕様に従い、関連看板と主プロフィールを実際に照合できたがUUIDを特定できない旧看板はFreeとする。未読取・欠損・矛盾とは区別する。BAN・playerdataの現在状態からP/Dを判定するのは別段階。保護が実際にないと検証されたコンテナもFree。非保護の不確実性と検証済みの非保護は区別する。

02Bに従い、単なる表示文字列部分一致、PDC owner hint、未走査の看板、生成中のMinecraft Chunk、LIVEと別世代の地形を保護根拠とみなさない。表示テキスト優先、旧形式、表裏、設定で定義されたローカライズ、支持先と隣接ブロック、連結チェストの全看板を実装で確認する。実装が不可能な入力はUNKNOWNとして保留し、任意の名前からUUIDを創作しない。

PR #560はLIVEの公式BlockLocker 1.15 APIでowner/Everyoneを読取済み。PR #562は停止後のSAVED Anvilで看板5・チェスト7・2チャンクの物理対応だけを検証済みで、**有効なSAVED所有者をまだ復元していない**。ここで分類するのは選択済み保存元内の局所候補のみ。全world・全writer排他、BAN/playerdata同一世代、P/F/D最終集計、原子Snapshot公開、G3 ACK、Admissionを成立させない。

本Artifactは2026-11-20承認の分類に対し、現行02B（2026-11-24）の明記済み仕様と矛盾した旧看板の扱いを是正したものである。Process Closure、Model Conformance、Implementation Conformanceおよび実Paper受入は独立した未実行の工程である。
