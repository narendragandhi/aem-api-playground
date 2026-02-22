# AEM SDK API vs Direct OSGi Services

Adobe Experience Manager (AEM) provides two categories of OSGi services:

1. **Public SDK API** - Can use `@Reference` injection
2. **Direct OSGi Calls** - Require `ServiceTracker`, `getServiceReference()`, or `bundleContext.getService()`

This document identifies which services require direct OSGi calls.

---

## Services Covered by Public SDK API

These services are part of the official AEM SDK API and can be injected using `@Reference`:

### ResourceResolverFactory (Public API)
```java
@Reference
private ResourceResolverFactory resourceResolverFactory;
```
Package: `org.apache.sling.api.resource`

### QueryBuilder (Public API)
```java
@Reference
private QueryBuilder queryBuilder;
```
Package: `com.day.cq.search`

### PageManager (Public API)
```java
@Reference
private PageManager pageManager;
```
Package: `com.day.cq.wcm.api`

### WorkflowService (Public API)
```java
@Reference
private WorkflowService workflowService;
```
Package: `com.adobe.granite.workflow`

### Replicator (Public API)
```java
@Reference
private Replicator replicator;
```
Package: `com.day.cq.replication`

### TagManager (Public API)
```java
@Reference
private TagManager tagManager;
```
Package: `com.day.cq.tagging`

### AssetManager (Public API)
```java
@Reference
private AssetManager assetManager;
```
Package: `com.day.cq.dam.api`

### ModelFactory (Public API)
```java
@Reference
private ModelFactory modelFactory;
```
Package: `org.apache.sling.models.factory`

### SlingSettingsService (Public API)
```java
@Reference
private SlingSettingsService settings;
```
Package: `org.apache.sling.settings`

---

## Services Requiring Direct OSGi Calls

These services are NOT part of the public AEM SDK API. They require direct OSGi service lookup:

### 1. UserManager (Jackrabbit API - Direct OSGi)
```java
// Using ServiceTracker
ServiceTracker<UserManager, UserManager> tracker = new ServiceTracker<>(
    bundleContext, 
    UserManager.class.getName(), 
    null
);
tracker.open();
UserManager userManager = tracker.getService();

// Or using getServiceReference
ServiceReference<UserManager> ref = bundleContext.getServiceReference(UserManager.class);
UserManager userManager = bundleContext.getService(ref);
```
**Package**: `org.apache.jackrabbit.api.security.user`
**Why**: Jackrabbit API, not AEM public API

### 2. SlingRepository (Direct OSGi)
```java
ServiceReference<SlingRepository> ref = bundleContext.getServiceReference(SlingRepository.class);
SlingRepository repository = bundleContext.getService(ref);

Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
```
**Package**: `org.apache.jackrabbit.api`
**Why**: Jackrabbit API, not AEM public API

### 3. PackageManager (Direct OSGi - Some Methods)
```java
ServiceReference<PackageManager> ref = bundleContext.getServiceReference(PackageManager.class);
PackageManager pkgManager = bundleContext.getService(ref);
```
**Package**: `com.day.jcr.vault`
**Note**: Some methods available via public API, but full functionality requires direct access

### 4. VersionManager (Direct OSGi)
```java
ServiceReference<VersionManager> ref = bundleContext.getServiceReference(VersionManager.class);
VersionManager vm = bundleContext.getService(ref);
```
**Package**: `org.apache.jackrabbit.api`
**Why**: Jackrabbit API

### 5. DataStore (Direct OSGi)
```java
ServiceReference<DataStore> ref = bundleContext.getServiceReference(DataStore.class);
DataStore dataStore = bundleContext.getService(ref);
```
**Package**: `org.apache.jackrabbit.core.data`
**Why**: Internal Jackrabbit storage

### 6. ObservationService (Direct OSGi)
```java
ServiceReference<ObservationManager> ref = bundleContext.getServiceReference(ObservationManager.class);
ObservationManager obsMgr = bundleContext.getService(ref);
```
**Package**: `javax.jcr.observation`
**Why**: JCR observation events

### 7. LockManager (Direct OSGi)
```java
ServiceReference<LockManager> ref = bundleContext.getServiceReference(LockManager.class);
LockManager lockManager = bundleContext.getService(ref);
```
**Package**: `org.apache.jackrabbit.api`
**Why**: Jackrabbit API for node locking

### 8. NamespaceRegistry (Direct OSGi)
```java
ServiceReference<NamespaceRegistry> ref = bundleContext.getServiceReference(NamespaceRegistry.class);
NamespaceRegistry nsRegistry = bundleContext.getService(ref);
```
**Package**: `javax.jcr`
**Why**: JCR namespace management

