package com.aemtools.aem.api;

import com.aemtools.aem.client.AemApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API client for AEM User and Group management operations.
 * Supports creating, listing, deleting users and managing group memberships.
 */
public class UsersApi {

    private static final Logger logger = LoggerFactory.getLogger(UsersApi.class);
    private final AemApiClient client;

    public UsersApi(AemApiClient client) {
        this.client = client;
    }

    // ==================== User Operations ====================

    /**
     * Lists users in AEM.
     *
     * @param path the path to search for users (default: /home/users)
     * @param limit maximum number of users to return
     * @return list of users
     * @throws IOException if the API call fails
     */
    public List<User> listUsers(String path, int limit) throws IOException {
        String searchPath = path != null ? path : "/home/users";

        // Use query builder to find users
        String queryPath = String.format(
            "/bin/querybuilder.json?path=%s&type=rep:User&p.limit=%d&p.hits=full",
            searchPath, limit
        );

        JsonNode response = client.get(queryPath);
        List<User> users = new ArrayList<>();

        JsonNode hits = response.path("hits");
        if (hits.isArray()) {
            for (JsonNode hit : hits) {
                users.add(parseUser(hit));
            }
        }

        logger.info("Found {} users in {}", users.size(), searchPath);
        return users;
    }

    /**
     * Gets a specific user by ID.
     *
     * @param userId the user ID
     * @return the user details
     * @throws IOException if the API call fails
     */
    public User getUser(String userId) throws IOException {
        // First find the user path
        String queryPath = String.format(
            "/bin/querybuilder.json?path=/home/users&type=rep:User&property=rep:authorizableId&property.value=%s&p.limit=1",
            userId
        );

        JsonNode response = client.get(queryPath);
        JsonNode hits = response.path("hits");

        if (hits.isArray() && hits.size() > 0) {
            return parseUser(hits.get(0));
        }

        throw new IOException("User not found: " + userId);
    }

    /**
     * Creates a new user.
     *
     * @param userId the user ID
     * @param password the password
     * @param email optional email address
     * @param givenName optional given name
     * @param familyName optional family name
     * @return the created user
     * @throws IOException if the API call fails
     */
    public User createUser(String userId, String password, String email,
                           String givenName, String familyName) throws IOException {
        Map<String, Object> params = new HashMap<>();
        params.put("createUser", "");
        params.put("authorizableId", userId);
        params.put("rep:password", password);

        if (email != null && !email.isEmpty()) {
            params.put("profile/email", email);
        }
        if (givenName != null && !givenName.isEmpty()) {
            params.put("profile/givenName", givenName);
        }
        if (familyName != null && !familyName.isEmpty()) {
            params.put("profile/familyName", familyName);
        }

        JsonNode response = client.post("/libs/granite/security/post/authorizables", params);

        logger.info("Created user: {}", userId);

        // Return the newly created user
        return getUser(userId);
    }

    /**
     * Deletes a user.
     *
     * @param userId the user ID to delete
     * @return true if deleted successfully
     * @throws IOException if the API call fails
     */
    public boolean deleteUser(String userId) throws IOException {
        User user = getUser(userId);

        Map<String, Object> params = new HashMap<>();
        params.put("deleteAuthorizable", "");

        client.post(user.path(), params);

        logger.info("Deleted user: {}", userId);
        return true;
    }

    /**
     * Updates user profile properties.
     *
     * @param userId the user ID
     * @param properties map of properties to update
     * @return the updated user
     * @throws IOException if the API call fails
     */
    public User updateUser(String userId, Map<String, String> properties) throws IOException {
        User user = getUser(userId);

        Map<String, Object> params = new HashMap<>();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            params.put("profile/" + entry.getKey(), entry.getValue());
        }

        client.post(user.path(), params);

