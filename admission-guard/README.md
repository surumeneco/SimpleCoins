# Independent admission guard — technical candidate

This guard is a separate JavaPlugin that loads at `STARTUP`, before the
SimpleCoins accounting plugin (`POSTWORLD`). It denies connections when the
trusted SimpleCoins `AdmissionPermit` service is absent, its provider plugin is
disabled, it returns `false`, or it throws. The provider only returns `true`
when the settlement snapshot is published and the ordinary accounting task is
running. The product startup path performs settlement before publishing the
permit; missing required providers, diagnostic-only mode and failure remain
closed. See the [operator setup](../README.md#導入運用手順受入用正式リリース前).

This guard alone cannot protect a server if the guard JAR itself is absent,
if plugin loading fails before it registers, or if the JVM dies. An operator
must also configure an external server-start preflight to verify BOTH JARs
are present before the server is exposed; a separate proxy or firewall can
supply an additional independent admission barrier. Tests in an isolated Paper
fixture are not evidence of a full-production settlement or recovery.

Do not deploy this candidate to the production server without an approved
startup/preflight plan, real Paper checks, and the product acceptance defined by the current release plan.
