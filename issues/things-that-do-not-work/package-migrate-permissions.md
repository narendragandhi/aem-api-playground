# `recipe package-migrate` requires package-manager rights on both environments

**Status:** works with caveats
**Affects:** `recipe package-migrate` — any source/target pair
**Why:** the recipe downloads the package from the active environment and
uploads/installs it to `--target-url` with the supplied auth.
**Symptoms:** 403s or failed installs when the configured user lacks
package-manager rights on either environment. On AEM as a Cloud Service,
package `install`/`uninstall` are restricted on higher tiers.
**Workaround:** verify package-manager rights on both environments before
running; on Cloud, prefer dev/RDE targets or Cloud Manager pipelines for
deployment.