### 9. NodeTypeRegistry (Direct OSGi)
```java
ServiceReference<NodeTypeRegistry> ref = bundleContext.getServiceReference(NodeTypeRegistry.class);
NodeTypeRegistry ntRegistry = bundleContext.getService(ref);
```
**Package**: `org.apache.jackrabbit.core`
**Why**: Jackrabbit internal API

### 10. ReplicationLog (Direct OSGi)
```java
ServiceReference<ReplicationLog> ref = bundleContext.getServiceReference(ReplicationLog.class);
ReplicationLog log = bundleContext.getService(ref);
```
**Package**: `com.day.cq.replication`
**Note**: Internal replication logging

### 11. WorkflowJobExecutor (Direct OSGi)
```java
ServiceReference<WorkflowJobExecutor> ref = bundleContext.getServiceReference(WorkflowJobExecutor.class);
WorkflowJobExecutor executor = bundleContext.getService(ref);
```
**Package**: `com.adobe.granite.workflow`
**Note**: Internal workflow execution

### 12. TransientStore (Direct OSGi)
```java
ServiceReference<TransientStore> ref = bundleContext.getServiceReference(TransientStore.class);
TransientStore store = bundleContext.getService(ref);
```
**Package**: `org.apache.jackrabbit.core`
**Why**: Internal transient storage

---

## How to Use Direct OSGi Service Calls

### Method 1: ServiceTracker (Recommended)
```java
@Component(service = MyService.class)
public class MyService {
    private BundleContext bundleContext;
    private ServiceTracker<UserManager, UserManager> userManagerTracker;

    @Activate
    public void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        
        // Create tracker for UserManager
        this.userManagerTracker = new ServiceTracker<>(
            bundleContext,
            UserManager.class.getName(),
            null
        );
        this.userManagerTracker.open();
    }

    public void doSomething() {
        UserManager um = userManagerTracker.getService();
        if (um != null) {
            // Use UserManager
        }
    }

    @Deactivate
    public void deactivate() {
        userManagerTracker.close();
    }
}
```

### Method 2: getServiceReference
```java
@Service
public class MyService {
    private BundleContext bundleContext;

    @Activate
    public void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void doSomething() {
        ServiceReference<UserManager> ref = bundleContext.getServiceReference(UserManager.class);
        if (ref != null) {
            try {
                UserManager um = bundleContext.getService(ref);
                // Use UserManager
            } finally {
                bundleContext.ungetService(ref);
            }
        }
    }
}
```

### Method 3: ServiceTrackerCustomizer (Advanced)
```java
ServiceTracker<UserManager, UserManager> tracker = new ServiceTracker<>(
    bundleContext,
    UserManager.class.getName(),
    new ServiceTrackerCustomizer<UserManager, UserManager>() {
        @Override
        public UserManager addingService(ServiceReference<UserManager> ref) {
            return bundleContext.getService(ref);
        }

        @Override
        public void modifiedService(ServiceReference<UserManager> ref, UserManager service) {
        }

        @Override
        public void removedService(ServiceReference<UserManager> ref, UserManager service) {
            bundleContext.ungetService(ref);
        }
    }
);
tracker.open();
```

---

## Quick Reference: Public vs Direct OSGi

| Service | Type | Package |
|---------|------|---------|
| ResourceResolverFactory | Public API | `org.apache.sling.api.resource` |
| QueryBuilder | Public API | `com.day.cq.search` |
| PageManager | Public API | `com.day.cq.wcm.api` |
| WorkflowService | Public API | `com.adobe.granite.workflow` |
| Replicator | Public API | `com.day.cq.replication` |
| TagManager | Public API | `com.day.cq.tagging` |
| AssetManager | Public API | `com.day.cq.dam.api` |
| ModelFactory | Public API | `org.apache.sling.models.factory` |
| **UserManager** | **Direct OSGi** | `org.apache.jackrabbit.api.security.user` |
| **SlingRepository** | **Direct OSGi** | `org.apache.jackrabbit.api` |
| **PackageManager** | **Direct OSGi** | `com.day.jcr.vault` |
| **VersionManager** | **Direct OSGi** | `org.apache.jackrabbit.api` |
| **DataStore** | **Direct OSGi** | `org.apache.jackrabbit.core.data` |
| **LockManager** | **Direct OSGi** | `org.apache.jackrabbit.api` |
| **NodeTypeRegistry** | **Direct OSGi** | `org.apache.jackrabbit.core` |
| **TransientStore** | **Direct OSGi** | `org.apache.jackrabbit.core` |

---

## Version History

- **2026.2** - Current SDK version
- Services marked with `@ProviderType` are public API
- Use ServiceTracker for dynamic service lookup

## References

