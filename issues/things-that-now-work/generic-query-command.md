# Generic `query` command now exposed (Query Builder is an official AEM API)

**Status:** fixed (2026-11-12)
**Affects:** `query run` — AEM 6.x and AEMaaCS
**Why fixed:** the CLI now exposes generic Query Builder searches via
`aem-api query run --path --type --param k=v --limit` against
`/bin/querybuilder.json`, with mock/dry-run short-circuiting and 4 new MCP
tools (`aem_query`, plus generic `aem_jcr_*` node tools). Covered by
`QueryApiMockTest`. Verified on the Adobe developer portal that Query
Builder is the documented content-search API.
**Note:** JCR-SQL2 over plain HTTP is not an OOTB AEM REST API (the old
`/bin/query` servlet is not standard), so Query Builder predicates are the
supported surface — that part of the original gap is out of scope.
