package com.aemtools.aem.gui;

import com.aemtools.aem.config.ConfigManager;
import com.formdev.flatlaf.FlatDarkLaf;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * AEM API Studio - Native Java GUI for AEM API Playground.
 */
public class AemStudioGui {

    private JFrame frame;
    private JPanel contentPanel;
    private CardLayout cardLayout;
    private DefaultListModel<String> sidebarModel;
    private ConsolePanel consolePanel;
    private final List<StudioView> views = new ArrayList<>();

    public AemStudioGui() {
        initComponents();
    }

    private void initComponents() {
        frame = new JFrame("AEM API Studio v1.0.0");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(1280, 900);
        frame.setMinimumSize(new Dimension(1024, 768));
        frame.setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        sidebarModel = new DefaultListModel<>();

        registerViews();

        JPanel sidebar = createSidebar();

        JSplitPane mainHorizontalSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebar, contentPanel);
        mainHorizontalSplit.setDividerLocation(220);
        mainHorizontalSplit.setContinuousLayout(true);

        consolePanel = new ConsolePanel();
        consolePanel.setPreferredSize(new Dimension(0, 200));

        JSplitPane mainVerticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mainHorizontalSplit, consolePanel);
        mainVerticalSplit.setDividerLocation(frame.getHeight() - 250);
        mainVerticalSplit.setResizeWeight(1.0);

        frame.add(mainVerticalSplit, BorderLayout.CENTER);

        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        JLabel statusLabel = new JLabel("Studio Ready");
        statusBar.add(statusLabel, BorderLayout.WEST);

        String activeEnv = ConfigManager.getInstance().getActiveEnvironment();
        JLabel envLabel = new JLabel("Active Environment: " + (activeEnv != null ? activeEnv : "None"));
        statusBar.add(envLabel, BorderLayout.EAST);

        frame.add(statusBar, BorderLayout.SOUTH);
    }

    /**
     * Registers all built-in views. Sidebar order equals card order.
     */
    private void registerViews() {
        addView("Home", new HomePanel());
        addView("Environments", new EnvPanel());
        addView("Content Browser", new ContentBrowserPanel());
        addView("Sites & Pages", new SitesPanel());
        addView("Package Manager", new PackageManagerPanel());
        addView("GraphQL Editor", new GraphQLPanel());
        addView("Workflow Monitor", new WorkflowPanel());
        addView("Automation Recipes", new RecipePanel());
        addView("AI Agent", new AgentPanel());
        addView("Audit & Cache", new AuditPanel());
    }

    /**
     * Registers a new view into the sidebar and card layout. Call before
     * {@link #show()} so the window picks up the new entry.
     *
     * @param label label shown in the sidebar
     * @param panel panel shown when the entry is selected
     */
    public void addView(String label, JPanel panel) {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(panel, "panel");
        String id = toCardId(label);
        views.add(new StudioView(label, id, panel));
        contentPanel.add(panel, id);
        sidebarModel.addElement(label);
    }

    private static String toCardId(String label) {
        return label.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(new Color(40, 40, 40));

        JList<String> list = new JList<>(sidebarModel);
        list.setBackground(new Color(40, 40, 40));
        list.setForeground(Color.WHITE);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        list.setFont(new Font("SansSerif", Font.BOLD, 14));
        list.setFixedCellHeight(45);

        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int index = list.getSelectedIndex();
                if (index >= 0 && index < views.size()) {
                    cardLayout.show(contentPanel, views.get(index).id());
                }
            }
        });

        sidebar.add(new JScrollPane(list), BorderLayout.CENTER);

        JLabel logo = new JLabel(" AEM STUDIO", SwingConstants.CENTER);
        logo.setFont(new Font("SansSerif", Font.BOLD, 18));
        logo.setForeground(new Color(230, 0, 0));
        logo.setPreferredSize(new Dimension(200, 60));
        sidebar.add(logo, BorderLayout.NORTH);

        return sidebar;
    }

    public void show() {
        frame.setVisible(true);
    }

    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            try {
                FlatDarkLaf.setup();
                AemStudioGui gui = new AemStudioGui();
                gui.show();
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error launching GUI: " + e.getMessage());
            }
        });
    }
}
