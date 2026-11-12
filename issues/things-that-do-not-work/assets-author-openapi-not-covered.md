# Assets coverage uses the legacy API; the Assets Author OpenAPI is not covered

**Status:** partially works
**Affects:** `assets` — AEMaaCS (modern surface)
**Why:** the `assets` command calls the legacy `/api/assets` endpoint. Adobe
now documents a richer Assets Author OpenAPI
(https://developer.adobe.com/experience-cloud/experience-manager-apis/api/stable/assets/author/)
covering metadata schemas, renditions, versioning, and relation operations
that the legacy endpoint does not expose.
**Symptoms:** metadata-schema, rendition, and asset-version operations are
unavailable; some operations behave differently on AEMaaCS than on 6.5.
**Workaround:** use the author UI or curl for rendition/version operations;
the legacy endpoint remains supported for basic list/upload/delete.
