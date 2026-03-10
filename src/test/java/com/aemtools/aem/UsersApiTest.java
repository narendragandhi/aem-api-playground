package com.aemtools.aem;

import com.aemtools.aem.api.UsersApi;
import com.aemtools.aem.api.UsersApi.User;
import com.aemtools.aem.api.UsersApi.Group;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for UsersApi data classes and helper methods.
 */
class UsersApiTest {

    @Test
    void testUserRecord() {
        User user = new User(
            "john.doe",
            "/home/users/j/john.doe",
            "john@example.com",
            "John",
            "Doe",
            false
        );

        assertEquals("john.doe", user.id());
        assertEquals("/home/users/j/john.doe", user.path());
        assertEquals("john@example.com", user.email());
        assertEquals("John", user.givenName());
        assertEquals("Doe", user.familyName());
        assertFalse(user.disabled());
    }

    @Test
    void testUserDisplayName_fullName() {
        User user = new User("john", "/home/users/j/john", null, "John", "Doe", false);
        assertEquals("John Doe", user.displayName());
    }

    @Test
    void testUserDisplayName_givenNameOnly() {
        User user = new User("john", "/home/users/j/john", null, "John", null, false);
        assertEquals("John", user.displayName());
    }

    @Test
    void testUserDisplayName_fallbackToId() {
        User user = new User("john", "/home/users/j/john", null, null, null, false);
        assertEquals("john", user.displayName());
    }

    @Test
    void testUserToString_active() {
        User user = new User("john", "/home/users/j/john", null, "John", "Doe", false);
        String str = user.toString();
        assertTrue(str.contains("john"));
        assertTrue(str.contains("John Doe"));
        assertFalse(str.contains("disabled"));
    }

    @Test
    void testUserToString_disabled() {
        User user = new User("john", "/home/users/j/john", null, "John", null, true);
        String str = user.toString();
        assertTrue(str.contains("disabled"));
    }

    @Test
    void testGroupRecord() {
        Group group = new Group(
            "administrators",
            "/home/groups/a/administrators",
            "Administrators",
            5
        );

        assertEquals("administrators", group.id());
        assertEquals("/home/groups/a/administrators", group.path());
        assertEquals("Administrators", group.displayName());
        assertEquals(5, group.memberCount());
    }

    @Test
    void testGroupToString() {
        Group group = new Group("editors", "/home/groups/e/editors", "Content Editors", 10);
        String str = group.toString();
        assertTrue(str.contains("editors"));
        assertTrue(str.contains("Content Editors"));
        assertTrue(str.contains("10"));
    }

    @Test
    void testGroupEquality() {
        Group group1 = new Group("test", "/home/groups/t/test", "Test Group", 3);
        Group group2 = new Group("test", "/home/groups/t/test", "Test Group", 3);
        assertEquals(group1, group2);
    }

    @Test
    void testUserEquality() {
        User user1 = new User("test", "/path", "test@test.com", "Test", "User", false);
        User user2 = new User("test", "/path", "test@test.com", "Test", "User", false);
        assertEquals(user1, user2);
    }

    @Test
    void testUserWithNullEmail() {
        User user = new User("test", "/path", null, "Test", "User", false);
        assertNull(user.email());
    }

    @Test
    void testGroupWithZeroMembers() {
        Group group = new Group("empty", "/home/groups/e/empty", "Empty Group", 0);
        assertEquals(0, group.memberCount());
        assertTrue(group.toString().contains("0"));
    }
}
