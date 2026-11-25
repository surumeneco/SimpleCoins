# SimpleCoins 1.0.0 — JE/BE resource-pack delivery acceptance handoff

Status: **verification protocol, not an execution receipt or release approval**. Current run ownership and remaining acceptance are [Issue #423](https://github.com/surumeneco/SimpleCoins/issues/423) A17/H01 and [Issue #6](https://github.com/surumeneco/SimpleCoins/issues/6); retired TaskQueue identifiers are historical only. Individual test run results must be recorded against the exact deployed HEAD/JAR/client.

## Environment and immutable evidence

Record the deployed SimpleCoins JAR SHA-256, Paper/Geyser/Floodgate/Nexo/Scaffolding versions, SimpleCoins commit SHA, **the externally hosted Java pack URL and the server configuration actually requesting it, if present** (redact signed query tokens), server log and client versions. With Nexo absent, `resource-pack.public-url` configures the URL for the generated Java ZIP. SimpleCoins requests that pack on Java login using the ZIP SHA-1 and its own stable pack ID. Host the ZIP at that URL before enabling the request; SimpleCoins does not include an HTTP hosting server. Verify actual hosting and client status rather than inferring successful delivery. Use a disposable test server with a fresh player profile; never change production resources to run this check.

## Nexo absent — SimpleCoins-owned assets

1. Start Paper 26.2 with SimpleCoins and Geyser/Floodgate, without Nexo. Check `plugins/SimpleCoins/SimpleCoins-java.zip`, `plugins/SimpleCoins/geyser/SimpleCoins.mcpack` and `plugins/SimpleCoins/geyser/simplecoins.json` exist. Check Geyser's managed `packs/simplecoins-generated.mcpack` and `custom_mappings/simplecoins-generated.json` are installed.
2. Host the **generated** Java ZIP at a verified external HTTP(S) URL and set `resource-pack.public-url` to it. Restart SimpleCoins and verify `SIMPLECOINS_JAVA_PACK_DELIVERY_CONFIGURED`. Verify `SIMPLECOINS_JAVA_PACK_REQUESTED` and the corresponding `SIMPLECOINS_JAVA_PACK_STATUS` on JE login. The status must be SUCCESSFULLY_LOADED for client acceptance; Geyser/Floodgate connections must not receive this Java request. From a separate network, download it with the **exact** URL (including any signed query) and compare the bytes, SHA-1 and ZIP integrity against the server-generated archive. Do not paste a signed URL into the report or a logged command. On a disposable Linux verification host with a copy of the generated archive, run:

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

The 05 G2 contract excludes real Nexo/Scaffolding environment assurance from 1.0.0. The optional SimpleCoins-side delegation and duplicate delivery suppression remain required. The current implementation stages the generated Java ZIP at `Nexo/pack/external_packs/simplecoins-generated.zip`, requests `nexo reload pack` after changed staging, and removes only owned Geyser files. Real Nexo pack merge/hosting, Scaffolding conversion and rendered clients are unverified, not release blockers under that contract. If voluntarily testing the delegated path, record actual Nexo versions, pack ownership and Geyser conversion configuration; do not infer success from staging alone.

## Admin command and offline grant acceptance (disposable server only)

**Current interface preflight:** verify the tested JAR descriptor and the README against the target commit. The implemented aliases are `/simplecoins` and `/sc`; the declared nodes are `simplecoins.give` for issuance and `simplecoins.admin` for queries (OP defaults). Confirm settlement has published and the guard is active before admitting the disposable test player. The prior missing-command state is historical. Test actual physical inventory, reservation state and query output; do not rely on command success text alone.

1. Record the effective alias and permission nodes from the tested JAR. Verify an unauthorized sender cannot reserve/issue and an authorized test operator can. Do not use production playerdata or journal files.
2. Once the approved alias/command is implemented, use a previously joined test player who is currently offline; execute the **implemented amount-based** `give` form with that player's verified identity and amount 100. Record reservation ID and journal entry; before login, no physical coins should be delivered. On login, verify the exact total and persisted receipt; disconnect/reconnect and restart the disposable server, then verify no duplicate delivery.
3. Once implemented, use an online test player and the **item-name plus count** `give` form to give three coins of denomination 50, using the actual accepted item name. Verify exactly three such coins are issued (not amount-based minimum-coin decomposition of 150), then repeat reconnect/restart checks. Do not assume `50` is a valid item-name token.
4. Fill all 36 player storage slots before a reserved grant is delivered. Verify no coin is dropped or falsely acknowledged and the reservation remains pending; free space, trigger delivery again, and verify the total is neither lost nor duplicated. Record the saved playerdata receipt and journal state.
5. In a disposable copy only, inject a corrupt/missing journal record or an unconfirmed saved receipt, restart, and verify issuance fails closed rather than replaying an uncertain grant. Keep this separate from a successful delivery case; do not induce failure on production.

Record each case as pass/fail/not-run with the tested commit, Paper version, journal/receipt evidence and observed inventory totals. The command-source inspection is **not** a Paper execution result. Refer to #423 for current run evidence and remaining acceptance; this protocol does not assert missing implementation or completion.

## 管理照会の受入マトリクス

以下は実機試験結果ではなく、管理照会をPaperで検証する条件である。`/sc stats`、`/sc balance <player>`、`/sc dead [page]` と `simplecoins.admin` の現行実装・対象JARを確認して実施する。

| Vector | 隔離Paperでの入力・操作 | 必須の観測結果 |
| --- | --- | --- |
| ADMIN-QUERY-01 | 管理権限のない送信者と権限付与済み送信者が同じ照会を試す | 無権限側に金額・UUID・死蔵所在を開示しない。権限付与後だけ照会を許可 |
| ADMIN-QUERY-02 | 有効UUIDの保有額、未知UUID、snapshot未完了の各状態を照会 | 検証済みA[u]と集計時点を区別。未知・未集計を確定0と表示しない |
| ADMIN-QUERY-03 | 隔離データでP=100、F=20、D=30を準備して総額を照会 | 通常総額T=120とD=30を別項表示し、150を通常総額にしない |
| ADMIN-QUERY-04 | BAN中UUIDの保護チェストとworld非依存の死蔵保管先を作る | 金額・所有者UUID・理由・保管種別・実座標または論理所在を正しく返す |
| ADMIN-QUERY-05 | 必須BAN sourceまたは保存sourceの読取を失敗させ、所在索引を未確定にする | 空一覧や0額を正常完了として公開せず、未確定状態を識別可能にする |
| ADMIN-QUERY-06 | worldを統計OFFにし、world非依存playerdata/エンダーチェストへ貨幣を配置 | world固有資産は除外し、world非依存資産は照会対象に維持する |

各行の結果は`not-run / pass / fail / blocked`とし、検証時のcommit、権限設定、snapshot世代、BAN/owner根拠、WorldScope、コマンド出力を対応付ける。未実行のケースをpassにしない。

## Negative cases and release gate

- Missing external Java pack hosting or server resource-pack request: no JE pack delivery; do not mark the JE client check `pass` on generated ZIP alone.
- Invalid or obsolete Java resource-pack URL in the **actual serving configuration** (e.g. invalid HTTPS/redirect, wrong ZIP): do not claim client delivery; require reachable verified bytes and observable client result. Use the actual `resource-pack.public-url` setting; missing or invalid configuration sends no Java pack request.
- Public URL points to an old ZIP: compare SHA-1 to the generated ZIP; mismatch is a failed deployment gate even if the client displays cached textures.
- Interrupted/modified managed Geyser resources: startup must not silently overwrite unowned or modified files.

Record pass/fail/not-run **per JE and BE client**, Nexo state, tested commit and actual artifacts. Apply the G2 exclusion above; actual non-Nexo rendering, external hosting and authenticated Floodgate/real environment acceptance remain separate from headless protocol CI.
