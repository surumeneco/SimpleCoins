# SimpleCoins 1.0.0 — JE/BE resource-pack delivery acceptance handoff

Status: **not-run on a real Paper 26.2 server / JE or BE client**. Leaf TASK-20261101-006. This is an executable verification protocol, not evidence of passing.

## Environment and immutable evidence

Record the deployed SimpleCoins JAR SHA-256, Paper/Geyser/Floodgate/Nexo/Scaffolding versions, SimpleCoins commit SHA, exact `resource-pack.public-url` (redact signed query tokens), server log and test client versions. Use a disposable test server with a fresh player profile; never change production resources to run this check.

## Nexo absent — SimpleCoins-owned assets

1. Start Paper 26.2 with SimpleCoins and Geyser/Floodgate, without Nexo. Check `plugins/SimpleCoins/SimpleCoins-java.zip`, `plugins/SimpleCoins/geyser/SimpleCoins.mcpack` and `plugins/SimpleCoins/geyser/simplecoins.json` exist. Check Geyser's managed `packs/simplecoins-generated.mcpack` and `custom_mappings/simplecoins-generated.json` are installed.
2. Host the **generated** Java ZIP at the configured HTTP(S) URL. From a separate network, download it with the **exact** configured URL (including any signed query) and compare the bytes, SHA-1 and ZIP integrity against the server-generated archive. Do not paste a signed URL into the report or a logged command. On a disposable Linux verification host with a copy of the generated archive, run:

   ```bash
   bash docs/verification/verify-je-pack.sh plugins/SimpleCoins/SimpleCoins-java.zip
   ```

   The script prompts for the exact URL without echoing it, restricts redirects to HTTP(S), compares the downloaded bytes, checks ZIP integrity and prints both SHA-1 values. It requires `curl`, `cmp`, `sha1sum` and `unzip`. Copy both this script and the generated ZIP to the disposable verification host; the relative paths above are illustrative.

   The helper's local HTTP positive/negative smoke tests can be run with `python3 docs/verification/test-verify-je-pack.py` (15 deterministic cases, including missing/directory local artifact rejection; no real server required). This verifies the verifier only; it does **not** replace the external hosted URL or JE/BE client checks.

   Treat any nonzero script exit as a failed gate; the helper does not certify real-client delivery. Record the two hashes, HTTP result and the tested artifact identity, not the secret URL. A successful GitHub CI build does not prove external reachability.
3. Join with a fresh JE client and verify resource-pack acceptance, all nine denominations, configured display names, item models and wallet appearance. Record client prompt/acceptance, screenshots and server log.
4. Join with a fresh Bedrock client via Geyser/Floodgate and verify all nine denominations, mappings, textures and wallet UI. Record whether custom content is enabled and whether the client actually downloaded the Bedrock pack.
5. Rename one denomination and replace its image in a disposable copy of the configuration; regenerate packs and repeat both client checks. Confirm stable PDC IDs and numerical denominations.

## Nexo present — delegated Java pack

1. Restart with Nexo installed. Verify SimpleCoins does not send its own Java pack, and that Nexo owns Java pack generation/hosting. Check duplicate resource-pack prompts do not occur.
2. Verify Geyser's Bedrock mapping/pack comes from an explicitly configured conversion/delegation path (e.g. Scaffolding). **Do not infer that Nexo's Java pack is automatically converted by Geyser.** If no verified conversion exists, mark BE as blocked/not-run rather than passed.
3. Repeat JE and BE denomination/model checks, preserving the same currency PDC identities and amounts as the Nexo-absent case.

### 実機配布の判定マトリクス

上記の具体的な操作を以下のケース単位で記録する。**起動成功・オフラインZIP検証・CI成功だけではクライアント受信成功としない**。

| Case | Nexo | 経路 | 合格の観測条件 |
| --- | --- | --- | --- |
| DIST-01 | なし | JE | HTTP(S)の生成ZIPと公開バイトが一致し、9額面のモデル・名称・テクスチャが実クライアントで表示される |
| DIST-02 | なし | BE / Geyser-Spigot | Geyser custom content有効、生成mcpackとv2 mappingの実配布、9額面の表示が一致する |
| DIST-03 | なし | JE + BE | 同じ額面と財布GUIを両クライアントで操作し、識別子・表示・スタックの差異を記録する |
| DIST-04 | あり | JE / Nexo | SimpleCoins独自ZIPが送られないことを確認し、Nexo側の定義・配布が検証されるまではBLOCKED |
| DIST-05 | あり | BE / Geyser | Nexo→BEの変換/マッピングを別途実証し、自己管理Geyserファイルが他者の資産を上書きしない |
| DIST-06 | なし | BE / Geyserなし | BE経路なし。対象外をPASSに数えない |

各行に `PASS / FAIL / BLOCKED / NOT-RUN`、JAR SHA-256、Paper/Geyser/Nexo版、JE/BEクライアント版、配布物SHA、設定、関連ログ、実表示を対応付ける。個人識別情報・署名付きURLの秘密値は記録しない。

## Admin command and offline grant acceptance (disposable server only)

The current `plugin.yml` exposes `/simplecoins` (alias `/sc`) with `simplecoins.admin` permission. `CoinCommand` implements `give`; the read-only balance/dead-asset location and reason query requested by TASK-20261101-006 is **not implemented** in the inspected command handler. Do not mark admin-query acceptance complete based on issuance tests.

