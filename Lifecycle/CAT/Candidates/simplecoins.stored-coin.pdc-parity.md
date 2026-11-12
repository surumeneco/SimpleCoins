---
cat_version: '0.3'
kind: domain-spec
id: simplecoins.stored-coin.pdc-parity
status: candidate
process: simplecoins.settlement.stored-coin
source_refs:
  - https://github.com/surumeneco/SimpleCoins/blob/51e134d860de8f937f731832d97db24b15f16b15/src/main/java/co/surumene/simplecoins/domain/StoredItemValue.java
  - https://github.com/surumeneco/SimpleCoins/blob/fbc0616ea33a4753c6ccd42df48855c3e870a358/src/main/java/co/surumene/simplecoins/paper/CoinItemCodec.java
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
decision_ref: ''
refs: []
semantic_contract: markdown-v2
language: ja
---

# Source-bound Candidate: 保存貨幣の識別キー整合

## Observation（実ソースで確認）

- `develop@51e134d` の `StoredItemValue.value` は保存済み現物貨幣を `PublicBukkitValues["simplecoins:denomination"]` で識別する。このキーが無くても通常アイテムとして `0` を返す。
- 積層 Draft PR #319 の `CoinItemCodec.DENOMINATION_KEY` は `simplecoins:coin_denomination`。正規のPaper通貨は `minecraft:paper`・整数PDC・`minecraft:item_model=simplecoins:coin_<額面>` を持つ。
- よって今のまま新現物貨幣を保存走査へ投入すると、決算がその額面を無視する経路がある。新しい9額面を使った実本番決算やJE/BEユーザー操作の通過は未確認。

## Proposed integration contract（未Confirmed）

- Paper現行Codecの `coin_denomination` と整数額面を保存走査で検証し、数量×額面として計上する。表示名で金額を推定しない。
- 現行Codecの `item_model` が欠損・不一致、素材不正、額面不正、現行キーと従来キーの重複では `IOException` として決算を中止し、未確定な合計を公開しない。
- 従来 `simplecoins:denomination` を引き続き会計対象として扱うか、未知/旧型として移行・拒否するかは別途正式な互換性判断が必要。技術候補の回帰試験では既存Domain挙動を変更しない。
- 入れ子シュルカーやバンドル、直接プレイヤー保持、BlockEntity等で同一価値を用いる。外部プラグイン/ライブWorldとの二重計上防止は別Work。

## Verification boundary

- Regression: `StoredItemValueCoinCodecParityTest` の現行PDC/正しいmodel/モデル欠損/材質不正/重複キー/入れ子を検証する。
- JUnit Green、Paper 26.2 JE/BE、CAT Process Closure / Model Conformance / Implementation Conformance、正式なキー互換性決定はそれぞれ独立の証拠を要する。
- 本ファイルは既存コードと設計正本の矛盾を記録する候補であって、決定・Confirmed・デプロイ許可ではない。

## 対象

| Ref | Aspect |
| --- | --- |
| simplecoins.coin.paper | Paper実体のPDC額面・ItemModel・PAPER素材 |
| simplecoins.settlement.stored | NBTの保存済み現物を集計する金額抽出関数 |
| simplecoins.settlement.nested | バンドル・シュルカー内の保存現物にも適用される再帰計上 |

## 規則

以下は既存実装・設計出典から抽出した**未承認の候補**であり、Confirmedの意味論を宣言しない。

| ID | 条件 | 要求 | 検証 |
| --- | --- | --- | --- |
| coin.pdc.active | Paper Codecが出力したPAPER現物を保存して読み取る | 現行coin_denominationと正規item_modelから額面×数量を計上し、0扱いしない候補 | 正規PDCの100額面3枚、5額面2枚の入れ子走査 |
| coin.pdc.invalid | 現行PDCがあるが素材・型・額面・item_modelが不正 | 正常額面や普通紙として扱わず、走査全体を失敗させる候補 | 不正モデル/欠損/材質/額面/型のIOException |
| coin.pdc.conflict | 現行キーと従来キー、または財布タグが同一物理スタックに併存 | 選択的な0扱い・恣意的な片方の計上を拒否する候補 | PDCキー競合のIOException |
| coin.pdc.legacy | 従来denominationのみを保存した現物を読み取る | 既存互換挙動を変えず、移行・拒否の正式方針は未決定と明記する候補 | 既存StoredItemValueTest回帰と互換性判断の別管理 |
