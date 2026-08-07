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
# Build (use ./mvnw if you don't have Maven installed)
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
| `shell` | Enter interactive shell mode |
| `connect` | Connect to an AEM environment (basic auth / OAuth / IMS) |
| `cf` | Content Fragment CRUD |
| `assets` | DAM asset management |
| `sites` | Pages & sites |
| `forms` | Adaptive Forms operations |
| `config` | Configuration management (environments, defaults) |
| `graphql` | GraphQL queries (adhoc + persisted) |
| `translation` | Translation projects & jobs |
| `cloudmgr` | Cloud Manager API (programs, pipelines) |
| `folders` | DAM folder operations |
| `tags` | Tag management (create, merge, apply, usage) |
| `workflow` | Workflow operations (list, start, terminate, inbox, stats) |
| `users` | User & group management |
| `replicate` | Publish/unpublish |
| `packages` | Package management |
| `models` | Content Fragment Models |
| `audit` | Audit log & API cache operations |
| `agent` | AI-powered assistant (OpenAI / Anthropic / Ollama) |
| `completion` | Generate shell completion scripts |
| `gui` | Launch the AEM API Studio (desktop GUI) |
| `recipe` | Predefined multi-step recipes (site-launch, content-backup, …) |

### Global flags

Global options can be placed before any subcommand:

```bash
aem-api --mock cf list                  # run against mock data (no AEM needed)
aem-api --dry-run replicate publish     # show what would happen
aem-api --json workflow list            # JSON output
aem-api --output raw assets list        # table | json | raw
aem-api --max 100 cf list               # max results
aem-api --timeout 60 graphql query      # request timeout (seconds)
aem-api --cache false cf list           # disable response cache
aem-api --verbose shell                 # verbose output
```

## Supported AEM versions & endpoints

The CLI calls AEM over **two generations of endpoints**. Make sure your AEM
instance supports the endpoints used by the commands you run.

### Modern `/api/*` endpoints (AEM as a Cloud Service, AEM 6.5 SP13+)

| Command | Endpoint |
|---------|----------|
| `pages create / update / delete / move` | `/api/pages` |
| `cf list / get / create / delete` | `/api/content/fragments` |
| `assets list / get / delete / move` | `/api/assets` |
| `workflow start / list` | `/api/workflow/instances` |

These are the only officially supported APIs going forward and are required for
`pages` CRUD and modern fragment/asset operations.

### Legacy `/bin/*`, `/etc/*` and `/crx/*` endpoints (classic AEM 6.x)

| Command | Endpoint |
|---------|----------|
| `replicate publish / unpublish` | `/bin/replicate.json` |
| `workflow inbox / history / purge` | `/bin/workflow/inbox`, `/bin/workflow/history.json`, `/etc/workflow/instances.purge.json` |
| `users`, `groups`, `tags` (search) | `/bin/querybuilder.json` |
| `pages search` | `/bin/cq/search.json` |
| `packages` (all operations) | `/crx/packmgr/*` |

### Caveats

- **`pages create/update/delete/move` fail on AEM 6.5 before SP13** and on any
  AEM 6.4 or earlier — the modern `/api/pages` endpoint does not exist there.
  `pages list/get` and `pages search` still work because they use `.1.json` /
  `/bin/cq/search.json`.
- **`workflow start` tries `/api/workflow/instances` first, then falls back to
  `/etc/workflow/instances`** when the modern endpoint returns 404, so it works
  on both generations. The `workflow inbox`/`history`/`purge` commands are
  classic-only and have no `/api/*` equivalent.
- **`tags` and `users` rely on `/bin/querybuilder.json`**, which exists on all
  AEM 6.x and Cloud Service instances, but response formats can vary slightly
  between major versions.
- Always validate against your target version. When in doubt, run a command in
  `--dry-run` or `--mock` first and inspect the exact HTTP path it would call.

### Current limitations

- Test coverage is focused on the API layer and data parsing (~25% instruction,
  ~24% line). The GUI (`gui`), interactive shell, MCP server, and AI agent
  (`agent`) are not yet covered by automated tests.
- `recipe package-migrate` downloads the package from the active environment and
  uploads/installs it to `--target-url` with the supplied auth. Verify that your
  user has package-manager rights on both environments before running it.

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
