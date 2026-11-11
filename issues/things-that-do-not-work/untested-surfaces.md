# GUI, interactive shell, MCP server, and AI agent lack automated tests

**Status:** does not work (test coverage gap)
**Affects:** `gui`, `shell`, the MCP server, and the `agent` command
**Why:** test coverage is focused on the API layer and data parsing
(~25% instruction, ~24% line). The `AemStudioGuiInteractionTest` covers view
registration, but these surfaces are not otherwise exercised by automated
tests.
**Symptoms:** regressions in these surfaces are found manually, not by CI.
**Workaround:** exercise changes manually before release; prefer changes that
go through the tested API/data layers where possible.
