# Phase 5 所有権証拠の判定境界

本書は、**「所有者が確認できないためフリー」と「所有権を調査できないため決算未完了」**を混同しないための統合検証表である。CATの確定仕様や実装済みアダプターではない。

## 根拠と責務

- [02A_所有者・統計定義](https://drive.google.com/file/d/1NDERmCBmKqFmBNYZQBKifQ6kXChdbB4g/view) の「所有者の定義（確定事項）」は、所有者が特定できない非保護資産・旧看板、WWC/WWWの指揮者→飼い主→フリー、現在BAN/有効playerdataによるP/F/D分類を規定する。
- [02B_データ収集・決算処理](https://drive.google.com/file/d/1eczGiqZgCNevfGhIJCkS2B1acvRRP0P8/view) は、未読取・失敗・時点不整合が残る決算を未完了とし、途中の統計公開を禁止する。
- [BAN証拠境界](phase5-ban-source-boundary.md)、[BlockLocker alias境界](phase5-blocklocker-alias-boundary.md)、[WCL提供元契約](phase5-wcl-provider-contract.md)が個別APIの詳細を保持する。本書はそれらを複製せず、集約側の判定順序だけを扱う。

## 判定順序

1. **読取の完全性**：対象コンテナ・個体・保護・提供元・必要なBAN情報・保存/ロード時点の証拠が揃ったかを検査する。未読取・不一致・提供元不明は`INCOMPLETE`として決算を停止する。これは「所有者がいない」と同義ではない。
2. **保管主体の帰属**：検証済みの無保護/有効`[Everyone]`なら`FREE`。保護看板の主所有者やMobの指揮者/飼い主は、**読取が成功して候補が不在と確定した場合だけ**次候補へ進む。全候補の検証済み不在は`FREE`。UUIDを取得できない旧看板も、看板・保護の読取自体が成功した場合の仕様上の`FREE`である。
3. **所有者UUIDの現況**：UUIDが確定した場合、現在BAN中またはplayerdata欠損なら`DEAD`、BANなしとplayerdata存在を両方確認できれば`PERSONAL`。BAN情報が不完全なら、他に正のBAN証拠がない限り`INCOMPLETE`。BAN解除イベント履歴だけでCLEARを推定しない。
4. **公開**：`INCOMPLETE`が一件でも残る全件決算は未完了。正しい`FREE`/ `DEAD`/ `PERSONAL`の部分集計があっても、全体の成功結果として公開しない。

## 統合受入ベクトル

| ID | 入力の事実 | 期待判定 | 検証境界 |
| --- | --- | --- | --- |
| OE-01 | 無保護で所有者が存在しないことを正常に読取 | FREE | 保管主体 |
| OE-02 | 同じ保護に適用される有効なEveryoneを正常に読取 | FREE | BlockLocker |
| OE-03 | 旧看板を正常に解析したが主所有者UUIDを識別できない | FREE | BlockLocker |
| OE-04 | 保護看板のNBT/テキスト/接続先の読取に失敗 | INCOMPLETE | BlockLocker |
| OE-05 | WWC/WWWの指揮者UUIDを確認、飼い主と異なる | 指揮者UUIDを選択 | WCL/WWC/WWW |
| OE-06 | 指揮者が検証済み不在、飼い主UUIDが既知 | 飼い主UUIDを選択 | WCL/WWC/WWW |
| OE-07 | 指揮者・飼い主とも検証済み不在 | FREE | WCL/WWC/WWW |
| OE-08 | 提供元のNotFound/Failureや未ロードPDC欠落で関係情報を検証できない | INCOMPLETE | WCL/WWC/WWW |
| OE-09 | UUID既知、現在BAN中（playerdata存否にかかわらず） | DEAD | BAN |
| OE-10 | UUID既知、BANなし、playerdata欠損 | DEAD | BAN/playerdata |
| OE-11 | UUID既知、BANなし、playerdata存在 | PERSONAL | BAN/playerdata |
| OE-12 | UUID既知、正のBANなし、必須IP/外部BAN情報が未取得 | INCOMPLETE | BAN |
| OE-13 | UUID既知、正のBANがあるが他のBAN提供元は未取得 | DEAD（正のBAN優先） | BAN |
| OE-14 | 保存済み/ロード済みの同一資産の時点・同一性が不明 | INCOMPLETE | 全件決算 |

`OE-13`は「BAN中」という肯定証拠の分類であり、他の資産ソースの未読取を免除しない。全件決算の完了判定では全ソースの完全性を別途要求する。

## 実装への引渡し

`BanEvidence`は宣言されたBANソースの集約器、`AssetOwner`（Draft PR #48）は**検証後**の分類器であり、どちらも外部情報の完全性を自動証明しない。未確認のowner UUIDを`null`として`AssetOwner.resolve()`へ渡すとFREEになるため、呼出側は`INCOMPLETE`と検証済み不在を型・分岐で分離する必要がある。

### 現行Draft実装との差分（未解決）

[PR #8](https://github.com/surumeneco/SimpleCoins/pull/8) の`BlockLockerVisibleSignResolver`（Git blob `bb66cebd22557796c3a240b8013e04a16e17acd0`）は、PRIVATE看板のidentity adapterが返す`FREE`を`UNKNOWN`へ変換する。`BlockLockerProtectionResolution`（blob `447be76aa545134b91bf0ca4abe16fd09a06a949`）は`FREE`をPRIVATE primaryとして拒否し、`UNKNOWN` primaryの集約結果も`UNKNOWN`とする。これは読取不能を誤ってFREEにしないための局所guardだが、**正常に読取・解析できた旧看板でUUIDだけを特定できないOE-03**と、**看板・提供元の読取が失敗したOE-04**を現在のprimary表現だけでは区別できない。

02Bの「UUIDを特定できない旧看板はフリー」と「実体を読めない場合は決算未完了」は両立する。解決時には`FREE`をPRIVATE primaryに直接流し込むのではなく、**看板・保護の検証完了**と**本人UUIDの検証済み不在**を独立した証拠として表現できるか、既存の[PRIVATE primary CAT Draft](https://github.com/surumeneco/SimpleCoins/blob/feature/owner-evidence-20261101/docs/cat/phase5-private-sign-primary-kind.md)を規範仕様と照合する。Draftの「lookup不明は常にUNKNOWN」を旧看板の検証済み不在に無条件適用してはならず、逆にlookup失敗をFREEへ読み替えてもならない。両者の境界を表すTCE/PIベクトルと正式Gateは**未実施**であり、ここで仕様確定や実装修正を宣言しない。

各OEベクトルは、正本CATの決定・Gate、外部プラグインの実機統合、決算公開制御の検証で対応付ける。表の記載だけでテスト成功・CAT Confirmed・Paper受入完了とは扱わない。
