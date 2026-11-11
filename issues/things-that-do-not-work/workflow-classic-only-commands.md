# `workflow inbox/history/purge` are classic-only (no `/api/*` equivalent)

**Status:** does not work on modern endpoints
**Affects:** `workflow inbox`, `workflow history`, `workflow purge` — all AEM versions
**Why:** these commands use the classic `/bin/workflow/inbox`,
`/bin/workflow/history.json`, and `/etc/workflow/instances.purge.json`
endpoints. There is no `/api/*` (modern) equivalent, so unlike `workflow
start` they cannot fall back or modernize.
**Symptoms:** they work on classic AEM 6.x but are not on the officially
supported `/api/*` path going forward.
**Workaround:** use them as-is on classic instances; on Cloud Service, prefer
`workflow list`/`start`/`terminate` via `/api/workflow/*` and manage the inbox
in the UI.