        logger.info("Updated user: {}", userId);
        return getUser(userId);
    }

    /**
     * Changes a user's password.
     *
     * @param userId the user ID
     * @param oldPassword the current password
     * @param newPassword the new password
     * @return true if password changed successfully
     * @throws IOException if the API call fails
     */
    public boolean changePassword(String userId, String oldPassword, String newPassword) throws IOException {
        User user = getUser(userId);

        Map<String, Object> params = new HashMap<>();
        params.put("rep:password", newPassword);
        params.put("oldPassword", oldPassword);

        client.post(user.path() + ".rw.userprops.html", params);

        logger.info("Changed password for user: {}", userId);
        return true;
    }

    /**
     * Enables or disables a user account.
     *
     * @param userId the user ID
     * @param enabled true to enable, false to disable
     * @return true if operation succeeded
     * @throws IOException if the API call fails
     */
    public boolean setUserEnabled(String userId, boolean enabled) throws IOException {
        User user = getUser(userId);

        Map<String, Object> params = new HashMap<>();
        params.put("rep:disabled", enabled ? "" : "Account disabled");

        client.post(user.path(), params);

        logger.info("{} user: {}", enabled ? "Enabled" : "Disabled", userId);
        return true;
    }

    // ==================== Group Operations ====================

    /**
     * Lists groups in AEM.
     *
     * @param path the path to search for groups (default: /home/groups)
     * @param limit maximum number of groups to return
     * @return list of groups
     * @throws IOException if the API call fails
     */
    public List<Group> listGroups(String path, int limit) throws IOException {
        String searchPath = path != null ? path : "/home/groups";

        String queryPath = String.format(
            "/bin/querybuilder.json?path=%s&type=rep:Group&p.limit=%d&p.hits=full",
            searchPath, limit
        );

        JsonNode response = client.get(queryPath);
        List<Group> groups = new ArrayList<>();

        JsonNode hits = response.path("hits");
        if (hits.isArray()) {
            for (JsonNode hit : hits) {
                groups.add(parseGroup(hit));
            }
        }

        logger.info("Found {} groups in {}", groups.size(), searchPath);
        return groups;
    }

    /**
     * Gets a specific group by ID.
     *
     * @param groupId the group ID
     * @return the group details
     * @throws IOException if the API call fails
     */
    public Group getGroup(String groupId) throws IOException {
        String queryPath = String.format(
            "/bin/querybuilder.json?path=/home/groups&type=rep:Group&property=rep:authorizableId&property.value=%s&p.limit=1",
            groupId
        );

        JsonNode response = client.get(queryPath);
        JsonNode hits = response.path("hits");

        if (hits.isArray() && hits.size() > 0) {
            return parseGroup(hits.get(0));
        }

        throw new IOException("Group not found: " + groupId);
    }

    /**
     * Creates a new group.
     *
     * @param groupId the group ID
     * @param givenName optional display name
     * @return the created group
     * @throws IOException if the API call fails
     */
    public Group createGroup(String groupId, String givenName) throws IOException {
        Map<String, Object> params = new HashMap<>();
        params.put("createGroup", "");
        params.put("authorizableId", groupId);

        if (givenName != null && !givenName.isEmpty()) {
            params.put("profile/givenName", givenName);
        }

        client.post("/libs/granite/security/post/authorizables", params);

        logger.info("Created group: {}", groupId);
        return getGroup(groupId);
    }

    /**
     * Deletes a group.
     *
     * @param groupId the group ID to delete
     * @return true if deleted successfully
     * @throws IOException if the API call fails
     */
    public boolean deleteGroup(String groupId) throws IOException {
        Group group = getGroup(groupId);

        Map<String, Object> params = new HashMap<>();
        params.put("deleteAuthorizable", "");

        client.post(group.path(), params);

        logger.info("Deleted group: {}", groupId);
        return true;
    }

    // ==================== Membership Operations ====================

    /**
     * Gets the groups a user belongs to.
     *
     * @param userId the user ID
     * @return list of group IDs
     * @throws IOException if the API call fails
     */
    public List<String> getUserGroups(String userId) throws IOException {
        User user = getUser(userId);

        JsonNode response = client.get(user.path() + ".rw.json?props=memberOf");
        List<String> groups = new ArrayList<>();

        JsonNode memberOf = response.path("memberOf");
        if (memberOf.isArray()) {
            for (JsonNode group : memberOf) {
                String groupPath = group.asText();
                // Extract group ID from path
                String groupId = groupPath.substring(groupPath.lastIndexOf('/') + 1);
                groups.add(groupId);
            }
        }

        return groups;
    }

    /**
     * Gets the members of a group.
     *
     * @param groupId the group ID
     * @return list of member user IDs
     * @throws IOException if the API call fails
     */
    public List<String> getGroupMembers(String groupId) throws IOException {
        Group group = getGroup(groupId);

        JsonNode response = client.get(group.path() + ".rw.json?props=members");
        List<String> members = new ArrayList<>();

        JsonNode membersNode = response.path("members");
        if (membersNode.isArray()) {
            for (JsonNode member : membersNode) {
                String memberPath = member.asText();
                String memberId = memberPath.substring(memberPath.lastIndexOf('/') + 1);
                members.add(memberId);
            }
        }

        return members;
    }

    /**
     * Adds a user to a group.
     *
     * @param userId the user ID
     * @param groupId the group ID
     * @return true if added successfully
     * @throws IOException if the API call fails
     */
    public boolean addUserToGroup(String userId, String groupId) throws IOException {
        Group group = getGroup(groupId);
        User user = getUser(userId);

        Map<String, Object> params = new HashMap<>();
        params.put("addMembers", user.path());

        client.post(group.path(), params);

        logger.info("Added user {} to group {}", userId, groupId);
        return true;
    }

    /**
     * Removes a user from a group.
     *
     * @param userId the user ID
     * @param groupId the group ID
     * @return true if removed successfully
     * @throws IOException if the API call fails
     */
    public boolean removeUserFromGroup(String userId, String groupId) throws IOException {
        Group group = getGroup(groupId);
        User user = getUser(userId);

        Map<String, Object> params = new HashMap<>();
        params.put("removeMembers", user.path());

        client.post(group.path(), params);

        logger.info("Removed user {} from group {}", userId, groupId);
        return true;
    }

    /**
     * Sets group membership for a user (replaces existing memberships).
     *
     * @param userId the user ID
     * @param groupIds list of group IDs
     * @return true if operation succeeded
     * @throws IOException if the API call fails
     */
    public boolean setUserGroups(String userId, List<String> groupIds) throws IOException {
        // Get current groups
        List<String> currentGroups = getUserGroups(userId);

        // Remove from groups not in the new list
        for (String groupId : currentGroups) {
            if (!groupIds.contains(groupId)) {
                removeUserFromGroup(userId, groupId);
            }
        }

        // Add to new groups
        for (String groupId : groupIds) {
            if (!currentGroups.contains(groupId)) {
                addUserToGroup(userId, groupId);
            }
        }

        logger.info("Set groups for user {}: {}", userId, groupIds);
        return true;
    }

    // ==================== Impersonation ====================

    /**
     * Gets the list of users that can impersonate the given user.
     *
     * @param userId the user ID
     * @return list of user IDs that can impersonate
     * @throws IOException if the API call fails
     */
    public List<String> getImpersonators(String userId) throws IOException {
        User user = getUser(userId);

        JsonNode response = client.get(user.path() + ".rw.json?props=impersonators");
        List<String> impersonators = new ArrayList<>();

        JsonNode impNode = response.path("impersonators");
        if (impNode.isArray()) {
            for (JsonNode imp : impNode) {
                impersonators.add(imp.asText());
            }
        }

        return impersonators;
    }

    /**
     * Allows a user to impersonate another user.
     *
     * @param impersonatorId the user who will impersonate
     * @param targetUserId the user to be impersonated
     * @return true if operation succeeded
     * @throws IOException if the API call fails
     */
    public boolean allowImpersonation(String impersonatorId, String targetUserId) throws IOException {
        User targetUser = getUser(targetUserId);

        Map<String, Object> params = new HashMap<>();
        params.put("addImpersonators", impersonatorId);

        client.post(targetUser.path(), params);

        logger.info("Allowed {} to impersonate {}", impersonatorId, targetUserId);
        return true;
    }

    /**
     * Revokes impersonation permission.
     *
     * @param impersonatorId the user who can currently impersonate
     * @param targetUserId the user being impersonated
     * @return true if operation succeeded
     * @throws IOException if the API call fails
     */
    public boolean revokeImpersonation(String impersonatorId, String targetUserId) throws IOException {
        User targetUser = getUser(targetUserId);

        Map<String, Object> params = new HashMap<>();
        params.put("removeImpersonators", impersonatorId);

        client.post(targetUser.path(), params);

        logger.info("Revoked {} impersonation of {}", impersonatorId, targetUserId);
        return true;
    }

    // ==================== Helper Methods ====================

    private User parseUser(JsonNode node) {
        String path = node.path("jcr:path").asText(node.path("path").asText());
        String id = node.path("rep:authorizableId").asText(
            path.substring(path.lastIndexOf('/') + 1)
        );

        JsonNode profile = node.path("profile");
        String email = profile.path("email").asText(null);
        String givenName = profile.path("givenName").asText(null);
        String familyName = profile.path("familyName").asText(null);
        boolean disabled = node.has("rep:disabled");

        return new User(id, path, email, givenName, familyName, disabled);
    }

    private Group parseGroup(JsonNode node) {
        String path = node.path("jcr:path").asText(node.path("path").asText());
        String id = node.path("rep:authorizableId").asText(
            path.substring(path.lastIndexOf('/') + 1)
        );

        JsonNode profile = node.path("profile");
        String givenName = profile.path("givenName").asText(id);

        // Count members if available
        int memberCount = 0;
        JsonNode members = node.path("members");
        if (members.isArray()) {
            memberCount = members.size();
        }

        return new Group(id, path, givenName, memberCount);
    }

    // ==================== Data Classes ====================

    /**
     * Represents an AEM user.
     */
    public record User(
        String id,
        String path,
        String email,
        String givenName,
        String familyName,
        boolean disabled
    ) {
        public String displayName() {
            if (givenName != null && familyName != null) {
                return givenName + " " + familyName;
            } else if (givenName != null) {
                return givenName;
            }
            return id;
        }

        @Override
        public String toString() {
            return String.format("User[%s, %s%s]",
                id,
                displayName(),
                disabled ? " (disabled)" : "");
        }
    }

    /**
     * Represents an AEM group.
     */
    public record Group(
        String id,
        String path,
        String displayName,
        int memberCount
    ) {
        @Override
        public String toString() {
            return String.format("Group[%s, %s, %d members]", id, displayName, memberCount);
        }
    }
}
