# AEM API CLI

A command-line interface for testing Adobe Experience Manager (AEM) APIs.

## Why This Tool?

| Pain Point | Solution |
|------------|----------|
| Writing curl scripts for every API call | Pre-built commands for 20+ AEM APIs |
| Managing multiple AEM environments | `aem-api connect --env dev/prod/local` |
| Plain text credentials in scripts | AES-256-GCM encrypted credentials |
| No scripting for API results | Unix-style piping: `cf list \| grep foo` |
| Slow API exploration | Interactive shell with completion |

## Comparison

| Feature | This Tool | cURL | Postman | ACS Commons |
|---------|-----------|------|---------|-------------|
| AEM-specific commands | ✅ | ❌ | ❌ | ✅ |
| Piping/Chaining | ✅ | ✅ | ❌ | ❌ |
| Encrypted credentials | ✅ | ❌ | ⚠️ | ❌ |
| Shell completion | ✅ | ❌ | ❌ | N/A |
| AI assistance | ✅ | ❌ | ❌ | ❌ |
| No AEM dependency | ✅ | ✅ | ✅ | ❌ |

## Quick Start

```bash
# Build
mvn package

# Run
java -jar target/aem-api-1.0.0.jar --help

# Connect to AEM
aem-api connect --env local --url http://localhost:4502 --user admin --password admin --save

# List content fragments
aem-api cf list --path /content/dam/my-project

# Interactive mode
aem-api shell
```

## Piping & Chaining

```bash
# Filter results
aem-api cf list | grep my-fragment

# Chain on success
aem-api assets list --path /content/dam && aem-api replicate publish

# Format output
aem-api cf list | json
aem-api cf list | table

# Export results
aem-api cf list | export results.txt
```

## Security

- **Credentials**: Encrypted with AES-256-GCM
- **Master key**: Stored in `~/.config/aem-api/.key`
- **HTTPS enforcement**: `--https-only` flag
- **Input validation**: Path traversal, injection prevention

## Commands

| Command | Description |
|---------|-------------|
| `cf` | Content Fragment CRUD |
| `assets` | DAM asset management |
| `sites` | Pages & sites |
| `graphql` | GraphQL queries |
| `workflow` | Workflow operations |
| `replicate` | Publish/unpublish |
| `packages` | Package management |
| `users` | User management |
| `agent` | AI-powered assistant |

## Architecture

```
┌─────────────────────────────────────────────┐
│                 AEM API CLI                  │
├─────────────────────────────────────────────┤
│  Shell (JLine3)  │  PipeProcessor          │
│  ─────────────   │  ─────────────          │
│  Command Router  │  Unix-style piping      │
├─────────────────────────────────────────────┤
│              API Client Layer                │
│  ─────────────────────────────────────────  │
│  Auth (OAuth/Basic) │  Caching │ Retry    │
├─────────────────────────────────────────────┤
│           AEM REST APIs                      │
│  Content Fragments │ Assets │ GraphQL      │
└─────────────────────────────────────────────┘
```

## For Architects

- **No AEM dependency** - Runs standalone, no bundle install required
- **Standard Java 21** - Uses picocli, JLine3, Apache HttpClient
- **Extensible** - Add new commands via API classes
- **Tested** - Unit tests with JUnit 5

## License

Apache 2.0
