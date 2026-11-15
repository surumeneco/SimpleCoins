# CAT補助資料の読み方

このフォルダは、既存のコードに対する観測・検証案・形式仕様の**Draft/Candidate**を保持します。正式なCAT-TDDのフロー、役割、入力・出力、生成物の所有者、規範承認とGateは[上流CAT-TDD](https://github.com/surumeneco/CAT-TDD)が決めます。

## 資料群

- [drafts/](drafts/)：Paper独立受付ガードやBAN等の草案。編集時は個別のfront matter、参照SHA、決定根拠を確認。
- `phase5-*`：保存済み資産、BlockLocker、BAN、WCL、WorldScopeの観測・候補。
- `phase6-*`：永続化、同時書込、journal、キャッシュ公開境界の観測・候補。
- `phase7-*`：統計、発行、JE/BEパック・URLの観測・候補。

候補の一部は[Lifecycle/CAT/Candidates/](../../Lifecycle/CAT/Candidates/)に別の作業で作られています。**同名・近似内容でも正式な統合決定なしに互いの意味やConfirmed状態を推定しない**こと。参照重複は、必要な出典を残し、更新対象の正本を特定して整理します。

## 正本・成功の扱い

1. 製品の要求は[Drive仕様](https://drive.google.com/file/d/1lLgAP9zAf8e0_Il8Mdh4HGXNr2aJ4ShC/view)。CAT候補は仕様を勝手に変更しない。
2. `status: draft` / `candidate` と、観測した単体試験Green・CI成功・固定blob SHAは別々の事実である。
3. 仕様のConfirmed昇格は有効な規範決定にのみ基づく。WorkのGatesや生成テストの実行結果は正規CATスクリプト所有とし、このREADMEでは更新しない。
4. Phase番号は歴史的な領域識別であり、現在の作業優先度は[Issue #423](https://github.com/surumeneco/SimpleCoins/issues/423)を参照する。

[docs/README](../README.md)に戻る。
