# `pages create/update/delete/move` fail on AEM 6.5 before SP13

**Status:** does not work
**Affects:** `pages create`, `pages update`, `pages delete`, `pages move` — AEM 6.5 before SP13, and any AEM 6.4 or earlier
**Why:** the modern `/api/pages` endpoint does not exist on those versions;
these commands use only the modern endpoint. `pages list`/`get` still work
because they read `.1.json`, and `pages search` uses `/bin/cq/search.json`.
**Symptoms:** 404 responses from the target instance.
**Workaround:** none for these commands on pre-SP13 6.5/6.4. Run a command
with `--dry-run` first to inspect the exact HTTP path it would call, and
verify your target version supports `/api/pages`.
