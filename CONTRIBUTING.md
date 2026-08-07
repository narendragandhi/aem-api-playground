# Contributing to AEM API CLI

Thanks for your interest in contributing! This project is a command-line
interface for testing Adobe Experience Manager (AEM) APIs, built with Java 21,
picocli, JLine3, and Apache HttpClient.

## Getting Started

1. **Fork** the repository and create a branch from `main`.
2. Install **JDK 21** and **Maven 3.9+**.
3. Build and run the test suite:

   ```bash
   mvn test
   ```

4. Make your changes and add tests for them.

## Development Workflow

```bash
./mvnw verify          # compiles, runs checkstyle, and runs all tests
./mvnw spotbugs:check  # optional static analysis
java -jar target/aem-api-1.0.0.jar --help
java -jar target/aem-api-1.0.0.jar --mock gui   # explore the GUI with mock data
```

### Code style

- Checkstyle (Google style) runs in the `validate` phase. Keep it green.
- No external formatting tools are required; match the surrounding style.

### Commit conventions

Use descriptive commit messages in the imperative mood, for example:

```
Fix NPE when resolving content fragment references
```

### Before submitting a PR

- Run `mvn test` locally and confirm it is green.
- Make sure your branch is rebased on the latest `main`.
- Reference the issue you're addressing, if any.

## Project Layout

```
src/main/java/com/aemtools/aem/
  AemApi.java          # root command and CLI entry point
  CliFlags.java        # global flag runtime store (populated by GlobalFlags)
  GlobalFlags.java     # picocli mixin for --mock/--json/--output/... options
  api/                 # per-domain API clients (AssetsApi, PagesApi, ...)
  commands/            # picocli subcommand implementations
  client/              # low-level HTTP client (AemApiClient)
  config/              # config/env/logging management
  security/            # credential encryption, OAuth, input validation
  shell/               # interactive shell + piping
  mcp/                 # Model Context Protocol server
  gui/                 # Swing AEM API Studio
```

## Testing

- JUnit 5 + Mockito. Unit tests live under
  `src/test/java/com/aemtools/aem/`.
- Prefer mock-based tests over live AEM connections so the suite runs
  anywhere. The CLI also ships a `--mock` mode for offline smoke tests.

## Reporting Issues

Open an issue for bugs, feature requests, or documentation improvements. For
security vulnerabilities, follow the process in [SECURITY.md](SECURITY.md).

## Code of Conduct

All participants are expected to follow the
[Contributor Covenant](CODE_OF_CONDUCT.md).
