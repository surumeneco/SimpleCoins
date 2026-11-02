---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.blocklocker.legacy-uuid-absence-boundary
status: draft
process: simplecoins.blocklocker
source_refs:
  - https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view
  - https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view
  - https://github.com/surumeneco/SimpleCoins/pull/8
refs: []
semantic_contract: markdown-v2
language: ja
---

# DomainSpec Draft: 旧BlockLocker看板のUUID不明と読取不能の区別

## 位置づけ

02B「BlockLocker保存形式の調査結果」は、**実体を正常に読み取った旧看板で主所有者UUIDを特定できない場合はフリーF**とし、**看板・外部情報の読取不能は決算未完了**としている。02AでもUUID不明資産はフリーとする。一方、PR #8の`BlockLockerVisibleSignResolver`はidentity adapterの`FREE`を`UNKNOWN`へ正規化し、`BlockLockerProtectionResolution`はPRIVATE primaryの`FREE`を拒否する。この差異を未解決のDraftとして扱う。ここで既存仕様の意味や実装の採用方式を変更・確定しない。

## 受入条件の候補

| ID | 入力と証拠 | 期待する分類 | 検証 |
| --- | --- | --- | --- |
| BL-LEGACY-01 | 有効な物理的保護・旧表示テキストを正常に確認し、信頼できるidentity sourceがUUIDの検証済み不在を返す | FREE（02Bの旧看板規則）。単なるlookup失敗をこの条件に含めない | 旧形式保存テキスト、identity adapterのverified absenceを表現するRed |
| BL-LEGACY-02 | 旧看板は確認できてもidentity sourceが利用不能・未照合・曖昧 | UNKNOWN/決算未完了。FREEを推測しない | unknown control |
| BL-LEGACY-03 | PDC hintと表示テキストが矛盾、または保存データの読取に失敗 | UNKNOWN/決算未完了。BL-LEGACY-01へ昇格しない | 既存のPDC/decoder不整合テストを再利用 |

## 意味上の境界

現行の`PrimaryIdentity`は`ExternalOwnerDecision.Evidence`を返すが、`FREE`が「検証済みUUID不在」なのか「保護全体の集約済みFREE」なのかを区別する証拠型がない。**旧看板・物理的保護・権威ある本人照合がすべて成功した証拠**を型または明示的な判定経路に保持する設計が必要。新しいテストで`FREE`を返すadapterはこの仕様差を示すための実験入力であり、実運用で任意のlookup失敗をFREEへ読み替える許可ではない。

## 状態・依存

- Draft。02A/02Bの確定事項とPR #8の実装観測を区別する。
- BL-LEGACY-01は現行実装でRedになる想定。RedはCAT Gate通過やGreen実装を意味しない。
- 正式なTCE/PI、Confirmedへの正当な昇格、`process-closure`・`model-conformance`・`implementation-conformance`等の正規Gateは未実行。
- 実機のBlockLocker 1.14.1、看板の取付・チャンク/ダブルチェスト、同時点snapshot、BAN・playerdataを照合するまで決算の公開可能性は未証明。
