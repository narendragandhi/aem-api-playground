# `tags` and `users` depend on QueryBuilder response quirks

**Status:** partially works
**Affects:** `tags` (search), `users`, `groups` — all AEM versions
**Why:** both rely on `/bin/querybuilder.json`, which exists on all AEM 6.x
and Cloud Service instances, but response formats can vary slightly between
major versions.
**Symptoms:** list output may be missing or inconsistently shaped when
pointed at a different AEM major version than the one the response parser was
tested against.
**Workaround:** validate against your target version; run the command in
`--dry-run` or `--mock` first and compare the live response shape.
