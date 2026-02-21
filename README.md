# AEM API Playground

An interactive CLI tool for testing Adobe Experience Manager (AEM) APIs.

## Features

- **20+ Commands** - Content Fragments, Assets, GraphQL, Workflows, Users, Replication, and more
- **AI Agent** - Natural language interface powered by OpenAI GPT-4
- **Interactive Shell** - REPL mode with command history
- **Multi-environment** - Manage dev, staging, prod configurations
- **Security** - Encrypted credentials, HTTPS enforcement, input validation
- **Caching** - Response caching for both API calls and AI agent
- **Shell Completion** - Bash, Zsh, and Fish completions

## Installation

```bash
# Build
mvn package

# Run
java -jar target/aem-api-1.0-SNAPSHOT.jar --help
```

## Quick Start

```bash
# Connect to local AEM
aem-api connect --env local --local --user admin --password admin --save

# List content fragments
aem-api cf list --path /content/dam/my-project

# Use AI agent
aem-api agent --message "list content fragments in /content/dam"

# Interactive mode
aem-api shell
```

## Commands

| Command | Description |
|---------|-------------|
| `shell` | Interactive REPL mode |
| `connect` | Connect to AEM environment |
| `cf` | Content Fragment operations |
| `assets` | Digital Assets management |
| `sites` | Sites/Pages operations |
| `forms` | Adaptive Forms |
| `graphql` | GraphQL queries |
| `translation` | Translation projects |
| `cloudmgr` | Cloud Manager API |
| `folders` | Folder operations |
| `tags` | Tag management |
| `workflow` | Workflow operations |
| `users` | User management |
| `replicate` | Replication/publish |
| `packages` | Content packages |
| `models` | Content Fragment Models |
| `audit` | Audit logs & cache |
| `agent` | AI-powered assistant |
| `config` | Configuration management |
| `completion` | Shell completion |

## AI Agent

```bash
# Chat with AI
aem-api agent --message "create a content fragment for my blog post"

# Interactive mode
aem-api agent -i

# Save session
aem-api agent --save-session my-project

# View memory stats
aem-api agent --stats
```

## Configuration

Config stored in `~/.config/aem-api/config.yaml` (or `~/.aem-api`).

```yaml
environments:
  dev:
    url: "https://author.dev.adobe.com"
    accessToken: "encrypted-token"
  local:
    url: "http://localhost:4502"
    basicAuth: "encrypted-basic-auth"
activeEnvironment: dev
```

## Environment Variables

| Variable | Description |
|----------|-------------|
| `AEM_API_KEY` | API key |
| `OPENAI_API_KEY` | OpenAI key for agent |
| `AEM_ENV` | Default environment |
| `AEM_DEBUG` | Enable debug mode |
| `AEM_TIMEOUT` | Request timeout (ms) |
| `XDG_CONFIG_HOME` | Config directory |

## Security

- Credentials encrypted with AES-256-GCM
- Master key stored in `~/.config/aem-api/.key`
- HTTPS enforcement available (`--https-only`)
- Input validation for paths and parameters

## Shell Completion

```bash
# Generate
aem-api completion --bash > /etc/bash_completion.d/aem-api
source <(aem-api completion --bash)

# Or install to home
aem-api completion --install
```

## Exit Codes

| Code | Meaning |
|------|---------|
| 0 | Success |
| 1 | General error |
| 2 | Invalid arguments |
| 3 | Authentication error |
| 4 | Network error |
| 5 | Not connected |

## Build Requirements

- Java 21+
- Maven 3.8+

## License

Apache 2.0
