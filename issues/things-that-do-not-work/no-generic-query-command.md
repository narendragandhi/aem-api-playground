# No generic `query` command despite Query Builder being an official AEM API

**Status:** does not work (coverage gap)
**Affects:** no command — AEM 6.x and AEMaaCS
**Why:** Query Builder is documented by Adobe as an official content-search
API (https://experienceleague.adobe.com/docs/experience-manager-cloud-service/content/implementing/developing/full-stack/search/query-builder-api.html),
and the CLI already uses it internally (`TagsApi`, `UsersApi`), but users
cannot run their own queries. JCR-SQL2 via `/bin/query` is likewise absent.
**Symptoms:** users must fall back to curl to search content by arbitrary
predicates; only the hard-coded tag/user queries are available.
**Workaround:** use `curl -u user:pass` against `/bin/querybuilder.json`, or
the `tags`/`users` commands for the two hard-coded query shapes.
