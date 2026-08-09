package com.aemtools.aem.gui;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives the real AEM API Studio window: builds the frame, clicks through every
 * sidebar entry, and asserts the corresponding CardLayout card is displayed.
 */
public class AemStudioGuiInteractionTest {

    private static final String[] SIDEBAR = {
        "Home", "Environments", "Content Browser", "Sites & Pages", "Package Manager",
        "GraphQL Editor", "Workflow Monitor", "Automation Recipes", "AI Agent", "Audit & Cache"
    };

    private static final int[] SIDEBAR_TO_CARD = {0, 1, 2, 3, 4, 6, 7, 9, 5, 8};

    @Test
    public void testSidebarDrivesAllCards() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("Skipping GUI interaction test in headless environment");
            return;
        }

        SwingUtilities.invokeAndWait(() -> {
            AemStudioGui gui = new AemStudioGui();
            JFrame frame = findFrame(gui);
            try {
                gui.show();
                frame.validate();

                JList<String> sidebar = findSidebarList(frame);
                assertNotNull(sidebar, "Sidebar list not found");
                assertEquals(SIDEBAR.length, sidebar.getModel().getSize(), "Sidebar entry count mismatch");

                JPanel contentPanel = findCardLayoutPanel(frame);
                assertNotNull(contentPanel, "CardLayout content panel not found");
                assertEquals(10, contentPanel.getComponentCount(), "Card count mismatch");

                for (int i = 0; i < SIDEBAR.length; i++) {
                    sidebar.setSelectedIndex(i);
                    assertEquals(SIDEBAR[i], sidebar.getSelectedValue(), "Sidebar selection failed at index " + i);

                    Component expectedCard = contentPanel.getComponent(SIDEBAR_TO_CARD[i]);
                    assertTrue(expectedCard.isShowing(), "Card for '" + SIDEBAR[i] + "' is not showing");

                    for (int j = 0; j < contentPanel.getComponentCount(); j++) {
                        boolean shouldShow = j == SIDEBAR_TO_CARD[i];
                        assertEquals(shouldShow, contentPanel.getComponent(j).isShowing(),
                            "Unexpected visible card when '" + SIDEBAR[i] + "' selected (index " + j + ")");
                    }
                }
                System.out.println("All " + SIDEBAR.length + " sidebar entries switch to the correct card");
            } finally {
                frame.dispose();
            }
        });
    }

    private static JFrame findFrame(AemStudioGui gui) {
        for (Frame f : Frame.getFrames()) {
            if (f instanceof JFrame) {
                return (JFrame) f;
            }
        }
        throw new IllegalStateException("No JFrame registered by AemStudioGui");
    }

    private static JList<String> findSidebarList(Container root) {
        return (JList<String>) findComponent(root, c -> {
            if (!(c instanceof JList)) {
                return false;
            }
            ListModel<?> model = ((JList<?>) c).getModel();
            if (model.getSize() < 5) {
                return false;
            }
            boolean hasSites = false;
            boolean hasPackages = false;
            for (int i = 0; i < model.getSize(); i++) {
                Object el = model.getElementAt(i);
                if ("Sites & Pages".equals(el)) {
                    hasSites = true;
                }
                if ("Package Manager".equals(el)) {
                    hasPackages = true;
                }
            }
            return hasSites && hasPackages;
        });
    }

    private static JPanel findCardLayoutPanel(Container root) {
        return (JPanel) findComponent(root, c ->
            c instanceof JPanel p && p.getLayout() instanceof CardLayout && p.getComponentCount() >= 10);
    }

    private static Component findComponent(Container root, Predicate<Component> predicate) {
        for (Component c : root.getComponents()) {
            if (predicate.test(c)) {
                return c;
            }
            if (c instanceof Container) {
                Component found = findComponent((Container) c, predicate);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
