package com.aemtools.aem.gui;

import javax.swing.JPanel;

/**
 * A navigable view in the AEM API Studio GUI: the sidebar label, the
 * CardLayout identifier, and the panel shown when the entry is selected.
 */
public record StudioView(String label, String id, JPanel panel) {
}
