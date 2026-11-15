# 開発補助資料の入口

このディレクトリは、SimpleCoinsの**検証手順・補助的なCAT Draft/Candidate・実験/証拠**を収めます。製品の確定要求そのものは[Driveの製品仕様](https://drive.google.com/file/d/1lLgAP9zAf8e0_Il8Mdh4HGXNr2aJ4ShC/view)、1.0.0の受入条件は[Drive 05](https://drive.google.com/file/d/1fSsHEn1tFX_DvnzxDK3t5FjDrIuvMrnt/view)、現在の作業順は[Issue #423](https://github.com/surumeneco/SimpleCoins/issues/423)が所有します。

## 読む入口

| 内容 | 場所 | 意味 |
| --- | --- | --- |
| 機能別・故障注入・実機受入の手順 | [verification/](verification/README.md) | 入力、環境、期待結果、証拠採取。合格記録とは別 |
| 既存コード由来のCAT草案・候補、補助調査 | [cat/](cat/README.md) | Candidate/Draftを再解釈・確認するための資料 |
| 規範CATの作業分割・候補とその出典 | [../Lifecycle/](../Lifecycle/README.md) | 各Workのstage/許可path/Gateの所有権に従う |
| 検証用のスクリプト | [verification/](verification/) | 動作は実行した対象HEAD、コマンド、ログに結び付ける |

## 更新する際の分担

- 機能の意味・運用要件・統計式・決算条件は、対応するDrive正本（01/02A/02B/03）を変更する。ここへ別の規範仕様を複製しない。
- 検証文書は**再現手順と判定条件**を保ち、テストの成功・失敗・履歴はIssue/PR/Actionsなどの個別証拠で管理する。
- 過去のTaskQueue ID、Phase名、旧PR、固定ソースSHAは**その資料が作られた当時の来歴**であり、現在の実装・責務を示さない。現HEADと照合するまで「現行」と扱わない。
- CATのDraft/Candidateを本フォルダの編集でConfirmedにしたり、CI成功をCAT正式Gateや実機受入に読み替えたりしない。

[リポジトリREADME](../README.md)に戻る。
