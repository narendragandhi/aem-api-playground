package com.aemtools.aem.commands;

import com.aemtools.aem.api.TagsApi;
import com.aemtools.aem.api.TagsApi.Tag;
import com.aemtools.aem.api.TagsApi.TagNamespace;
import com.aemtools.aem.client.AemApiClient;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Command for AEM tag operations.
 * Supports listing, creating, updating, and deleting tags.
 */
@Command(name = "tags", description = "Tag operations", subcommands = {
    TagsCommand.ListCommand.class,
    TagsCommand.NamespacesCommand.class,
    TagsCommand.GetCommand.class,
    TagsCommand.CreateCommand.class,
    TagsCommand.DeleteCommand.class,
    TagsCommand.MoveCommand.class,
    TagsCommand.MergeCommand.class,
    TagsCommand.ApplyCommand.class,
    TagsCommand.UsageCommand.class,
    TagsCommand.LocalizeCommand.class
})
public class TagsCommand implements Callable<Integer> {

    private static AemApiClient sharedClient;

    public static void setSharedClient(AemApiClient client) {
        sharedClient = client;
    }

    private static TagsApi getApi() {
        if (sharedClient == null) {
            sharedClient = new AemApiClient();
        }
        return new TagsApi(sharedClient);
    }

    @Override
    public Integer call() throws Exception {
        System.out.println("Tag management commands:");
        System.out.println("  tags list       - List tags");
        System.out.println("  tags namespaces - List tag namespaces");
        System.out.println("  tags get        - Get tag details");
        System.out.println("  tags create     - Create a tag");
        System.out.println("  tags delete     - Delete a tag");
        System.out.println("  tags move       - Move/rename a tag");
        System.out.println("  tags merge      - Merge tags");
        System.out.println("  tags apply      - Apply tags to content");
        System.out.println("  tags usage      - Show tag usage");
        System.out.println("  tags localize   - Manage localized titles");
        return 0;
    }

    /**
     * Lists tags.
     */
    @Command(name = "list", description = "List tags")
    public static class ListCommand implements Callable<Integer> {
        @Option(names = {"-p", "--path"}, description = "Tag path or namespace", defaultValue = "/content/cq:tags")
        private String path;

        @Option(names = {"-r", "--recursive"}, description = "List recursively")
        private boolean recursive;

        @Option(names = {"-m", "--max"}, description = "Maximum results", defaultValue = "100")
        private int max;

        @Override
        public Integer call() throws Exception {
            try {
                TagsApi api = getApi();
                List<Tag> tags = api.listTags(path, recursive, max);

                System.out.println("\n=== Tags (" + tags.size() + ") ===");
                System.out.printf("%-40s %-30s %s%n", "TAG ID", "TITLE", "CHILDREN");
                System.out.println("-".repeat(80));

                for (Tag tag : tags) {
                    System.out.printf("%-40s %-30s %d%n",
                        truncate(tag.tagId(), 40),
                        truncate(tag.title(), 30),
                        tag.childCount());
                }
                return 0;
            } catch (Exception e) {
                System.err.println("Error listing tags: " + e.getMessage());
                return 1;
            }
        }

        private String truncate(String s, int max) {
            if (s == null) return "";
            return s.length() > max ? s.substring(0, max - 2) + ".." : s;
        }
    }

    /**
     * Lists tag namespaces.
     */
    @Command(name = "namespaces", description = "List tag namespaces")
    public static class NamespacesCommand implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            try {
                TagsApi api = getApi();
                List<TagNamespace> namespaces = api.listNamespaces();

                System.out.println("\n=== Tag Namespaces (" + namespaces.size() + ") ===");
                System.out.printf("%-20s %-30s %-8s %s%n", "ID", "TITLE", "TAGS", "DESCRIPTION");
                System.out.println("-".repeat(90));