Use a disposable Paper 26.2-compatible server, a previously joined known test player A, privileged sender B and unprivileged sender C. Before each case, preserve a **same-stop** backup of `plugins/SimpleCoins/issuance/` and the test world's `playerdata/`, and record physical coin amount, pending grant sequence, `delivered`, and saved PDC `simplecoins:grant_completed`, `simplecoins:grant_active`, `simplecoins:grant_paid`. Test data must not be real player assets. Keep wallet issuance separate from the durable money-grant path.

| Case | 操作 | 合格の観測条件 |
| --- | --- | --- |
| GRANT-01 | 無権限Cが `/sc give A 100` と補完を実行 | 拒否され、台帳連番・物理コインが変化しない |
| GRANT-02 | Aがオフライン中にBが `/sc give <known-player-uuid> 100` | 一意の予約を記録し、ログイン前の物理額は不変。次回ログインで100相当を受領し台帳とPDCが整合 |
| GRANT-03 | GRANT-02完了後、退出→正常停止→再起動→再ログイン | 同じ予約の再配布がなく、完了受領値と物理額が維持される |
| GRANT-04 | 36収納スロット満杯で予約し、ログイン後に空きを作る | 未配布予約が消えず、分割受領 `active/paid` が整合し、再起動後も総額が予約を超えない |
| GRANT-05 | 未知/曖昧な名前、オフラインへの `wallet 1` または額面 `50 3` | 不正・未対応経路は拒否され、副作用がない。wallet/額面指定はオンライン限定 |
| GRANT-06 | オンラインAへ `/sc give A 50 3`、別途 `/sc give A wallet 1` | 額面50の3枚を予約・受領。walletは新規IDの直接配布であり、オフライン予約の一回性と混同しない |
| GRANT-07 | Aへ連続した異なる金額の予約、再接続・再起動 | 各予約が順序どおり一度だけ反映され、未配布の古い予約を受領連番が飛び越さない |
| GRANT-08 | **停止時の隔離コピーだけ**で台帳欠損/受領巻戻りを注入 | 安全停止または明示的な不整合検出。故障注入不可ならNOT-RUN、本番データを編集しない |

`GRANT-04` の満杯時は `inserted == 0` なら保留される設計だが、再試行時刻は実測する。`GRANT-08` は正常再起動試験と独立した障害復旧試験。いずれも、予約額・実物理額・PDC受領値・台帳状態・ログを同時に照合する。発行永続化エラー、保存playerdata候補の不一致、PDC/台帳不整合が起きた場合は再発行で隠さずFAILとし、停止時バックアップとログを保全する。

停止後の同一時点コピーで `docs/verification/verify-issuance-journal.py` を実行する場合は、PR #116 の `docs/verification/phase7-issuance-journal-offline-check.md` に従う。`ok` は台帳局所整合のみで、Paperの `saveData` とインベントリの永続化を証明しない。各Caseは `PASS / FAIL / BLOCKED / NOT-RUN` と実際のJAR SHA-256・Paper版・再起動/ログ・物理額で記録する。

## 管理照会の受入マトリクス（CAT Draft・未実行）

現行の`CoinCommand`には照会コマンドがないため、以下は実機試験済みの結果ではなく、`docs/cat/phase7-admin-asset-query.md`の既存ベクトルをPaperで検証するための条件である。コマンド名・ページング・未確定時の表示形式は規範未決定のまま維持する。

| Vector | 隔離Paperでの入力・操作 | 必須の観測結果 |
| --- | --- | --- |
| ADMIN-QUERY-01 | 管理権限のない送信者と権限付与済み送信者が同じ照会を試す | 無権限側に金額・UUID・死蔵所在を開示しない。権限付与後だけ照会を許可 |
| ADMIN-QUERY-02 | 有効UUIDの保有額、未知UUID、snapshot未完了の各状態を照会 | 検証済みA[u]と集計時点を区別。未知・未集計を確定0と表示しない |
| ADMIN-QUERY-03 | 隔離データでP=100、F=20、D=30を準備して総額を照会 | 通常総額T=120とD=30を別項表示し、150を通常総額にしない |
| ADMIN-QUERY-04 | BAN中UUIDの保護チェストとworld非依存の死蔵保管先を作る | 金額・所有者UUID・理由・保管種別・実座標または論理所在を正しく返す |
| ADMIN-QUERY-05 | 必須BAN sourceまたは保存sourceの読取を失敗させ、所在索引を未確定にする | 空一覧や0額を正常完了として公開せず、未確定状態を識別可能にする |
| ADMIN-QUERY-06 | worldを統計OFFにし、world非依存playerdata/エンダーチェストへ貨幣を配置 | world固有資産は除外し、world非依存資産は照会対象に維持する |

各行の結果は`not-run / pass / fail / blocked`とし、検証時のcommit、権限設定、snapshot世代、BAN/owner根拠、WorldScope、コマンド出力を対応付ける。未実装の段階でpassにしない。

## Negative cases and release gate

- Blank public URL: no JE pack request; operator warning remains visible.
- Invalid public URL (e.g. hostless HTTPS, unsupported scheme, embedded credentials, fragment): no client request and a diagnostic log.
- Public URL points to an old ZIP: compare SHA-1 to the generated ZIP; mismatch is a failed deployment gate even if the client displays cached textures.
- Interrupted/modified managed Geyser resources: startup must not silently overwrite unowned or modified files.

Record pass/fail/not-run **per JE and BE client**, Nexo state, tested commit and actual artifacts. Do not mark the leaf complete until relevant live checks and the independent offline give/admin command acceptance are satisfied.
