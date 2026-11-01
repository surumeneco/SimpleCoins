# Phase 5: 保存済み全資産sourceのPaper受入手順

**状態：未実行。** TASK-20261101-003 / PR #9の保存source検証計画。規範要求は `forGPT/XPlayServer/OriginalPlugins/SimpleCoins/02B_データ収集・決算処理.md`、対応する未確定のCAT Draftは `docs/cat/phase5-authoritative-source-coverage.md`、`phase5-source-set-closure.md`、`phase5-legacy-terrain-block-entities-*.md`。既存のJUnit Greenを、全world網羅・loaded memoryとの同一時点性・決算公開の許可に拡張しない。

## 前提と証拠

- 本番ワールドのregion/entity/playerdataを編集しない。隔離したPaper 26.2検証サーバーと使い捨てMultiverse worldを使い、開始前にJAR/commit SHA、Paper・Multiverse版、world catalog、WorldScope、playerdata正本、対象保存パスのmanifestを記録する。
- 権威world一覧を**独立したPaper/Multiverse実体**から取得し、scannerへ渡すcallerのroot一覧と照合する。callerが自己申告したrootの走査成功だけではworld網羅を証明できない。
- 旧 `region` / `entities`、`DIM-1` / `DIM1`、26.x `dimensions/<namespace>/<dimension>/region|entities`、旧 `playerdata` と新 `players/data` を区別する。world除外時もworld非依存playerdataは対象とする。
- 保存ファイルの改変・破損・ハードリンク作成は**サーバー停止中のコピー**に限定する。稼働中のファイルを書換えてクラッシュを誘発することを実機試験の必須条件にしない。

## 受入マトリクス

| ID / Draft | 入力・操作 | 合格条件 |
| --- | --- | --- |
| P5-SOURCE-01 / SC-COVERAGE-01 | Paper側の権威worldが2つ、callerのroot一覧は1つだけ | world網羅の証拠なし。成功した全件決算として公開しない |
| P5-SOURCE-02 / SC-COVERAGE-02 | terrainと保存Entityが別source。片方の調査を省略 | Entityが0件と確定した扱いにせず、未確認として拒否 |
| P5-SOURCE-03 / SC-COVERAGE-03 | 同じUUIDの旧/新playerdata候補が併存 | 任意の一方を選択せず、権威衝突を検出 |
| P5-SOURCE-04 / SC-COVERAGE-04 | WorldScopeでworld固有sourceを除外、playerdataは存在 | 除外worldのterrain/entityは計上せず、world非依存playerdataは維持 |
| P5-SOURCE-05 / SC-ROOT-01,03,04 | world rootの置換、物理alias、symlink、欠損をそれぞれ検証 | visitor前または閉包再検証時に拒否し、成功Reportを返さない |
| P5-SOURCE-06 / SC-LEGACY-BLOCK-01,02,03,06 | 旧terrain `TileEntities`非空・不正型、`Level`非compound | 未対応の旧BlockEntityを0資産として通過させず、visitor前に拒否 |
| P5-SOURCE-07 / SC-COVERAGE-05 | 保存source一覧は前後一致するが、loaded側に同じ物理資産が存在 | 保存走査の成功だけで決算を公開しない。二重計上防止と同一時点性を別に検証 |
| P5-SOURCE-08 / SC-COVERAGE-06 | 権威world一覧・保存source・playerdataを全件列挙し、無変更で読取完了 | 保存source網羅の候補証拠を得るが、BAN/owner・loaded memory・決算全体の成功とは区別 |

## 結果の扱い

各ケースを **not-run / pass / fail / blocked** で記録し、対象commit、world rootとdimension、manifest件数、NBT種別、物理ファイル同一性、callback回数、エラー、公開世代の前後を対応付ける。未知source・読取失敗・途中変更は部分集計を成功として公開しない。

`WorldStorageDiscovery.discover` は渡されたrootの探索器であり、権威world catalogを取得する実装ではない。`WorldSnapshotTraversal` のファイル変化検出はatomic snapshotでもPaper loaded/disk調停でもない。legacy TileEntitiesの規範決定、正式CAT Process/PI/TCE・機械Gate、Paper実機、全件決算は**未完了**。本手順書自体を受入証拠に数えない。