                for (TagNamespace ns : namespaces) {
                    System.out.printf("%-20s %-30s %-8d %s%n",
                        truncate(ns.id(), 20),
                        truncate(ns.title(), 30),
                        ns.tagCount(),
                        truncate(ns.description() != null ? ns.description() : "", 30));
                }
                return 0;
            } catch (Exception e) {
                System.err.println("Error listing namespaces: " + e.getMessage());
                return 1;
            }
        }

        private String truncate(String s, int max) {
            if (s == null) return "";
            return s.length() > max ? s.substring(0, max - 2) + ".." : s;
        }
    }

    /**
     * Gets tag details.
     */
    @Command(name = "get", description = "Get tag details")
    public static class GetCommand implements Callable<Integer> {
        @Option(names = {"-t", "--tag"}, description = "Tag ID (namespace:tag)", required = true)
        private String tagId;

        @Option(names = {"--show-usage"}, description = "Show usage count")
        private boolean showUsage;

        @Option(names = {"--show-locales"}, description = "Show localized titles")
        private boolean showLocales;

        @Override
        public Integer call() throws Exception {
            try {
                TagsApi api = getApi();
                Tag tag = api.getTag(tagId);

                System.out.println("\n=== Tag Details ===");
                System.out.println("Tag ID:      " + tag.tagId());
                System.out.println("Path:        " + tag.path());
                System.out.println("Title:       " + tag.title());
                System.out.println("Description: " + (tag.description() != null ? tag.description() : "-"));
                System.out.println("Namespace:   " + tag.namespace());
                System.out.println("Children:    " + tag.childCount());

                if (showUsage) {
                    int usage = api.getTagUsageCount(tagId);
                    System.out.println("\nUsage Count: " + usage);
                }

                if (showLocales) {
                    Map<String, String> locales = api.getLocalizedTitles(tagId);
                    System.out.println("\nLocalized Titles:");
                    for (Map.Entry<String, String> entry : locales.entrySet()) {
                        System.out.println("  " + entry.getKey() + ": " + entry.getValue());
                    }
                }

                return 0;
            } catch (Exception e) {
                System.err.println("Error getting tag: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Creates a tag.
     */
    @Command(name = "create", description = "Create a tag or namespace")
    public static class CreateCommand implements Callable<Integer> {
        @Option(names = {"-t", "--tag"}, description = "Tag ID (namespace:tag)")
        private String tagId;

        @Option(names = {"-n", "--namespace"}, description = "Create a namespace instead")
        private String namespaceId;

        @Option(names = {"--title"}, description = "Display title")
        private String title;

        @Option(names = {"--description"}, description = "Description")
        private String description;

        @Override
        public Integer call() throws Exception {
            try {
                TagsApi api = getApi();

                if (namespaceId != null) {
                    TagNamespace ns = api.createNamespace(namespaceId, title, description);
                    System.out.println("Created namespace: " + ns.id());
                    System.out.println("  Title: " + ns.title());
                    System.out.println("  Path:  " + ns.path());
                } else if (tagId != null) {
                    Tag tag = api.createTag(tagId, title != null ? title : tagId, description);
                    System.out.println("Created tag: " + tag.tagId());
                    System.out.println("  Title: " + tag.title());
                    System.out.println("  Path:  " + tag.path());
                } else {
                    System.err.println("Specify either --tag or --namespace");
                    return 1;
                }

                return 0;
            } catch (Exception e) {
                System.err.println("Error creating tag: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Deletes a tag.
     */
    @Command(name = "delete", description = "Delete a tag")
    public static class DeleteCommand implements Callable<Integer> {
        @Option(names = {"-t", "--tag"}, description = "Tag ID (namespace:tag)", required = true)
        private String tagId;

        @Option(names = {"--force"}, description = "Force delete even if tag is in use")
        private boolean force;

        @Option(names = {"--confirm"}, description = "Confirm deletion")
        private boolean confirm;

        @Override
        public Integer call() throws Exception {
            if (!confirm) {
                System.out.println("This will permanently delete tag: " + tagId);
                System.out.println("Add --confirm to proceed.");
                return 1;
            }

            try {
                TagsApi api = getApi();
                api.deleteTag(tagId, force);
                System.out.println("Deleted tag: " + tagId);
                return 0;
            } catch (Exception e) {
                System.err.println("Error deleting tag: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Moves/renames a tag.
     */
    @Command(name = "move", description = "Move or rename a tag")
    public static class MoveCommand implements Callable<Integer> {
        @Option(names = {"-s", "--source"}, description = "Source tag ID", required = true)
        private String sourceTagId;

        @Option(names = {"-d", "--dest"}, description = "Destination tag ID", required = true)
        private String destTagId;

        @Override
        public Integer call() throws Exception {
            try {
                TagsApi api = getApi();
                Tag tag = api.moveTag(sourceTagId, destTagId);
                System.out.println("Moved tag to: " + tag.tagId());
                return 0;
            } catch (Exception e) {
                System.err.println("Error moving tag: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Merges tags.
     */
    @Command(name = "merge", description = "Merge one tag into another")
    public static class MergeCommand implements Callable<Integer> {
        @Option(names = {"-s", "--source"}, description = "Source tag ID (will be deleted)", required = true)
        private String sourceTagId;

        @Option(names = {"-d", "--dest"}, description = "Destination tag ID", required = true)
        private String destTagId;

        @Option(names = {"--confirm"}, description = "Confirm merge")
        private boolean confirm;

        @Override
        public Integer call() throws Exception {
            if (!confirm) {
                System.out.println("This will:");
                System.out.println("  1. Retag all content using '" + sourceTagId + "' to use '" + destTagId + "'");
                System.out.println("  2. Delete the source tag '" + sourceTagId + "'");
                System.out.println("Add --confirm to proceed.");
                return 1;
            }

            try {
                TagsApi api = getApi();
                int retagged = api.mergeTags(sourceTagId, destTagId);
                System.out.println("Merged " + sourceTagId + " into " + destTagId);
                System.out.println("Retagged " + retagged + " items");
                return 0;
            } catch (Exception e) {
                System.err.println("Error merging tags: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Applies tags to content.
     */
    @Command(name = "apply", description = "Apply or remove tags from content")
    public static class ApplyCommand implements Callable<Integer> {
        @Option(names = {"-c", "--content"}, description = "Content path", required = true)
        private String contentPath;

        @Option(names = {"--add"}, description = "Tags to add (comma-separated)")
        private String addTags;

        @Option(names = {"--remove"}, description = "Tags to remove (comma-separated)")
        private String removeTags;

        @Option(names = {"--replace"}, description = "Replace existing tags (with --add)")
        private boolean replace;

        @Override
        public Integer call() throws Exception {
            try {
                TagsApi api = getApi();

                if (addTags != null) {
                    List<String> tags = Arrays.asList(addTags.split(","));
                    api.applyTags(contentPath, tags, replace);
                    System.out.println("Applied " + tags.size() + " tags to " + contentPath);
                }

                if (removeTags != null) {
                    List<String> tags = Arrays.asList(removeTags.split(","));
                    api.removeTags(contentPath, tags);
                    System.out.println("Removed " + tags.size() + " tags from " + contentPath);
                }

                if (addTags == null && removeTags == null) {
                    System.err.println("Specify --add or --remove");
                    return 1;
                }

                return 0;
            } catch (Exception e) {
                System.err.println("Error applying tags: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Shows tag usage.
     */
    @Command(name = "usage", description = "Show tag usage")
    public static class UsageCommand implements Callable<Integer> {
        @Option(names = {"-t", "--tag"}, description = "Tag ID (namespace:tag)", required = true)
        private String tagId;

        @Option(names = {"--show-content"}, description = "Show content paths using this tag")
        private boolean showContent;

        @Option(names = {"-m", "--max"}, description = "Maximum content paths to show", defaultValue = "20")
        private int max;

        @Override
        public Integer call() throws Exception {
            try {
                TagsApi api = getApi();
                int count = api.getTagUsageCount(tagId);

                System.out.println("\nTag: " + tagId);
                System.out.println("Usage Count: " + count);

                if (showContent && count > 0) {
                    List<String> content = api.getTaggedContent(tagId, max);
                    System.out.println("\nContent using this tag:");
                    for (String path : content) {
                        System.out.println("  " + path);
                    }
                    if (count > content.size()) {
                        System.out.println("  ... and " + (count - content.size()) + " more");
                    }
                }

                return 0;
            } catch (Exception e) {
                System.err.println("Error getting usage: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Manages localized titles.
     */
    @Command(name = "localize", description = "Manage localized tag titles")
    public static class LocalizeCommand implements Callable<Integer> {
        @Option(names = {"-t", "--tag"}, description = "Tag ID (namespace:tag)", required = true)
        private String tagId;

        @Option(names = {"-l", "--locale"}, description = "Locale code (e.g., de, fr, ja)")
        private String locale;

        @Option(names = {"--title"}, description = "Localized title")
        private String title;

        @Option(names = {"--list"}, description = "List all localized titles")
        private boolean list;

        @Override
        public Integer call() throws Exception {
            try {
                TagsApi api = getApi();

                if (locale != null && title != null) {
                    api.setLocalizedTitle(tagId, locale, title);
                    System.out.println("Set " + locale + " title for " + tagId + ": " + title);
                }

                if (list || (locale == null && title == null)) {
                    Map<String, String> titles = api.getLocalizedTitles(tagId);
                    System.out.println("\nLocalized titles for " + tagId + ":");
                    for (Map.Entry<String, String> entry : titles.entrySet()) {
                        System.out.println("  " + entry.getKey() + ": " + entry.getValue());
                    }
                }

                return 0;
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                return 1;
            }
        }
    }
}
