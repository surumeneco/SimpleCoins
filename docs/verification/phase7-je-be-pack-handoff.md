# SimpleCoins 1.0.0 — JE/BE resource-pack delivery acceptance handoff

Status: **verification protocol, not evidence of successful real Paper 26.2 or JE/BE client tests**. Current run ownership and remaining acceptance are [Issue #423](https://github.com/surumeneco/SimpleCoins/issues/423) A17/H01 and [Issue #6](https://github.com/surumeneco/SimpleCoins/issues/6); retired TaskQueue identifiers are historical only. Individual test run results must be recorded against the exact deployed HEAD/JAR/client.

## Environment and immutable evidence

Record the deployed SimpleCoins JAR SHA-256, Paper/Geyser/Floodgate/Nexo/Scaffolding versions, SimpleCoins commit SHA, **the externally hosted Java pack URL and the server configuration actually requesting it, if present** (redact signed query tokens), server log and client versions. The current `config.yml` has no `resource-pack.public-url` field; `SimpleCoinsPlugin` builds the JE ZIP but does not publish or request it for clients. Do not claim a product-provided public URL or a successful delivery before the actual hosting/serving path exists. Use a disposable test server with a fresh player profile; never change production resources to run this check.

### Offline Bedrock artifact structure audit (candidate)

On a disposable verification host, copy the **generated** `.mcpack` and Geyser mapping JSON as a pair and run `python3 tools/audit_bedrock_pack.py <pack.mcpack> <mapping.json>`. The [read-only auditor](../../tools/audit_bedrock_pack.py) checks ZIP entry integrity, manifest UUIDs, PNG signatures, all nine stable denomination IDs, atlas texture references, and Geyser v2 mapping/model/icon/stack-size consistency. It returns machine-readable `valid_structure` (exit 0) or `invalid` (exit 2). No files are changed.

**Exit 0 is not a client-delivery, publication-atomicity, or live Paper/Geyser Gate.** It does not decode full PNG pixels, establish that Geyser loaded these exact files, prove pack/mapping were atomically published, or verify JE/BE/Nexo UI behavior. The separate output-destination alias Red is [PR #149](https://github.com/surumeneco/SimpleCoins/pull/149); this verifier does not repair it. CAT remains Draft and the real-client cases below remain not-run.

## Nexo absent — SimpleCoins-owned assets

1. Start Paper 26.2 with SimpleCoins and Geyser/Floodgate, without Nexo. Check `plugins/SimpleCoins/SimpleCoins-java.zip`, `plugins/SimpleCoins/geyser/SimpleCoins.mcpack` and `plugins/SimpleCoins/geyser/simplecoins.json` exist. Check Geyser's managed `packs/simplecoins-generated.mcpack` and `custom_mappings/simplecoins-generated.json` are installed.
2. **After an actual Java pack distribution path is configured** (external HTTP(S) hosting and the server's real resource-pack request configuration, or a subsequently implemented equivalent), host the **generated** Java ZIP at that verified URL. This is a future acceptance action, not a capability already provided by the current SimpleCoins plugin. From a separate network, download it with the **exact** URL (including any signed query) and compare the bytes, SHA-1 and ZIP integrity against the server-generated archive. Do not paste a signed URL into the report or a logged command. On a disposable Linux verification host with a copy of the generated archive, run:

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

1. On a disposable server with an **implemented and verified Nexo integration**, restart with Nexo installed. Verify SimpleCoins does not generate/serve a duplicate pack and that the actual Nexo Java pack generation/hosting is connected. The current bootstrap merely suppresses SimpleCoins-owned pack generation when Nexo is detected; **it does not yet register SimpleCoins currency assets with Nexo**. If that integration is absent, record the acceptance case as `blocked`, not `pass`. Check duplicate resource-pack prompts only when a real pack request can occur.
2. Verify Geyser's Bedrock mapping/pack comes from an explicitly configured conversion/delegation path (e.g. Scaffolding). **Do not infer that Nexo's Java pack is automatically converted by Geyser.** If no verified conversion exists, mark BE as blocked/not-run rather than passed.
3. Repeat JE and BE denomination/model checks, preserving the same currency PDC identities and amounts as the Nexo-absent case.

## Admin command and offline grant acceptance (disposable server only)

**Current-implementation preflight:** at `develop@7dc4ffdc2f08900ad87aa8a440fb39805c931ace`, `src/main/resources/plugin.yml` registers **no commands or permission nodes**, there is **no `CoinCommand` source file**, and `SimpleCoinsPlugin` registers **no give/administration command executor or player-delivery coordinator**. It keeps admission default-deny. Therefore `/sc give` and the `simplecoins.admin` node in the historical steps below are **not executable product interfaces** on this HEAD; do not attempt to test them as implemented or count missing-command errors as monetary function tests. Names, aliases and nodes require verification against the eventual implemented descriptor and the [Drive 03 command specification](https://drive.google.com/file/d/1llx5bKVdrmqeSgRo3KdsB2jD5mDwXofE/view). The numbered cases are **future acceptance conditions only**, currently `blocked` pending product implementation and G0/G3 assurance.

1. **Only after the real command is registered**, record its effective alias and permission node from the tested JAR, then verify that an unauthorized sender cannot reserve/issue and an authorized test operator can. Do not assume `simplecoins.admin` is the implemented node. Do not use production playerdata or journal files.
2. Once the approved alias/command is implemented, use a previously joined test player who is currently offline; execute the **implemented amount-based** `give` form with that player's verified identity and amount 100. Record reservation ID and journal entry; before login, no physical coins should be delivered. On login, verify the exact total and persisted receipt; disconnect/reconnect and restart the disposable server, then verify no duplicate delivery.
3. Once implemented, use an online test player and the **item-name plus count** `give` form to give three coins of denomination 50, using the actual accepted item name. Verify exactly three such coins are issued (not amount-based minimum-coin decomposition of 150), then repeat reconnect/restart checks. Do not assume `50` is a valid item-name token.
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

- Missing external Java pack hosting or server resource-pack request: no JE pack delivery; do not mark the JE client check `pass` on generated ZIP alone.
- Invalid or obsolete Java resource-pack URL in the **actual serving configuration** (e.g. invalid HTTPS/redirect, wrong ZIP): do not claim client delivery; require reachable verified bytes and observable client result. No unimplemented SimpleCoins `public-url` setting or warning message is assumed.
- Public URL points to an old ZIP: compare SHA-1 to the generated ZIP; mismatch is a failed deployment gate even if the client displays cached textures.
- Interrupted/modified managed Geyser resources: startup must not silently overwrite unowned or modified files.

Record pass/fail/not-run **per JE and BE client**, Nexo state, tested commit and actual artifacts. Do not mark the leaf complete until relevant live checks and the independent offline give/admin command acceptance are satisfied.
