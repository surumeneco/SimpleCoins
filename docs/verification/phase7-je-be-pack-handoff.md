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

   Treat any nonzero script exit as a failed gate; the helper does not certify real-client delivery. Record the two hashes, HTTP result and the tested artifact identity, not the secret URL. A successful GitHub CI build does not prove external reachability.
3. Join with a fresh JE client and verify resource-pack acceptance, all nine denominations, configured display names, item models and wallet appearance. Record client prompt/acceptance, screenshots and server log.
4. Join with a fresh Bedrock client via Geyser/Floodgate and verify all nine denominations, mappings, textures and wallet UI. Record whether custom content is enabled and whether the client actually downloaded the Bedrock pack.
5. Rename one denomination and replace its image in a disposable copy of the configuration; regenerate packs and repeat both client checks. Confirm stable PDC IDs and numerical denominations.

## Nexo present — delegated Java pack

1. Restart with Nexo installed. Verify SimpleCoins does not send its own Java pack, and that Nexo owns Java pack generation/hosting. Check duplicate resource-pack prompts do not occur.
2. Verify Geyser's Bedrock mapping/pack comes from an explicitly configured conversion/delegation path (e.g. Scaffolding). **Do not infer that Nexo's Java pack is automatically converted by Geyser.** If no verified conversion exists, mark BE as blocked/not-run rather than passed.
3. Repeat JE and BE denomination/model checks, preserving the same currency PDC identities and amounts as the Nexo-absent case.

## Negative cases and release gate

- Blank public URL: no JE pack request; operator warning remains visible.
- Invalid public URL (e.g. hostless HTTPS, unsupported scheme, embedded credentials, fragment): no client request and a diagnostic log.
- Public URL points to an old ZIP: compare SHA-1 to the generated ZIP; mismatch is a failed deployment gate even if the client displays cached textures.
- Interrupted/modified managed Geyser resources: startup must not silently overwrite unowned or modified files.

Record pass/fail/not-run **per JE and BE client**, Nexo state, tested commit and actual artifacts. Do not mark the leaf complete until relevant live checks and the independent offline give/admin command acceptance are satisfied.
