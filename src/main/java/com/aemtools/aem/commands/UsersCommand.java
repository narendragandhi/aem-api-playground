package com.aemtools.aem.commands;

import com.aemtools.aem.api.UsersApi;
import com.aemtools.aem.api.UsersApi.User;
import com.aemtools.aem.api.UsersApi.Group;
import com.aemtools.aem.client.AemApiClient;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Command for AEM user and group operations.
 * Supports listing, creating, and deleting users, and managing group memberships.
 */
@Command(name = "users", description = "User and group operations", subcommands = {
    UsersCommand.ListCommand.class,
    UsersCommand.GetCommand.class,
    UsersCommand.CreateCommand.class,
    UsersCommand.DeleteCommand.class,
    UsersCommand.GroupsCommand.class,
    UsersCommand.MembershipCommand.class,
    UsersCommand.EnableCommand.class,
    UsersCommand.ImpersonateCommand.class
})
public class UsersCommand implements Callable<Integer> {

    private static AemApiClient sharedClient;

    public static void setSharedClient(AemApiClient client) {
        sharedClient = client;
    }

    private static UsersApi getApi() {
        if (sharedClient == null) {
            sharedClient = new AemApiClient();
        }
        return new UsersApi(sharedClient);
    }

    @Override
    public Integer call() throws Exception {
        System.out.println("User and group management commands:");
        System.out.println("  users list      - List users");
        System.out.println("  users get       - Get user details");
        System.out.println("  users create    - Create a user");
        System.out.println("  users delete    - Delete a user");
        System.out.println("  users groups    - List/manage groups");
        System.out.println("  users membership - Manage group memberships");
        System.out.println("  users enable    - Enable/disable user");
        System.out.println("  users impersonate - Manage impersonation");
        return 0;
    }

    /**
     * Lists users.
     */
    @Command(name = "list", description = "List users")
    public static class ListCommand implements Callable<Integer> {
        @Option(names = {"-p", "--path"}, description = "Users path", defaultValue = "/home/users")
        private String path;

        @Option(names = {"-m", "--max"}, description = "Maximum results", defaultValue = "50")
        private int max;

        @Option(names = {"--groups"}, description = "List groups instead of users")
        private boolean groups;

        @Override
        public Integer call() throws Exception {
            try {
                UsersApi api = getApi();

                if (groups) {
                    List<Group> groupList = api.listGroups(path.replace("/users", "/groups"), max);
                    System.out.println("\n=== Groups (" + groupList.size() + ") ===");
                    System.out.printf("%-25s %-30s %s%n", "ID", "NAME", "MEMBERS");
                    System.out.println("-".repeat(70));
                    for (Group group : groupList) {
                        System.out.printf("%-25s %-30s %d%n",
                            truncate(group.id(), 25),
                            truncate(group.displayName(), 30),
                            group.memberCount());
                    }
                } else {
                    List<User> users = api.listUsers(path, max);
                    System.out.println("\n=== Users (" + users.size() + ") ===");
                    System.out.printf("%-20s %-25s %-30s %s%n", "ID", "NAME", "EMAIL", "STATUS");
                    System.out.println("-".repeat(85));
                    for (User user : users) {
                        System.out.printf("%-20s %-25s %-30s %s%n",
                            truncate(user.id(), 20),
                            truncate(user.displayName(), 25),
                            truncate(user.email() != null ? user.email() : "-", 30),
                            user.disabled() ? "DISABLED" : "ACTIVE");
                    }
                }
                return 0;
            } catch (Exception e) {
                System.err.println("Error listing users: " + e.getMessage());
                return 1;
            }
        }

        private String truncate(String s, int max) {
            if (s == null) return "";
            return s.length() > max ? s.substring(0, max - 2) + ".." : s;
        }
    }

    /**
     * Gets user details.
     */
    @Command(name = "get", description = "Get user details")
    public static class GetCommand implements Callable<Integer> {
        @Option(names = {"-u", "--user"}, description = "User ID", required = true)
        private String userId;

        @Option(names = {"--show-groups"}, description = "Show group memberships")
        private boolean showGroups;

