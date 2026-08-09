package com.aemtools.aem;

import com.aemtools.aem.api.UsersApi;
import com.aemtools.aem.api.UsersApi.Group;
import com.aemtools.aem.api.UsersApi.User;
import com.aemtools.aem.client.AemApiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsersApiMockTest {

    @Mock
    private AemApiClient mockClient;

    private UsersApi usersApi;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        usersApi = new UsersApi(mockClient);
    }

    private ObjectNode userNode(String path, String id, String email) {
        ObjectNode node = mapper.createObjectNode();
        node.put("jcr:path", path);
        node.put("rep:authorizableId", id);
        ObjectNode profile = mapper.createObjectNode();
        profile.put("email", email);
        profile.put("givenName", "John");
        profile.put("familyName", "Doe");
        node.set("profile", profile);
        return node;
    }

    private ObjectNode groupNode(String path, String id, String givenName) {
        ObjectNode node = mapper.createObjectNode();
        node.put("jcr:path", path);
        node.put("rep:authorizableId", id);
        ObjectNode profile = mapper.createObjectNode();
        profile.put("givenName", givenName);
        node.set("profile", profile);
        return node;
    }

    private ObjectNode searchResponse(ArrayNode hits) {
        ObjectNode response = mapper.createObjectNode();
        response.set("hits", hits);
        return response;
    }

    @Test
    void testListUsers() throws IOException {
        ArrayNode hits = mapper.createArrayNode();
        hits.add(userNode("/home/users/j/john", "john", "john@example.com"));
        hits.add(userNode("/home/users/j/jane", "jane", "jane@example.com"));
        when(mockClient.get(contains("type=rep:User"))).thenReturn(searchResponse(hits));

        List<User> users = usersApi.listUsers("/home/users", 10);

        assertEquals(2, users.size());
        assertEquals("john", users.get(0).id());
        assertEquals("john@example.com", users.get(0).email());
    }

    @Test
    void testGetUser() throws IOException {
        ArrayNode hits = mapper.createArrayNode();
        hits.add(userNode("/home/users/j/john", "john", "john@example.com"));
        when(mockClient.get(contains("property.value=john"))).thenReturn(searchResponse(hits));

        User user = usersApi.getUser("john");

        assertEquals("john", user.id());
        assertEquals("John", user.givenName());
    }

    @Test
    void testGetUserNotFound() throws IOException {
        when(mockClient.get(anyString())).thenReturn(searchResponse(mapper.createArrayNode()));

        assertThrows(IOException.class, () -> usersApi.getUser("ghost"));
    }

    @Test
    void testCreateUser() throws IOException {
        ArrayNode hits = mapper.createArrayNode();
        hits.add(userNode("/home/users/j/john", "john", "john@example.com"));
        when(mockClient.postForm(contains("authorizables"), any())).thenReturn(new AemApiClient.RawResponse(201, "ok"));
        when(mockClient.get(contains("property.value=john"))).thenReturn(searchResponse(hits));

        User user = usersApi.createUser("john", "secret", "john@example.com", "John", "Doe");

        assertEquals("john", user.id());
        verify(mockClient).postForm(eq("/libs/granite/security/post/authorizables"), any());
    }

    @Test
    void testDeleteUser() throws IOException {
        ArrayNode hits = mapper.createArrayNode();
        hits.add(userNode("/home/users/j/john", "john", null));
        when(mockClient.get(anyString())).thenReturn(searchResponse(hits));
        when(mockClient.postForm(anyString(), any())).thenReturn(new AemApiClient.RawResponse(201, "ok"));

        assertTrue(usersApi.deleteUser("john"));
        verify(mockClient).postForm(eq("/home/users/j/john"), any());
    }

    @Test
    void testUpdateUser() throws IOException {
        ArrayNode hits = mapper.createArrayNode();
        hits.add(userNode("/home/users/j/john", "john", null));
        when(mockClient.get(anyString())).thenReturn(searchResponse(hits));
        when(mockClient.postForm(anyString(), any())).thenReturn(new AemApiClient.RawResponse(201, "ok"));

        User user = usersApi.updateUser("john", Map.of("email", "new@example.com"));

        assertEquals("john", user.id());
        verify(mockClient).postForm(eq("/home/users/j/john"), any());
    }

    @Test
    void testChangePassword() throws IOException {
        ArrayNode hits = mapper.createArrayNode();
        hits.add(userNode("/home/users/j/john", "john", null));
        when(mockClient.get(anyString())).thenReturn(searchResponse(hits));
        when(mockClient.postForm(anyString(), any())).thenReturn(new AemApiClient.RawResponse(201, "ok"));

        assertTrue(usersApi.changePassword("john", "old", "new"));
        verify(mockClient).postForm(eq("/home/users/j/john.rw.userprops.html"), any());
    }

    @Test
    void testSetUserEnabledDisablesAccount() throws IOException {
        ArrayNode hits = mapper.createArrayNode();
        hits.add(userNode("/home/users/j/john", "john", null));
        when(mockClient.get(anyString())).thenReturn(searchResponse(hits));
        when(mockClient.postForm(anyString(), any())).thenReturn(new AemApiClient.RawResponse(201, "ok"));

        assertTrue(usersApi.setUserEnabled("john", false));
        verify(mockClient).postForm(eq("/home/users/j/john"), any());
    }

    @Test
    void testListGroups() throws IOException {
        ArrayNode hits = mapper.createArrayNode();
        hits.add(groupNode("/home/groups/e/editors", "editors", "Editors"));
        when(mockClient.get(contains("type=rep:Group"))).thenReturn(searchResponse(hits));

        List<Group> groups = usersApi.listGroups("/home/groups", 10);

        assertEquals(1, groups.size());
        assertEquals("editors", groups.get(0).id());
        assertEquals("Editors", groups.get(0).displayName());
    }

    @Test
    void testGetGroupNotFound() throws IOException {
        when(mockClient.get(anyString())).thenReturn(searchResponse(mapper.createArrayNode()));

        assertThrows(IOException.class, () -> usersApi.getGroup("ghost"));
    }

    @Test
    void testCreateGroup() throws IOException {
        ArrayNode hits = mapper.createArrayNode();
        hits.add(groupNode("/home/groups/e/editors", "editors", "Editors"));
        when(mockClient.postForm(contains("authorizables"), any())).thenReturn(new AemApiClient.RawResponse(201, "ok"));
        when(mockClient.get(contains("property.value=editors"))).thenReturn(searchResponse(hits));

        Group group = usersApi.createGroup("editors", "Editors");

        assertEquals("editors", group.id());
    }

    @Test
    void testDeleteGroup() throws IOException {
        ArrayNode hits = mapper.createArrayNode();
        hits.add(groupNode("/home/groups/e/editors", "editors", "Editors"));
        when(mockClient.get(anyString())).thenReturn(searchResponse(hits));
        when(mockClient.postForm(anyString(), any())).thenReturn(new AemApiClient.RawResponse(201, "ok"));

        assertTrue(usersApi.deleteGroup("editors"));
        verify(mockClient).postForm(eq("/home/groups/e/editors"), any());
    }

    @Test
    void testGetUserGroups() throws IOException {
        ArrayNode userHits = mapper.createArrayNode();
        userHits.add(userNode("/home/users/j/john", "john", null));
        when(mockClient.get(contains("property.value=john"))).thenReturn(searchResponse(userHits));

        ObjectNode groups = mapper.createObjectNode();
        ArrayNode memberOf = mapper.createArrayNode();
        memberOf.add("/home/groups/e/editors");
        memberOf.add("/home/groups/c/content-authors");
        groups.set("memberOf", memberOf);
        when(mockClient.get(eq("/home/users/j/john.rw.json?props=memberOf"))).thenReturn(groups);

        List<String> result = usersApi.getUserGroups("john");

        assertEquals(2, result.size());
        assertTrue(result.contains("editors"));
    }

    @Test
    void testGetGroupMembers() throws IOException {
        ArrayNode groupHits = mapper.createArrayNode();
        groupHits.add(groupNode("/home/groups/e/editors", "editors", "Editors"));
        when(mockClient.get(contains("property.value=editors"))).thenReturn(searchResponse(groupHits));

        ObjectNode members = mapper.createObjectNode();
        ArrayNode memberList = mapper.createArrayNode();
        memberList.add("/home/users/j/john");
        members.set("members", memberList);
        when(mockClient.get(eq("/home/groups/e/editors.rw.json?props=members"))).thenReturn(members);

        List<String> result = usersApi.getGroupMembers("editors");

        assertEquals(1, result.size());
        assertEquals("john", result.get(0));
    }

    @Test
    void testAddUserToGroup() throws IOException {
        ArrayNode groupHits = mapper.createArrayNode();
        groupHits.add(groupNode("/home/groups/e/editors", "editors", "Editors"));
        ArrayNode userHits = mapper.createArrayNode();
        userHits.add(userNode("/home/users/j/john", "john", null));
        when(mockClient.get(anyString())).thenReturn(searchResponse(groupHits), searchResponse(userHits));
        when(mockClient.postForm(anyString(), any())).thenReturn(new AemApiClient.RawResponse(201, "ok"));

        assertTrue(usersApi.addUserToGroup("john", "editors"));
        verify(mockClient).postForm(eq("/home/groups/e/editors"), any());
    }

    @Test
    void testRemoveUserFromGroup() throws IOException {
        ArrayNode groupHits = mapper.createArrayNode();
        groupHits.add(groupNode("/home/groups/e/editors", "editors", "Editors"));
        ArrayNode userHits = mapper.createArrayNode();
        userHits.add(userNode("/home/users/j/john", "john", null));
        when(mockClient.get(anyString())).thenReturn(searchResponse(groupHits), searchResponse(userHits));
        when(mockClient.postForm(anyString(), any())).thenReturn(new AemApiClient.RawResponse(201, "ok"));

        assertTrue(usersApi.removeUserFromGroup("john", "editors"));
        verify(mockClient).postForm(eq("/home/groups/e/editors"), any());
    }

    @Test
    void testSetUserGroupsAddsAndRemoves() throws IOException {
        ArrayNode userHits = mapper.createArrayNode();
        userHits.add(userNode("/home/users/j/john", "john", null));
        when(mockClient.get(contains("property.value=john"))).thenReturn(searchResponse(userHits));

        ObjectNode groups = mapper.createObjectNode();
        ArrayNode memberOf = mapper.createArrayNode();
        memberOf.add("/home/groups/o/old-group");
        memberOf.add("/home/groups/s/stay-group");
        groups.set("memberOf", memberOf);
        when(mockClient.get(eq("/home/users/j/john.rw.json?props=memberOf"))).thenReturn(groups);

        ArrayNode oldGroupHits = mapper.createArrayNode();
        oldGroupHits.add(groupNode("/home/groups/o/old-group", "old-group", "Old Group"));
        ArrayNode newGroupHits = mapper.createArrayNode();
        newGroupHits.add(groupNode("/home/groups/n/new-group", "new-group", "New Group"));
        when(mockClient.get(contains("property.value=old-group"))).thenReturn(searchResponse(oldGroupHits));
        when(mockClient.get(contains("property.value=new-group"))).thenReturn(searchResponse(newGroupHits));
        when(mockClient.postForm(anyString(), any())).thenReturn(new AemApiClient.RawResponse(201, "ok"));

        assertTrue(usersApi.setUserGroups("john", List.of("stay-group", "new-group")));
        verify(mockClient).postForm(eq("/home/groups/o/old-group"), any());
        verify(mockClient).postForm(eq("/home/groups/n/new-group"), any());
    }

    @Test
    void testGetImpersonators() throws IOException {
        ArrayNode userHits = mapper.createArrayNode();
        userHits.add(userNode("/home/users/j/john", "john", null));
        when(mockClient.get(contains("property.value=john"))).thenReturn(searchResponse(userHits));

        ObjectNode response = mapper.createObjectNode();
        ArrayNode impersonators = mapper.createArrayNode();
        impersonators.add("/home/users/a/admin");
        response.set("impersonators", impersonators);
        when(mockClient.get(eq("/home/users/j/john.rw.json?props=impersonators")))
            .thenReturn(response);

        List<String> result = usersApi.getImpersonators("john");

        assertEquals(1, result.size());
        assertEquals("admin", result.get(0));
    }

    @Test
    void testAllowImpersonation() throws IOException {
        ArrayNode userHits = mapper.createArrayNode();
        userHits.add(userNode("/home/users/j/john", "john", null));
        when(mockClient.get(contains("property.value=john"))).thenReturn(searchResponse(userHits));
        when(mockClient.postForm(anyString(), any())).thenReturn(new AemApiClient.RawResponse(201, "ok"));

        assertTrue(usersApi.allowImpersonation("admin", "john"));
        verify(mockClient).postForm(eq("/home/users/j/john"), any());
    }

    @Test
    void testRevokeImpersonation() throws IOException {
        ArrayNode userHits = mapper.createArrayNode();
        userHits.add(userNode("/home/users/j/john", "john", null));
        when(mockClient.get(contains("property.value=john"))).thenReturn(searchResponse(userHits));
        when(mockClient.postForm(anyString(), any())).thenReturn(new AemApiClient.RawResponse(201, "ok"));

        assertTrue(usersApi.revokeImpersonation("admin", "john"));
        verify(mockClient).postForm(eq("/home/users/j/john"), any());
    }
}
