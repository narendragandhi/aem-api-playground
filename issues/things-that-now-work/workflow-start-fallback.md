# `workflow start` works on both classic and modern AEM

**Status:** fixed (documented)
**Affects:** `workflow start` — AEM 6.5 and Cloud Service
**Why:** the command tries `/api/workflow/instances` first, then falls back
to `/etc/workflow/instances` when the modern endpoint returns 404, so it
works on both generations of endpoints.
**Symptoms:** none; this is the intended behavior.
