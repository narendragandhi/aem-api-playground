package com.aemtools.aem;

import com.aemtools.aem.api.TagsApi;
import com.aemtools.aem.api.TagsApi.Tag;
import com.aemtools.aem.api.TagsApi.TagNamespace;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TagsApi data classes and helper methods.
 */
class TagsApiTest {

    @Test
    void testTagRecord() {
        Tag tag = new Tag(
            "marketing:campaigns/summer2024",
            "/content/cq:tags/marketing/campaigns/summer2024",
            "Summer 2024 Campaign",
            "Tags for summer 2024 marketing campaign",
            "marketing",
            3
        );

        assertEquals("marketing:campaigns/summer2024", tag.tagId());
        assertEquals("/content/cq:tags/marketing/campaigns/summer2024", tag.path());
        assertEquals("Summer 2024 Campaign", tag.title());
        assertEquals("Tags for summer 2024 marketing campaign", tag.description());
        assertEquals("marketing", tag.namespace());
        assertEquals(3, tag.childCount());
    }

    @Test
    void testTagToString_withChildren() {
        Tag tag = new Tag("ns:tag", "/path", "My Tag", null, "ns", 5);
        String str = tag.toString();
        assertTrue(str.contains("ns:tag"));
        assertTrue(str.contains("My Tag"));
        assertTrue(str.contains("5 children"));
    }

    @Test
    void testTagToString_noChildren() {
        Tag tag = new Tag("ns:tag", "/path", "My Tag", null, "ns", 0);
        String str = tag.toString();
        assertTrue(str.contains("ns:tag"));
        assertFalse(str.contains("children"));
    }

    @Test
    void testTagNamespaceRecord() {
        TagNamespace ns = new TagNamespace(
            "marketing",
            "/content/cq:tags/marketing",
            "Marketing Tags",
            "Tags for marketing content",
            25
        );

        assertEquals("marketing", ns.id());
        assertEquals("/content/cq:tags/marketing", ns.path());
        assertEquals("Marketing Tags", ns.title());
        assertEquals("Tags for marketing content", ns.description());
        assertEquals(25, ns.tagCount());
    }

    @Test
    void testTagNamespaceToString() {
        TagNamespace ns = new TagNamespace("geo", "/path", "Geography", null, 100);
        String str = ns.toString();
        assertTrue(str.contains("geo"));
        assertTrue(str.contains("Geography"));
        assertTrue(str.contains("100"));
    }

    @Test
    void testTagEquality() {
        Tag tag1 = new Tag("ns:tag", "/path", "Title", "Desc", "ns", 0);
        Tag tag2 = new Tag("ns:tag", "/path", "Title", "Desc", "ns", 0);
        assertEquals(tag1, tag2);
    }

    @Test
    void testTagNamespaceEquality() {
        TagNamespace ns1 = new TagNamespace("ns", "/path", "Title", "Desc", 5);
        TagNamespace ns2 = new TagNamespace("ns", "/path", "Title", "Desc", 5);
        assertEquals(ns1, ns2);
    }

    @Test
    void testTagWithNullDescription() {
        Tag tag = new Tag("ns:tag", "/path", "Title", null, "ns", 0);
        assertNull(tag.description());
    }

    @Test
    void testTagNamespaceWithNullDescription() {
        TagNamespace ns = new TagNamespace("ns", "/path", "Title", null, 0);
        assertNull(ns.description());
    }

    @Test
    void testSimpleTagId() {
        Tag tag = new Tag("products:electronics", "/content/cq:tags/products/electronics", "Electronics", null, "products", 10);
        assertEquals("products", tag.namespace());
        assertEquals("products:electronics", tag.tagId());
    }

    @Test
    void testNestedTagId() {
        Tag tag = new Tag(
            "geo:us/california/los-angeles",
            "/content/cq:tags/geo/us/california/los-angeles",
            "Los Angeles",
            null,
            "geo",
            0
        );
        assertEquals("geo", tag.namespace());
        assertTrue(tag.tagId().contains("california"));
    }

    @Test
    void testEmptyNamespace() {
        TagNamespace ns = new TagNamespace("empty", "/path", "Empty Namespace", null, 0);
        assertEquals(0, ns.tagCount());
    }

    @Test
    void testTagHashCode() {
        Tag tag1 = new Tag("ns:tag", "/path", "Title", null, "ns", 0);
        Tag tag2 = new Tag("ns:tag", "/path", "Title", null, "ns", 0);
        assertEquals(tag1.hashCode(), tag2.hashCode());
    }
}
