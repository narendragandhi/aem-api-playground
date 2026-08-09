# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed
- Compile blocker: `RecipeCommand` referenced `PagesApi` and `PackagesApi`
  without importing them; `mvn package` now builds cleanly.
- Global flags (`--mock`, `--json`, `--output`, `--max`, `--timeout`,
  `--cache`, `--verbose`, `--dry-run`) are now declared once in a picocli
  mixin and funnel into `CliFlags`, removing the duplicated option fields on
  the root command.
- Removed dead Spring-style `application.properties` from a non-Spring CLI.
- `CliFlagsTest` / `AemApiCliTest` now exercise the real picocli parser.
- `PackagesApi.parsePackage` derived package `name`/`group` from the ZIP
  path (e.g. `vanityurls-components-1.0.6`), so `get`/`build`/`install`/
  `delete`/`download` failed against PackMgr. It now prefers the
  authoritative `name`/`group` fields returned by `list.jsp`.
- User/group mutation methods in `UsersApi` sent JSON bodies to the Granite
  Security authorizables servlets, which expect URL-encoded form data
  (resulting in 404s). Added `AemApiClient.postForm` and switched all
  `createUser`/`createGroup`/membership/impersonation writes to it.

### Added
- `RecipeEngine` — a single source of truth for the five recipes (site
  launch, content backup, asset batch, user onboarding, package migrate)
  shared by the CLI, GUI, and MCP server.
- MCP tools to complete the PackMgr surface: `aem_packages_get`,
  `aem_packages_uninstall`, `aem_packages_delete`, `aem_packages_upload`,
  `aem_packages_download`.
- MCP recipe tools exposing the automation runbooks to AI agents:
  `aem_recipe_site_launch`, `aem_recipe_content_backup`,
  `aem_recipe_asset_batch`, `aem_recipe_user_onboard`,
  `aem_recipe_package_migrate`.
- `AemMcpServerTest` covering the JSON-RPC handshake, `tools/list`
  content, error responses, and recipe validation without network.
- `.mcp.json.example` template and README registration snippets for
  Claude Desktop, Claude Code CLI, and Cursor, including running this
  server side by side with Adobe's hosted AEM MCP server.

### Changed
- Dependencies updated to current stable releases: Jackson 2.22.1, HttpClient5
  5.6.3, picocli 4.7.7, JLine 3.30.16, JUnit 5.14.4, Mockito 5.23.0,
  sqlite-jdbc 3.53.2.1, Logback 1.6.1, FlatLaf 3.7.2, and current Maven
  plugin versions (checkstyle 3.6.0, jacoco 0.8.15, surefire 3.5.6, shade
  3.6.2, compiler 3.15.0, exec 3.6.3).
- `RecipeCommand` delegates execution to the shared `RecipeEngine`; the
  picocli option surface and mock/dry-run behavior are unchanged, so the
  CLI and GUI behave identically to before.

### Added
- Open-source hygiene: Apache-2.0 `LICENSE`, `CODE_OF_CONDUCT.md`,
  `SECURITY.md`, `CONTRIBUTING.md`, `CHANGELOG.md`, issue and pull request
  templates, Dependabot configuration, and a CodeQL workflow.
- `.env` files are now ignored (with an `.env.example` template).
- README command table expanded to cover all 22 subcommands.

## 2026-03-25 — Recipe hardening

- Fully implemented site launch and migration recipes; added `.gitignore`.
- Real `ContentBackupRecipe` implementation and GitHub Actions CI/CD.

## 2026-03-10 — Improvements

- Package structure refactor; functional recipes and AI actions.

## 2026-02-25 — AEM API Studio (GUI)

- Native Java Swing GUI (AEM API Studio) with dashboard, content browser,
  GraphQL editor, workflow monitor, recipe runner, and integrated console.

## 2026-02-23 — Security & quality

- Enabled checkstyle in the validate phase; added SpotBugs/JaCoCo config.
- `.env` and credentials added to `.gitignore`.
- Added log-level, log-file, proxy, and `.env` support.

## 2026-02-22 — Core CLI

- Real AEM API commands (assets, pages, tags, users, workflows, replication,
  packages, models, content fragments, GraphQL).
- OAuth, bulk operations, async handling, and unit tests.
- Mock mode, recipes, CLI flags, and test coverage.
- Unix-style piping, interactive shell, and documentation.
