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

### Changed
- Dependencies updated to current stable releases: Jackson 2.22.1, HttpClient5
  5.6.3, picocli 4.7.7, JLine 3.30.16, JUnit 5.14.4, Mockito 5.23.0,
  sqlite-jdbc 3.53.2.1, Logback 1.6.1, FlatLaf 3.7.2, and current Maven
  plugin versions (checkstyle 3.6.0, jacoco 0.8.15, surefire 3.5.6, shade
  3.6.2, compiler 3.15.0, exec 3.6.3).

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
