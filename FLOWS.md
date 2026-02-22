# AEM API CLI - Real-Time Complex Content Operations

## Flow Diagrams

### 1. Content Migration Flow
```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Source     │     │   Export     │     │   Transform  │     │   Import     │
│   AEM        │────▶│   to JSON    │────▶│   (optional) │────▶│   Target     │
│   Instance   │     │   cf list    │     │   pipe       │     │   AEM        │
└──────────────┘     └──────────────┘     └──────────────┘     └──────────────┘
                                                                     
# Command:
aem-api cf list --path /content/dam/source | pipe transform | aem-api cf create --target prod
```

### 2. Bulk Publish Flow
```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Query      │     │   Filter     │     │   Replicate  │     │   Verify     │
│   pages      │────▶│   by date    │────▶│   publish    │────▶│   status     │
│   cf list    │     │   pipe grep  │     │   replicate  │     │   audit      │
└──────────────┘     └──────────────┘     └──────────────┘     └──────────────┘

# Command:
aem-api cf list --path /content/dam/blog | pipe grep "2024-01" | aem-api replicate publish
```

### 3. Asset Sync Flow
```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Source     │     │   List       │     │   Download   │     │   Upload     │
│   DAM        │────▶│   new assets │────▶│   tmp dir    │────▶│   Target     │
│   assets     │     │   pipe new   │     │   curl       │     │   DAM        │
└──────────────┘     └──────────────┘     └──────────────┘     └──────────────┘
```

---

## Use Cases

### UC1: Weekend Content Publish
**Scenario:** Marketing team updates 50+ content fragments on Friday. Need to publish all by Monday morning.

```
# Traditional: Manual or custom scripts
# With CLI:
aem-api cf list --path /content/dam/campaigns/friday-update \
  | pipe grep "status:draft" \
  | aem-api replicate publish \
  && echo "Published successfully"
```

### UC2: Multi-Region Asset Rollout
**Scenario:** New product images need to go to AEM instances in US, EU, and APAC.

```
# Pipeline style:
aem-api assets upload --file product.jpg --path /content/dam/products \
  && aem-api replicate publish --env us \
  && aem-api replicate publish --env eu \
  && aem-api replicate publish --env apac
```

### UC3: Content Audit & Cleanup
**Scenario:** Find all content fragments older than 2 years and archive them.

```
# Complex query:
aem-api cf list --path /content/dam \
  | pipe filter --older-than "730d" \
  | pipe export /tmp/old-content.json \
  && aem-api workflow start --model archive-workflow --batch
```

### UC4: GraphQL Endpoint Validation
**Scenario:** Validate GraphQL queries before deployment.

```
# Query testing:
aem-api graphql --endpoint /graphql/execute \
  --query '{ articleList { title slug } }' \
  | pipe validate --schema article-schema.json \
  | pipe export /tmp/graphql-results.json
```

### UC5: Replication Queue Monitoring
**Scenario:** Monitor replication queue and alert if stuck.

```
# Watch mode:
aem-api replicate queue --watch --threshold 100 \
  && echo "ALERT: Queue stuck!"
```

---

## Complex Scenarios

### Scenario A: Content Fragment Model Migration

**Requirement:** Migrate all content fragments using model "blog-post" from dev to prod, but only those modified in the last week.

**Traditional Approach:**
1. Write a Java service or Groovy script
2. Run in AEM via curl
3. Handle auth, pagination, errors manually

**With CLI:**
```bash
# Step 1: List fragments with model filter
aem-api cf list --model blog-post --path /content/dam

# Step 2: Filter by modification date
... | pipe filter --modified-after "2024-01-01"

# Step 3: Export to JSON
... | pipe export migration-input.json

# Step 4: Transform if needed
cat migration-input.json | jq '.[] | {title, author, body}' > transformed.json

# Step 5: Import to target
aem-api cf import --file transformed.json --target /content/dam/prod

# Step 6: Verify and replicate
... | aem-api replicate publish
```

**Flow:**
```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  List CF    │───▶│  Filter by │───▶│  Export     │───▶│  Transform  │
│  by model   │    │  mod date  │    │  JSON       │    │  (jq)      │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
                                                              │
                                                              ▼
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  Publish    │◀───│  Verify     │◀───│  Import     │◀───│  Target     │
│  (replicate)│    │  count      │    │  CF API     │    │  AEM        │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
```

### Scenario B: DAM Asset Reorganization

**Requirement:** Move 1000+ assets from /content/dam/old-structure to /content/dam/new-structure, update references, and publish.

**With CLI:**
```bash
# Find all assets in old location
aem-api assets list --path /content/dam/old-structure \
  | pipe map --field path --to newPath \
    'str.replace("/old-structure/", "/new-structure/")' \
  | pipe batch --size 50 \
  | aem-api assets move --batch

# Update references
aem-api assets update-refs --from /old-structure/ --to /new-structure/

# Publish
aem-api replicate publish --path /content/dam/new-structure
```

### Scenario C: Multi-Step Workflow Activation

**Requirement:** Activate a new site section that requires:
1. Activate all pages
2. Activate all assets
3. Clear cache
4. Verify delivery API

**With CLI:**
```
┌──────────────┐
│  Start      │
└──────┬───────┘
       │
       ▼
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  Activate    │────▶│  Activate    │────▶│  Clear       │
│  Pages       │     │  Assets      │     │  Cache       │
│  sites act   │     │  assets act  │     │  cache clear │
└──────────────┘     └──────────────┘     └──────────────┘
                                                  │
                                                  ▼
                                          ┌──────────────┐
                                          │  Verify      │
                                          │  delivery    │
                                          │  health      │
                                          └──────────────┘
                                          
# One command:
aem-api workflow activate-site --path /content/we-retail/summer \
  --clear-cache --verify
```

---

## Command Combinations

### Daily Operations
```bash
# Morning: Check what's pending publish
aem-api replicate status | pipe count

# Get replication agents
aem-api replicate agents

# Clear stuck queue
aem-api replicate queue --clear
```

### Weekly Content Sync
```bash
# Export week's content
aem-api cf list --modified-after "last-monday" \
  | pipe json \
  | export weekly-content.json

# Import to staging
aem-api cf import --file weekly-content.json --env staging
```

### Release Day
```bash
# Complete release flow
aem-api packages build --name release-v2 \
  && aem-api packages install \
  && aem-api workflow start --model approval \
  && aem-api replicate publish
```

---

## Why This Is Unique

| Feature | Benefit |
|---------|---------|
| **Composable operations** | Pipe outputs to inputs, chain commands |
| **No scripting** | Unix-style CLI, no Java/Groovy needed |
| **Batch processing** | Handle 1000s of items with `--batch` |
| **Real-time feedback** | Watch mode, progress bars |
| **Audit trail** | All operations logged |
| **Multi-env support** | dev → staging → prod in one flow |
