# oak-run / oak-upgrade wrappers are unverified on live segment stores

**Status:** works with caveats
**Affects:** `oak check/compact/checkpoints/index/backup/export`, `oak-upgrade` — any Oak repository
**Why:** the wrappers assemble the documented command lines (tested via
argument-assembly and jar-resolution unit tests) but have not been executed
against a real segment store or a real migration in CI — the tests never run
`java -jar oak-run.jar` / `oak-upgrade.jar` themselves.
**Symptoms:** none known; the risk is in flags the oak tools changed between
Oak branches (e.g. `oak-run index` sub-options) or jar/version mismatches.
**Workaround:** always run `aem-api oak detect` against the target instance
first and use the jar from the matching Oak branch; keep the global
`--dry-run` flag to inspect the exact command line before executing; run any
mutation (compact, reindex, oak-upgrade) against a copy of the store first.
