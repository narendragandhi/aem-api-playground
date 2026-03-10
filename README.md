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

## AEM API Studio (GUI)

A full-featured visual interface is included for users who prefer a graphical dashboard over the terminal.

### Features
- **Dashboard**: Quick view of environment health and active tasks.
- **Content Browser**: Explore Assets and Content Fragments with a visual tree and property inspector.
- **GraphQL Editor**: Test and execute GraphQL queries with variables.
- **Workflow Monitor**: Track and manage running workflow instances.
- **Recipe Runner**: Execute multi-step automation sequences visually.
- **Integrated Console**: See the raw CLI logs and HTTP traffic in real-time.

### Launching the Studio
```bash
# Standard Launch
java -jar target/aem-api-1.0.0.jar gui

# Mock Mode (Explore without a server)
java -jar target/aem-api-1.0.0.jar --mock gui
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

## MCP Server for Claude Code

An MCP (Model Context Protocol) server is included for direct integration with Claude Code.
This provides Claude with native access to 40+ AEM tools without parsing CLI output.

### Setup

1. Build the MCP server JAR:
```bash
mvn package -DskipTests
```

2. Add to your Claude Code MCP configuration (`~/.claude/claude_desktop_config.json`):
```json
{
  "mcpServers": {
    "aem": {
      "command": "java",
      "args": ["-jar", "/path/to/aem-mcp-server-1.0.0.jar"]
    }
  }
}
```

3. Configure your AEM environment (one-time setup):
```bash
# Using the CLI to save credentials
java -jar aem-api-1.0.0.jar connect --env dev \
  --url http://localhost:4502 --user admin --password admin --save
```

### Available MCP Tools

| Category | Tools |
|----------|-------|
| **Workflows** | `aem_workflow_list`, `aem_workflow_start`, `aem_workflow_terminate`, `aem_workflow_models`, `aem_workflow_stats` |
| **Assets** | `aem_assets_list`, `aem_assets_get`, `aem_assets_delete`, `aem_assets_move`, `aem_assets_search`, `aem_folder_create` |
| **Content Fragments** | `aem_cf_list`, `aem_cf_get`, `aem_cf_create`, `aem_cf_delete`, `aem_cf_export` |
| **Tags** | `aem_tags_list`, `aem_tags_namespaces`, `aem_tags_create`, `aem_tags_delete`, `aem_tags_apply`, `aem_tags_usage` |
| **Users** | `aem_users_list`, `aem_users_get`, `aem_users_create`, `aem_users_delete`, `aem_groups_list`, `aem_groups_members`, `aem_users_add_to_group` |
| **Replication** | `aem_replicate_activate`, `aem_replicate_deactivate`, `aem_replicate_status` |
| **GraphQL** | `aem_graphql_execute`, `aem_graphql_persisted` |
| **Pages** | `aem_pages_list`, `aem_pages_get`, `aem_pages_create`, `aem_pages_delete` |
| **Packages** | `aem_packages_list`, `aem_packages_build`, `aem_packages_install` |

### Example Usage in Claude Code

Once configured, ask Claude:
- "List all running workflows in AEM"
- "Create a new content fragment in /content/dam/myproject"
- "Publish the page at /content/mysite/en/home"
- "Show me which users are in the administrators group"

## For Architects

- **No AEM dependency** - Runs standalone, no bundle install required
- **Standard Java 21** - Uses picocli, JLine3, Apache HttpClient
- **Extensible** - Add new commands via API classes
- **Tested** - Unit tests with JUnit 5
- **MCP Ready** - Native Claude Code integration via MCP protocol

## License

Apache 2.0
