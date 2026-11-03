---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.blocklocker.double-chest-owner-evidence
status: draft
process: simplecoins.blocklocker
source_refs:
  - https://drive.google.com/file/d/1sn-FXbU7nFDr6yc6YxPAe2V6U6mV2CZD/view
refs: []
semantic_contract: markdown-v2
language: ja
---

# DomainSpec: BlockLocker double-chest ownership evidence (candidate)

## 対象と境界
保存済み看板からチェストへの物理的な接続を解決し、単体チェストとダブルチェストの両半分の所有権証拠を整合させる工程。これは **CAT Draft / 未決定の検証候補** であり、現行コードの実装済み動作・確定仕様・正式Gate通過を意味しない。座標、看板の向き、左右の結合、所有者および Everyone の解釈は、実際のBlockLocker/Paper保存形式とAPIを照合して決定する。

## 候補ルール
| Ref | 条件 | 候補の安全要件 | Vector |
| --- | --- | --- | --- |
| bl.double.single | 単体チェストに有効な看板が結び付く | 接続先を一意に特定できる場合のみ所有権証拠を採用 | BL-DOUBLE-01 |
| bl.double.left-right | 左右両半分に別々の看板が存在する | 片方だけを読んで所有者を確定しない。競合する証拠はunknown | BL-DOUBLE-02 |
| bl.double.orientation | 看板の向きが対象チェストから外れている | 座標の近接だけで所有権証拠を採用しない | BL-DOUBLE-03 |
| bl.double.missing | 一方の半分または隣接チャンクが読めない | 読めない半分を空・無所有と扱わずunknown | BL-DOUBLE-04 |
| bl.double.alias | 同じ物理チェストが左右・別走査経路から再発見される | 物理対象を一度だけ計上し、別名で所有権を二重計上しない | BL-DOUBLE-05 |
| bl.double.everyone | Everyone表示と明示所有者が同時に存在する | Everyoneを無所有・ゼロ資産と同一視しない。解釈未確定ならunknown | BL-DOUBLE-06 |
| bl.double.invalid | 看板テキストが壊れている、権限APIが取得不能 | 解析失敗・外部API不在を所有者なしに変換せずunknown | BL-DOUBLE-07 |
| bl.double.change | 走査中にチェスト結合や看板が変わる | 時点整合の証拠がなければ確定した所有権として公開しない | BL-DOUBLE-08 |

## Test vectors（Draft）
| ID | 事前条件 | 期待する検証対象 |
| --- | --- | --- |
| BL-DOUBLE-01 | 有効な壁看板と単体チェスト、全対象保存データを取得 | 正しい面・接続先だけを候補にする |
| BL-DOUBLE-02 | ダブルチェスト左右に異なる所有者の看板 | 一方の所有者へ黙って確定しない |
| BL-DOUBLE-03 | 隣接しているがチェストを向いていない看板 | 偽陽性の所有権証拠を生成しない |
| BL-DOUBLE-04 | ダブルチェストの片半分が未ロード・欠損・読取不能 | unknownとし、ゼロ資産として公開しない |
| BL-DOUBLE-05 | 同じチェストを左右から2回列挙 | 一意な物理対象として集約する |
| BL-DOUBLE-06 | Everyoneと明示所有者が同時に観測される | API/保存仕様による権限意味論を未確認ならunknown |
| BL-DOUBLE-07 | 不正看板NBTまたはBlockLocker API不在 | fail-closedし所有者不明を維持 |
| BL-DOUBLE-08 | 走査前後で看板または左右結合が変化 | 同一時点性の証拠不足を検出 |

## 正本決定前の確認事項
1. BlockLockerの現行バージョンと保存形式における所有者、Everyone、複数看板の意味論。
2. Paperのダブルチェスト結合・座標・向きの権威取得方法、未ロード時の代替手段とスレッド制約。
3. 保存データとロード済みブロックの時点整合、失敗時のunknown伝播と決算非公開の境界。
4. 既存のPR #160 / #163のpalette入力形状・値マトリクスと重複せず、接続先・所有者の関係だけを扱うこと。

本書は実装前の検討素材であり、Confirmedへの昇格、正式CAT Process/PI/TCE Gate、JUnit Red/Green、Paper実機検証はいずれも未実施。