        @Override
        public Integer call() throws Exception {
            try {
                UsersApi api = getApi();
                User user = api.getUser(userId);

                System.out.println("\n=== User Details ===");
                System.out.println("ID:         " + user.id());
                System.out.println("Path:       " + user.path());
                System.out.println("Name:       " + user.displayName());
                System.out.println("Email:      " + (user.email() != null ? user.email() : "-"));
                System.out.println("Status:     " + (user.disabled() ? "DISABLED" : "ACTIVE"));

                if (showGroups) {
                    List<String> groups = api.getUserGroups(userId);
                    System.out.println("\nGroup Memberships (" + groups.size() + "):");
                    for (String group : groups) {
                        System.out.println("  - " + group);
                    }
                }

                return 0;
            } catch (Exception e) {
                System.err.println("Error getting user: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Creates a user.
     */
    @Command(name = "create", description = "Create a user")
    public static class CreateCommand implements Callable<Integer> {
        @Option(names = {"-u", "--user-id"}, description = "User ID", required = true)
        private String userId;

        @Option(names = {"-p", "--password"}, description = "Password", required = true)
        private String password;

        @Option(names = {"-e", "--email"}, description = "Email address")
        private String email;

        @Option(names = {"--given-name"}, description = "First name")
        private String givenName;

        @Option(names = {"--family-name"}, description = "Last name")
        private String familyName;

        @Option(names = {"-g", "--groups"}, description = "Groups to add user to (comma-separated)")
        private String groups;

        @Override
        public Integer call() throws Exception {
            try {
                UsersApi api = getApi();

                User user = api.createUser(userId, password, email, givenName, familyName);
                System.out.println("Created user: " + user.id());

                if (groups != null && !groups.isEmpty()) {
                    List<String> groupList = Arrays.asList(groups.split(","));
                    for (String group : groupList) {
                        String groupId = group.trim();
                        try {
                            api.addUserToGroup(userId, groupId);
                            System.out.println("  Added to group: " + groupId);
                        } catch (Exception e) {
                            System.err.println("  Warning: Failed to add to group " + groupId + ": " + e.getMessage());
                        }
                    }
                }

                return 0;
            } catch (Exception e) {
                System.err.println("Error creating user: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Deletes a user.
     */
    @Command(name = "delete", description = "Delete a user")
    public static class DeleteCommand implements Callable<Integer> {
        @Option(names = {"-u", "--user-id"}, description = "User ID", required = true)
        private String userId;

        @Option(names = {"--confirm"}, description = "Confirm deletion")
        private boolean confirm;

        @Override
        public Integer call() throws Exception {
            if (!confirm) {
                System.out.println("This will permanently delete user: " + userId);
                System.out.println("Add --confirm to proceed.");
                return 1;
            }

            try {
                UsersApi api = getApi();
                api.deleteUser(userId);
                System.out.println("Deleted user: " + userId);
                return 0;
            } catch (Exception e) {
                System.err.println("Error deleting user: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Manages groups.
     */
    @Command(name = "groups", description = "List and manage groups")
    public static class GroupsCommand implements Callable<Integer> {
        @Option(names = {"-l", "--list"}, description = "List all groups")
        private boolean list;

        @Option(names = {"--create"}, description = "Create a new group")
        private String createGroupId;

        @Option(names = {"--delete"}, description = "Delete a group")
        private String deleteGroupId;

        @Option(names = {"-n", "--name"}, description = "Display name for new group")
        private String displayName;

        @Option(names = {"--confirm"}, description = "Confirm deletion")
        private boolean confirm;

        @Option(names = {"--members"}, description = "Show members of a group")
        private String showMembersOf;

        @Override
        public Integer call() throws Exception {
            try {
                UsersApi api = getApi();

                if (createGroupId != null) {
                    Group group = api.createGroup(createGroupId, displayName);
                    System.out.println("Created group: " + group.id());
                    return 0;
                }

                if (deleteGroupId != null) {
                    if (!confirm) {
                        System.out.println("This will permanently delete group: " + deleteGroupId);
                        System.out.println("Add --confirm to proceed.");
                        return 1;
                    }
                    api.deleteGroup(deleteGroupId);
                    System.out.println("Deleted group: " + deleteGroupId);
                    return 0;
                }

                if (showMembersOf != null) {
                    List<String> members = api.getGroupMembers(showMembersOf);
                    System.out.println("\nMembers of " + showMembersOf + " (" + members.size() + "):");
                    for (String member : members) {
                        System.out.println("  - " + member);
                    }
                    return 0;
                }

                // Default: list groups
                List<Group> groups = api.listGroups(null, 100);
                System.out.println("\n=== Groups (" + groups.size() + ") ===");
                System.out.printf("%-30s %-35s %s%n", "ID", "NAME", "MEMBERS");
                System.out.println("-".repeat(75));
                for (Group group : groups) {
                    System.out.printf("%-30s %-35s %d%n",
                        group.id().length() > 30 ? group.id().substring(0, 28) + ".." : group.id(),
                        group.displayName().length() > 35 ? group.displayName().substring(0, 33) + ".." : group.displayName(),
                        group.memberCount());
                }
                return 0;

            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Manages group membership.
     */
    @Command(name = "membership", description = "Manage group memberships")
    public static class MembershipCommand implements Callable<Integer> {
        @Option(names = {"-u", "--user"}, description = "User ID", required = true)
        private String userId;

        @Option(names = {"--add"}, description = "Add user to groups (comma-separated)")
        private String addToGroups;

        @Option(names = {"--remove"}, description = "Remove user from groups (comma-separated)")
        private String removeFromGroups;

        @Option(names = {"--list"}, description = "List user's group memberships")
        private boolean list;

        @Override
        public Integer call() throws Exception {
            try {
                UsersApi api = getApi();

                if (addToGroups != null) {
                    for (String group : addToGroups.split(",")) {
                        String groupId = group.trim();
                        api.addUserToGroup(userId, groupId);
                        System.out.println("Added " + userId + " to group: " + groupId);
                    }
                }

                if (removeFromGroups != null) {
                    for (String group : removeFromGroups.split(",")) {
                        String groupId = group.trim();
                        api.removeUserFromGroup(userId, groupId);
                        System.out.println("Removed " + userId + " from group: " + groupId);
                    }
                }

                if (list || (addToGroups == null && removeFromGroups == null)) {
                    List<String> groups = api.getUserGroups(userId);
                    System.out.println("\nGroup memberships for " + userId + " (" + groups.size() + "):");
                    for (String group : groups) {
                        System.out.println("  - " + group);
                    }
                }

                return 0;
            } catch (Exception e) {
                System.err.println("Error managing membership: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Enables or disables users.
     */
    @Command(name = "enable", description = "Enable or disable a user account")
    public static class EnableCommand implements Callable<Integer> {
        @Option(names = {"-u", "--user"}, description = "User ID", required = true)
        private String userId;

        @Option(names = {"--disable"}, description = "Disable the user (default is enable)")
        private boolean disable;

        @Override
        public Integer call() throws Exception {
            try {
                UsersApi api = getApi();
                api.setUserEnabled(userId, !disable);
                System.out.println((disable ? "Disabled" : "Enabled") + " user: " + userId);
                return 0;
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Manages impersonation permissions.
     */
    @Command(name = "impersonate", description = "Manage impersonation permissions")
    public static class ImpersonateCommand implements Callable<Integer> {
        @Option(names = {"-t", "--target"}, description = "Target user to be impersonated", required = true)
        private String targetUser;

        @Option(names = {"--allow"}, description = "User ID to allow impersonation")
        private String allowUser;

        @Option(names = {"--revoke"}, description = "User ID to revoke impersonation")
        private String revokeUser;

        @Option(names = {"--list"}, description = "List who can impersonate this user")
        private boolean list;

        @Override
        public Integer call() throws Exception {
            try {
                UsersApi api = getApi();

                if (allowUser != null) {
                    api.allowImpersonation(allowUser, targetUser);
                    System.out.println("Allowed " + allowUser + " to impersonate " + targetUser);
                }

                if (revokeUser != null) {
                    api.revokeImpersonation(revokeUser, targetUser);
                    System.out.println("Revoked " + revokeUser + " impersonation of " + targetUser);
                }

                if (list || (allowUser == null && revokeUser == null)) {
                    List<String> impersonators = api.getImpersonators(targetUser);
                    System.out.println("\nUsers who can impersonate " + targetUser + " (" + impersonators.size() + "):");
                    if (impersonators.isEmpty()) {
                        System.out.println("  (none)");
                    } else {
                        for (String imp : impersonators) {
                            System.out.println("  - " + imp);
                        }
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
