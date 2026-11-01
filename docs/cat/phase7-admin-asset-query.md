---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.admin.asset-query
status: draft
process: simplecoins.admin
source_refs:
  - https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1KxmgwxYdE5dhBxW6mwzaYezxcRaSWx8o/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# DomainSpec: 運営向け保有額・死蔵資産照会（CAT Draft）

## 対象
| Ref | Aspect |
| --- | --- |
| simplecoins.admin | 管理権限と照会結果の公開境界 |
| simplecoins.snapshot | 完了した統計・所在索引と照会時点 |
| simplecoins.owner | 現在BAN・playerdata欠損による死蔵分類 |

## 規則
| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| admin.query.permission | 管理照会を呼び出すとき | 管理権限がない主体へ保有額・死蔵所在・UUIDを開示しない。権限はデフォルトOP、権限プラグインから付与可能 | ADMIN-QUERY-01 |
| admin.query.player | 任意のプレイヤー保有額を照会するとき | UUIDに対応する検証済みA[u]と統計の時点・対象範囲を明示し、未知・未集計を0と偽らない | ADMIN-QUERY-02 |
| admin.query.dead-total | 死蔵資産の集計を照会するとき | DをP・F・T=P+Fから独立した総額として提示し、Dを通常総額へ加算しない | ADMIN-QUERY-03 |
| admin.query.dead-location | 死蔵資産の内訳を照会するとき | 保管場所ごとの金額、ワールド・座標または論理保管先、保管種別、所有者UUID、現在BANまたはplayerdata欠損の該当理由を提示する | ADMIN-QUERY-04 |
| admin.query.incomplete | 必須source・所有者・BAN証拠または整合した所在索引が未確認のとき | 欠落を資産0・空一覧・正常な完了として公開せず、未確定を識別可能にする | ADMIN-QUERY-05 |
| admin.query.scope | ワールド統計ON/OFFが適用されるとき | world固有sourceの対象範囲を明示し、world非依存playerdata等を対象から落とさない | ADMIN-QUERY-06 |

## 受入ベクトル（未生成・未実行）
- ADMIN-QUERY-01: 権限のない送信者の照会は拒否され、保有額・UUID・死蔵所在を返さない。権限を付与した送信者は同じ照会境界へ進める。
- ADMIN-QUERY-02: 記録済みで有効なUUIDのA[u]を返す。未知UUID、未集計または時点不明の値を検証済み0として表示しない。
- ADMIN-QUERY-03: P=100、F=20、D=30なら通常T=120、死蔵D=30を別項表示する。Dを含む150を通常Tとしない。
- ADMIN-QUERY-04: BAN中UUIDの保護チェストは金額・world・座標・種別・UUID・BAN理由を示す。world非依存の所在は論理保管先を示し、架空の座標を付けない。
- ADMIN-QUERY-05: 必須BAN source不明、未走査の保存領域または未確定の所在索引があるとき、空の死蔵一覧を完全な0件と認定しない。
- ADMIN-QUERY-06: world固有sourceを設定で除外しても、world非依存playerdata/エンダーチェストの対象性を維持する。

## 仕様状態・実装境界
本書は既存の運営設定・統計定義にある1.0.0要求を照会契約として整理した**Draft**。具体的なコマンド構文、ページング、未確定時の表示形式、snapshotの取得・公開方法は未決定であり、本書から独断で確定しない。

現行 `CoinCommand` は `give` のみを実装し、管理照会は未実装。正式CAT Process/PI/TCE、process-closure、model-conformance、vectors/Queue/current-item生成Gate、Java Red/Green、Paper実機照会は**not-run**。本書の作成や構文lintだけでは実装・検証・公開を完了したことにならない。
