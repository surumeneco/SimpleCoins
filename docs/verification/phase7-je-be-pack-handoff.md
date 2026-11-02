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

   **Verifier evidence boundary:** byte identity and `unzip -tq` integrity do not validate ZIP member names. A disposable localhost probe against the exact verifier source accepted both `../escape.txt` and `/absolute.txt` entries when the hosted and generated ZIP bytes matched. Record ZIP entry-name safety as **not verified**, not as a passed deployment gate. The stricter rejection policy is a separate CAT Draft in [PR #70](https://github.com/surumeneco/SimpleCoins/pull/70), not a Confirmed requirement or real JE/BE client result.

   The helper's local HTTP positive/negative smoke tests can be run with `python3 docs/verification/test-verify-je-pack.py` (15 deterministic cases, including missing/directory local artifact rejection; no real server required). This verifies the verifier only; it does **not** replace the external hosted URL or JE/BE client checks.

   Treat any nonzero script exit as a failed gate; the helper does not certify real-client delivery. Record the two hashes, HTTP result and the tested artifact identity, not the secret URL. A successful GitHub CI build does not prove external reachability.
3. Join with a fresh JE client and verify resource-pack acceptance, all nine denominations, configured display names, item models and wallet appearance. Record client prompt/acceptance, screenshots and server log.
4. Join with a fresh Bedrock client via Geyser/Floodgate and verify all nine denominations, mappings, textures and wallet UI. Record whether custom content is enabled and whether the client actually downloaded the Bedrock pack.
5. Rename one denomination and replace its image in a disposable copy of the configuration; regenerate packs and repeat both client checks. Confirm stable PDC IDs and numerical denominations.

## Nexo present — delegated Java pack

1. Restart with Nexo installed. Verify SimpleCoins does not send its own Java pack, and that Nexo owns Java pack generation/hosting. Check duplicate resource-pack prompts do not occur.
2. Verify Geyser's Bedrock mapping/pack comes from an explicitly configured conversion/delegation path (e.g. Scaffolding). **Do not infer that Nexo's Java pack is automatically converted by Geyser.** If no verified conversion exists, mark BE as blocked/not-run rather than passed.
3. Repeat JE and BE denomination/model checks, preserving the same currency PDC identities and amounts as the Nexo-absent case.

## Admin command and offline grant acceptance (disposable server only)

The current `plugin.yml` exposes `/simplecoins` (alias `/sc`) with `simplecoins.admin` permission. `CoinCommand` implements `give`; the read-only balance/dead-asset location and reason query requested by TASK-20261101-006 is **not implemented** in the inspected command handler. Do not mark the admin-query acceptance complete based on the issuance tests below.

1. On a disposable Paper 26.2 server, confirm a non-privileged sender cannot reserve or issue currency; grant `simplecoins.admin` to a test operator and record the command result. Do not use production playerdata or journal files.
2. Use an existing, previously joined test player who is currently offline. Run `/sc give <player-uuid> 100`. Record the reservation ID and issuance journal entry; before login, no physical coins should be delivered. On login, verify the exact total and persisted receipt; disconnect/reconnect and restart the disposable server, then verify no duplicate delivery.
3. With an online test player, run `/sc give <player-name> 50 3`. Verify exactly three denomination-50 coins are reserved and delivered, not a minimum-coin decomposition of 150. Repeat reconnect/restart checks.
4. Fill all 36 player storage slots before a reserved grant is delivered. Verify no coin is dropped or falsely acknowledged and the reservation remains pending; free space, trigger delivery again, and verify the total is neither lost nor duplicated. Record the saved playerdata receipt and journal state.
5. In a disposable copy only, inject a corrupt/missing journal record or an unconfirmed saved receipt, restart, and verify issuance fails closed rather than replaying an uncertain grant. Keep this separate from a successful delivery case; do not induce failure on production.

Record each case as pass/fail/not-run with the tested commit, Paper version, journal/receipt evidence and observed inventory totals. The command-source inspection is **not** a Paper execution result. The missing read-only admin query, real Paper failure injection, and JE/BE client acceptance remain open.

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
