# Phase 7: 発行台帳の停止時オフライン診断

本手順は **SimpleCoins 1.0.0 の永続予約台帳の構造診断**であり、Paper実機での引渡し一回性やplayerdata受領記録の整合を証明しない。CAT Candidate: `docs/cat/phase7-issuance-journal-offline-verification.md`。

## 実行条件

1. Paperサーバーを正常停止し、`plugins/SimpleCoins/issuance/` と該当ワールドの `playerdata` を同一時点のバックアップとして保全する。**稼働中のディレクトリに対しては実行しない**。読み取り対象は停止時コピーのみ。
2. 読み取り専用コピーの `issuance/` ディレクトリを引数に渡す。スクリプトは元データを変更せず、プレイヤーUUID・予約ID・発行者を標準出力に含めない。

```sh
python3 docs/verification/verify-issuance-journal.py /path/to/stopped-copy/plugins/SimpleCoins/issuance
python3 docs/verification/test-verify-issuance-journal.py
```

## 終了コードと判断

| 終了コード | JSON status | 意味 |
| --- | --- | --- |
| `0` | `ok` | 台帳記録のチェックサム・連番・head・SCG1/SCG2形式が局所的に整合 |
| `1` | `warning` | headが末尾より古い、または一時ファイルが残存。自動修復しない |
| `2` | `invalid` | 欠損、チェックサム・形式・額面・UUID・連番等に異常。発行再試行を行わず、同一時点バックアップを保全して調査 |

`ok` でも `pending` は未配布額の総額ではなく未完了予約**件数**。台帳内の `delivered` はplayerdata受領PDCと独立した情報であり、`pending=0` だけで二重配布防止や資産総額を証明しない。

## 未検証と制限

- Paperの `saveData` 完了、クラッシュ直後のインベントリとPDCの同時永続化、ログイン/切断/満杯時の分割配布は別の実機試験が必要。
- 本スクリプトは各ファイルのread中変更を一部検知するが、複数ファイルを同一瞬間の原子的スナップショットとして読むものではない。必ず停止時コピーを使う。
- Java/Bedrock、Geyser、Nexoの表示・配布とは無関係。対応する実機受入は `docs/verification/phase7-je-be-nexo-acceptance.md` を参照する。
- 診断結果を理由に `issuance/head` や `*.grant` を手動で書き換えない。修復判断には台帳とplayerdataの同時点バックアップ、実際の障害ログが必要。
