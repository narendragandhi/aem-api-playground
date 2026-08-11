package com.aemtools.aem.gui;

import org.junit.jupiter.api.Test;

import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives the real AEM API Studio window: builds the frame, clicks through every
 * sidebar entry, and asserts the matching CardLayout card is displayed.
 *
 * <p>Sidebar index maps to card index 1:1 because both are driven by the same
 * {@link StudioView} registry, so this test auto-covers any newly added view.</p>
 */
public class AemStudioGuiInteractionTest {

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
                assertTrue(sidebar.getModel().getSize() > 0, "Sidebar is empty");

                JPanel contentPanel = findCardLayoutPanel(frame);
                assertNotNull(contentPanel, "CardLayout content panel not found");

                int count = sidebar.getModel().getSize();
                assertEquals(count, contentPanel.getComponentCount(), "Sidebar/card count mismatch");

                for (int i = 0; i < count; i++) {
                    sidebar.setSelectedIndex(i);
                    assertEquals(sidebar.getModel().getElementAt(i), sidebar.getSelectedValue(),
                        "Sidebar selection failed at index " + i);
                    assertTrue(contentPanel.getComponent(i).isShowing(),
                        "Card at index " + i + " ('" + sidebar.getModel().getElementAt(i) + "') is not showing");

                    for (int j = 0; j < count; j++) {
                        boolean shouldShow = j == i;
                        assertEquals(shouldShow, contentPanel.getComponent(j).isShowing(),
                            "Unexpected visible card when index " + i + " selected (card " + j + ")");
                    }
                }
                System.out.println("All " + count + " sidebar entries switch to the correct card");
            } finally {
                frame.dispose();
            }
        });
    }

    @Test
    public void testDuplicateViewRejected() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("Skipping GUI interaction test in headless environment");
            return;
        }
        SwingUtilities.invokeAndWait(() -> {
            AemStudioGui gui = new AemStudioGui();
            try {
                assertThrows(IllegalArgumentException.class,
                    () -> gui.addView("Home", new JPanel()),
                    "Duplicate label should be rejected");
            } finally {
                disposeAllFrames();
            }
        });
    }

    private static void disposeAllFrames() {
        for (Frame f : Frame.getFrames()) {
            f.dispose();
        }
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
