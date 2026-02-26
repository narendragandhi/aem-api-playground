package com.aemtools.aem;

import com.aemtools.aem.gui.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

import javax.swing.*;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class GuiInitializationTest {

    @Test
    public void testGuiComponentsInitialize() {
        // Skip if truly headless environment without any graphics support
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("Skipping GUI test in headless environment");
            return;
        }

        try {
            // Test individual panels
            assertNotNull(new EnvPanel(), "EnvPanel should initialize");
            assertNotNull(new ContentBrowserPanel(), "ContentBrowserPanel should initialize");
            assertNotNull(new AgentPanel(), "AgentPanel should initialize");
            assertNotNull(new GraphQLPanel(), "GraphQLPanel should initialize");
            assertNotNull(new WorkflowPanel(), "WorkflowPanel should initialize");
            assertNotNull(new AuditPanel(), "AuditPanel should initialize");
            assertNotNull(new HomePanel(), "HomePanel should initialize");
            assertNotNull(new RecipePanel(), "RecipePanel should initialize");
            assertNotNull(new ConsolePanel(), "ConsolePanel should initialize");
            
            System.out.println("All GUI panels initialized successfully!");
        } catch (HeadlessException e) {
            System.out.println("HeadlessException caught - expected in some CI environments");
        }
    }
}