- [Adobe AEM SDK Docs](https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/implementing/developing/aem-as-a-cloud-service-sdk)
- [Apache Sling API](https://sling.apache.org/apidocs/sling12/)
- [AEM Javadoc](https://developer.adobe.com/experience-manager/reference-materials/cloud-service/javadoc/)
- [OSGi Service Tracker](https://docs.osgi.org/javadoc/osgi.core/8.0.0/org/osgi/util/tracker/ServiceTracker.html)

---

## Core Resource Services

### ResourceResolverFactory
Manages ResourceResolver instances for accessing JCR content.

```java
@Reference
private ResourceResolverFactory resourceResolverFactory;

// Service resolver (system user)
Map<String, Object> authInfo = new HashMap<>();
authInfo.put(ResourceResolverFactory.SUBSERVICE, "my-service");
ResourceResolver resolver = resourceResolverFactory.getServiceResourceResolver(authInfo);

// Admin resolver (use sparingly)
ResourceResolver adminResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
```

| Method | Description |
|--------|-------------|
| `getServiceResourceResolver(Map<String,Object>)` | Get resolver for service user |
| `getAdministrativeResourceResolver(Map<String,Object>)` | Get admin resolver |
| `getResourceResolver(Map<String,Object>)` | Standard resolver |

### ResourceResolver
Access and manipulate JCR resources.

```java
@Reference
private ResourceResolver resolver;

Resource resource = resolver.getResource("/content/we-retail/us/en");
Node node = resource.adaptTo(Node.class);
ValueMap props = resource.getValueMap();
```

---

## Query & Search

### QueryBuilder
Build and execute JCR queries.

```java
@Reference
private QueryBuilder queryBuilder;

Map<String, String> predicates = new HashMap<>();
predicates.put("type", "cq:Page");
predicates.put("path", "/content/we-retail");
predicates.put("1_property", "jcr:content/title");
predicates.put("1_property.value", "Welcome");

Query query = queryBuilder.createQuery(PredicateGroup.create(predicates), session);
SearchResult result = query.getResult();
```

| Service | Description |
|---------|-------------|
| `QueryBuilder` | Create/execute JCR queries |
| `PredicateGroup` | Query predicate builder |
| `Query` | Executable query object |
| `SearchResult` | Query results |

---

## Page Management

### PageManager
Manage AEM pages.

```java
@Reference
private PageManager pageManager;

Page page = pageManager.getPage("/content/we-retail/us/en");
Page parent = page.getParent();
List<Page> children = page.listChildren();
Page newPage = pageManager.create("/content/we-retail/us/en", "newpage", "page-template", "New Page");
```

| Method | Description |
|--------|-------------|
| `getPage(path)` | Get page by path |
| `create(path, name, template, title)` | Create new page |
| `copy(source, target, shallow, preserve)` | Copy page |
| `move(source, target)` | Move page |
| `delete(page, force)` | Delete page |

### WCMUsePojo / WCMUse
Base class for Sightly/HTL Java backing components.

```java
public class MyComponent extends WCMUsePojo {
    @Override
    public void activate() throws Exception {
        ValueMap properties = getProperties();
        Resource resource = getResource();
        Page currentPage = getCurrentPage();
    }
}
```

---

## Workflow Services

### WorkflowService
Manage workflows.

```java
@Reference
private WorkflowService workflowService;

WorkflowSession wfSession = workflowService.getWorkflowSession(session);
Workflow[] workflows = wfSession.getWorkflows("/var/workflow/models/my-model", true);
```

### WorkflowSession
Execute workflow operations.

```java
WorkflowData wfData = wfSession.newWorkflowData("JCR_PATH", "/content/my-node");
WorkflowInstance instance = wfSession.startWorkflow(model, wfData);
wfSession.terminateWorkflow(instance);
```

| Service | Description |
|---------|-------------|
| `WorkflowService` | Workflow management |
| `WorkflowSession` | Workflow session operations |
| `Workflow` | Workflow model representation |
| `WorkflowInstance` | Running workflow instance |

---

## Replication

### Replicator
Publish/replicate content.

```java
@Reference
private Replicator replicator;

ReplicationOptions options = new ReplicationOptions();
options.setFilter(new ReplicationFilter());
replicator.replicate(session, ReplicationActionType.ACTIVATE, "/content/we-retail/us/en", options);
```

| Action Type | Description |
|-------------|-------------|
| `ACTIVATE` | Publish content |
| `DEACTIVATE` | Unpublish content |
| `DELETE` | Delete content |
| `INTERNAL` | Internal replication |

### TransportManager
Manage replication transport.

```java
@Reference
private TransportManager transportManager;
```

---

## Tagging

### TagManager
Manage tags.

```java
@Reference
private TagManager tagManager;

Tag tag = tagManager.resolve("my-tag");
Tag[] tags = tagManager.findTags("campaign*");
tagManager.createTag("my-new-tag", "My New Tag", "en");
```

| Method | Description |
|--------|-------------|
| `resolve(tagId)` | Get tag by ID/path |
| `createTag(path, title, locale)` | Create new tag |
| `deleteTag(tag)` | Delete tag |
| `findTags(query)` | Search tags |

---

## User Management

### UserManager
Manage users and groups.

```java
@Reference
private UserManager userManager;

User user = (User) userManager.getAuthorizable("admin");
Group group = (Group) userManager.getAuthorizable("contributors");
userManager.createUser("new-user", "password");
userManager.createGroup("new-group");
```

### Authorizableable
Represents users/groups.

```java
String id = authorizable.getID();
String principal = authorizable.getPrincipal().getName();
boolean isGroup = authorizable.isGroup();
```

---

## Asset Services

### AssetManager
Manage DAM assets.

```java
@Reference
private AssetManager assetManager;

Asset asset = assetManager.createAsset("/content/dam/my-image.png");
asset = assetManager.getAsset("/content/dam/my-image.png");
assetManager.removeAsset("/content/dam/my-image.png");
```

### Asset
DAM asset representation.

```java
Asset asset = assetManager.getAsset("/content/dam/photo.jpg");
Resource metadata = asset.getMetadata();
Rendition rendition = asset.getRendition("original");
InputStream is = rendition.getStream();
```

### MetadataEditor
Edit asset metadata.

```java
@Reference
private MetadataEditor metadataEditor;
```

---

## Content Services

### ExporterExporter
Export Sling Models as JSON.

```java
@Model(adaptables = Resource.class, resourceType = "my-app/components/content")
@Exporter(name = "jackson", extensions = "json")
public class MyModel implements ExporterExporter {
    // Model implementation
}
```

### ModelFactory
Programmatically export Sling Models.

```java
@Reference
private ModelFactory modelFactory;

MyModel model = resource.adaptTo(MyModel.class);
String json = modelFactory.exportModel(model, "jackson", String.class, options);
```

---

## Miscellaneous

### SlingSettingsService
Get Sling run modes and settings.

```java
@Reference
private SlingSettingsService settings;

Set<String> runModes = settings.getRunModes();
String id = settings.getId();
```

### SlingServletResolver
Resolve servlets.

```java
@Reference
private SlingServletResolver resolver;
```

### PackageManager
Manage content packages.

```java
@Reference
private PackageManager packageManager;

Package package = packageManager.getPackage("/etc/packages/my-package.zip");
packageManager.installPackage("/etc/packages", package);
```

### VersionManager
Manage version history.

```java
@Reference
private VersionManager versionManager;

VersionHistory vh = versionManager.getVersionHistory(resource);
Version version = vh.createVersion("checkpoint label", "checkpoint description");
```

---

## OSGi Service Injection Patterns

### Field Injection
```java
@Reference
private QueryBuilder queryBuilder;
```

### Constructor Injection (Recommended)
```java
private final QueryBuilder queryBuilder;

@Activate
public MyService(@Reference QueryBuilder queryBuilder) {
    this.queryBuilder = queryBuilder;
}
```

### Optional Reference
```java
@Reference(cardinality = ReferenceCardinality.OPTIONAL)
private SomeService optionalService;
```

### Multiple References (List)
```java
@Reference(cardinality = ReferenceCardinality.MULTIPLE, 
           policy = ReferencePolicy.DYNAMIC)
private List<MyPlugin> plugins;
```

---

## Common Service Interfaces

| Interface | Package | Description |
|-----------|---------|-------------|
| `ResourceResolverFactory` | `org.apache.sling.api.resource` | Resource resolver factory |
| `QueryBuilder` | `com.day.cq.search` | Query builder |
| `PageManager` | `com.day.cq.wcm.api` | Page management |
| `WorkflowService` | `com.adobe.granite.workflow` | Workflow ops |
| `Replicator` | `com.day.cq.replication` | Replication |
| `TagManager` | `com.day.cq.tagging` | Tag management |
| `UserManager` | `org.apache.jackrabbit.api.security.user` | User management |
| `AssetManager` | `com.day.cq.dam.api` | DAM assets |
| `ModelFactory` | `org.apache.sling.models.factory` | Sling Models |
| `PackageManager` | `com.day.jcr.vault` | Package management |
| `SlingSettingsService` | `org.apache.sling.settings` | Sling settings |

---

## Version History

- **2026.2** - Current SDK version
- Services marked with `@ProviderType` are public API
- Use ServiceReference for dynamic service lookup

## References

- [Adobe AEM SDK Docs](https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/implementing/developing/aem-as-a-cloud-service-sdk)
- [Apache Sling API](https://sling.apache.org/apidocs/sling12/)
- [AEM Javadoc](https://developer.adobe.com/experience-manager/reference-materials/cloud-service/javadoc/)
