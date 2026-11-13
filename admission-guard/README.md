# Independent admission guard — technical candidate

This guard is a separate JavaPlugin that loads at `STARTUP`, before the
SimpleCoins accounting plugin (`POSTWORLD`). It denies connections when the
trusted SimpleCoins `AdmissionPermit` service is absent, its provider plugin is
disabled, it returns `false`, or it throws. The provider only returns `true`
when the complete, verified settlement snapshot **and** durable journal service
are both running; neither is installed in the present bootstrap.

This guard alone cannot protect a server if the guard JAR itself is absent,
if plugin loading fails before it registers, or if the JVM dies. An operator
must also configure an external server-start preflight to verify BOTH JARs
are present before the server is exposed; a separate proxy or firewall can
supply an additional independent admission barrier. Tests in an isolated Paper
fixture are not evidence of a full-production settlement or recovery.

Do not deploy this candidate to the production server without an approved
startup/preflight plan, real Paper checks, and full journaling acceptance.
