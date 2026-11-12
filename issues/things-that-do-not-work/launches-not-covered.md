# Launches API not covered

**Status:** does not work (coverage gap)
**Affects:** no command — AEMaaCS (modern surface); 6.5 via console only
**Why:** Adobe documents Launches as a first-class OpenAPI
(https://developer.adobe.com/experience-cloud/experience-manager-apis/api/stable/sites/launches/);
the CLI has no launches command at all.
**Symptoms:** creating, listing, promoting, or deleting launches must be done
in the author UI or via curl.
**Workaround:** author UI; `--dry-run` recipes that create page copies under
`/content/launches` mimic the simplest launch flows but are not a real
launch.
