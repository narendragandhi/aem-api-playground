package com.aemtools.aem.gui;

import com.aemtools.aem.config.ConfigManager;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * AEM API Studio - Native Java GUI for AEM API Playground.
 */
public class AemStudioGui {

    private JFrame frame;
    private JPanel contentPanel;
    private CardLayout cardLayout;
    
    private HomePanel homePanel;
    private EnvPanel envPanel;
    private ContentBrowserPanel contentBrowserPanel;
    private SitesPanel sitesPanel;
    private PackageManagerPanel packageManagerPanel;
    private AgentPanel agentPanel;
    private GraphQLPanel graphQLPanel;
    private WorkflowPanel workflowPanel;
    private AuditPanel auditPanel;
    private RecipePanel recipePanel;
    private ConsolePanel consolePanel;

    public AemStudioGui() {
        initComponents();
    }

    private void initComponents() {
        frame = new JFrame("AEM API Studio v1.0.0");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(1280, 900);
        frame.setMinimumSize(new Dimension(1024, 768));
        frame.setLocationRelativeTo(null);

        // Sidebar
        JPanel sidebar = createSidebar();
        
        // Content Area
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        
        homePanel = new HomePanel();
        envPanel = new EnvPanel();
        contentBrowserPanel = new ContentBrowserPanel();
        sitesPanel = new SitesPanel();
        packageManagerPanel = new PackageManagerPanel();
        agentPanel = new AgentPanel();
        graphQLPanel = new GraphQLPanel();
        workflowPanel = new WorkflowPanel();
        auditPanel = new AuditPanel();
        recipePanel = new RecipePanel();
        
        contentPanel.add(homePanel, "HOME");
        contentPanel.add(envPanel, "ENV");
        contentPanel.add(contentBrowserPanel, "BROWSER");
        contentPanel.add(sitesPanel, "SITES");
        contentPanel.add(packageManagerPanel, "PACKAGES");
        contentPanel.add(agentPanel, "AGENT");
        contentPanel.add(graphQLPanel, "GRAPHQL");
        contentPanel.add(workflowPanel, "WORKFLOW");
        contentPanel.add(auditPanel, "AUDIT");
        contentPanel.add(recipePanel, "RECIPE");

        // Console at bottom
        consolePanel = new ConsolePanel();
        consolePanel.setPreferredSize(new Dimension(0, 200));
        
        // Split: (Sidebar | Content) over Console
        JSplitPane mainHorizontalSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebar, contentPanel);
        mainHorizontalSplit.setDividerLocation(220);
        mainHorizontalSplit.setContinuousLayout(true);
        
        JSplitPane mainVerticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mainHorizontalSplit, consolePanel);
        mainVerticalSplit.setDividerLocation(frame.getHeight() - 250);
        mainVerticalSplit.setResizeWeight(1.0);
        
        frame.add(mainVerticalSplit, BorderLayout.CENTER);
        
        // Status Bar
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        JLabel statusLabel = new JLabel("Studio Ready");
        statusBar.add(statusLabel, BorderLayout.WEST);
        
        String activeEnv = ConfigManager.getInstance().getActiveEnvironment();
        JLabel envLabel = new JLabel("Active Environment: " + (activeEnv != null ? activeEnv : "None"));
        statusBar.add(envLabel, BorderLayout.EAST);
        
        frame.add(statusBar, BorderLayout.SOUTH);
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(new Color(40, 40, 40));
        
        DefaultListModel<String> model = new DefaultListModel<>();
        model.addElement("Home");
        model.addElement("Environments");
        model.addElement("Content Browser");
        model.addElement("Sites & Pages");
        model.addElement("Package Manager");
        model.addElement("GraphQL Editor");
        model.addElement("Workflow Monitor");
        model.addElement("Automation Recipes");
        model.addElement("AI Agent");
        model.addElement("Audit & Cache");
        
        JList<String> list = new JList<>(model);
        list.setBackground(new Color(40, 40, 40));
        list.setForeground(Color.WHITE);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        list.setFont(new Font("SansSerif", Font.BOLD, 14));
        list.setFixedCellHeight(45);
        
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selection = list.getSelectedValue();
                if ("Home".equals(selection)) {
                    cardLayout.show(contentPanel, "HOME");
                } else if ("Environments".equals(selection)) {
                    cardLayout.show(contentPanel, "ENV");
                } else if ("Content Browser".equals(selection)) {
                    cardLayout.show(contentPanel, "BROWSER");
                } else if ("Sites & Pages".equals(selection)) {
                    cardLayout.show(contentPanel, "SITES");
                } else if ("Package Manager".equals(selection)) {
                    cardLayout.show(contentPanel, "PACKAGES");
                } else if ("GraphQL Editor".equals(selection)) {
                    cardLayout.show(contentPanel, "GRAPHQL");
                } else if ("Workflow Monitor".equals(selection)) {
                    cardLayout.show(contentPanel, "WORKFLOW");
                } else if ("Automation Recipes".equals(selection)) {
                    cardLayout.show(contentPanel, "RECIPE");
                } else if ("AI Agent".equals(selection)) {
                    cardLayout.show(contentPanel, "AGENT");
                } else if ("Audit & Cache".equals(selection)) {
                    cardLayout.show(contentPanel, "AUDIT");
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
                // Set Look and Feel
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
