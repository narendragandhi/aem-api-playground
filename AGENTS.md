# AGENTS.md

Guidance for AI coding agents working in this repository.

## What this is

`aem-api` (project name "AEM API Playground") — a standalone Java 21 CLI with
two deliberately separate tools:

1. **Remote AEM API surface** — everything except `oak`/`oak-upgrade`: talks
   to a live AEM over REST (CLI, Studio GUI, shell, MCP server, recipes,
   `--mock` mode). No AEM bundle dependency.
2. **Offline repository maintenance** — `oak` and `oak-upgrade` only: wraps
   Apache oak-run/oak-upgrade against the segment store on the AEM host's
   disk. Different contract: AEM stopped, shell access to the host,
   version-locked jar.

Keep the two apart in code reviews and docs — a new offline command does not
belong in the remote surface and vice versa. The only bridge is
`OakVersionDetector` (used by `oak detect`).

## Build and test

```bash
mvn package          # builds the fat jar in target/
mvn test             # full suite (~260 tests)
mvn test -Dtest=ConfigManagerEnvOverrideTest   # single class
```

- Java 21 required. Use `./mvnw` if Maven is not installed.
- Test framework: JUnit 5, AssertJ/JUnit assertions, JaCoCo coverage.
- Tests share the `ConfigManager` singleton; use unique environment names in
  tests and restore injected state (see `ConfigManagerEnvOverrideTest`).

## Conventions

- CLI: picocli with global flags funneled through `CliFlags` (mixin pattern).
- New auth/URL resolution goes in `ConfigManager` so CLI, GUI, and MCP server
  all pick it up. Precedence: CLI flags > `AEM_API_*` env vars > saved profile.
- Credentials are AES-256-GCM encrypted via `CredentialEncryption`; never log
  or commit secrets, and never send credentials over plain HTTP unless the
  user explicitly opted out of `--https-only`.
- Mock-based tests implement the real HTTP endpoints (see
  `AemCloudMockE2ETest`), so prefer extending those over adding test doubles.

## Docs to keep in sync

- `CHANGELOG.md` — every user-visible change under `[Unreleased]`.
- `README.md` — command table, caveats, and the fidelity-scores table.
- `issues/` — per-command gaps. When a gap is fixed, move the file from
  `things-that-do-not-work/` to `things-that-now-work/`, update its status
  line, and adjust the README wording. When you discover a new gap, add a
  file there following the format in `issues/README.md`.

## AEM endpoints

Commands target two generations of AEM endpoints (modern `/api/*` and legacy
`/bin/*`, `/etc/*`, `/crx/*`). Check the README "Supported AEM versions &
endpoints" section before changing which endpoint a command calls — version
compatibility is the reason some commands use legacy endpoints.

## Workspace rules (from ../CLAUDE.md)

- Nothing ships without an explicit ask: no commits, pushes, or PRs unless
  the user asks for that specific change.
- Start or restart local AEM (localhost:4502) only when asked.
