package com.aemtools.aem.gui;

import com.aemtools.aem.CliFlags;
import com.aemtools.aem.api.AssetsApi;
import com.aemtools.aem.client.AemApiClient;
import com.aemtools.aem.util.MockDataHelper;
import com.fasterxml.jackson.databind.JsonNode;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.List;

public class ContentBrowserPanel extends JPanel {

    private JTree folderTree;
    private JTable contentTable;
    private DefaultTableModel tableModel;
    private JEditorPane detailsPane;
    private String currentPath = "/content/dam";

    public ContentBrowserPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Tree
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("/content/dam");
        folderTree = new JTree(root);
        folderTree.getSelectionModel().setSelectionMode(javax.swing.tree.TreeSelectionModel.SINGLE_TREE_SELECTION);
        folderTree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) folderTree.getLastSelectedPathComponent();
            if (node != null) {
                currentPath = (String) node.getUserObject();
                refreshContent();
            }
        });
        
        JScrollPane treeScroll = new JScrollPane(folderTree);
        treeScroll.setPreferredSize(new Dimension(250, 0));
        
        // Table
        String[] columns = {"Name", "Title", "Type", "Path"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        contentTable = new JTable(tableModel);
        contentTable.setRowHeight(30);
        contentTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showItemDetails();
            }
        });
        JScrollPane tableScroll = new JScrollPane(contentTable);
        
        // Details Pane
        detailsPane = new JEditorPane();
        detailsPane.setEditable(false);
        detailsPane.setContentType("text/html");
        detailsPane.setBackground(new Color(45, 45, 45));
        JScrollPane detailsScroll = new JScrollPane(detailsPane);
        detailsScroll.setPreferredSize(new Dimension(300, 0));
        detailsScroll.setBorder(BorderFactory.createTitledBorder("Properties"));

        // Layout: Tree | Table | Details
        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableScroll, detailsScroll);
        centerSplit.setDividerLocation(500);
        
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, centerSplit);
        mainSplit.setDividerLocation(250);
        
        add(mainSplit, BorderLayout.CENTER);
        
        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshContent());
        toolbar.add(refreshBtn);
        
        JButton deleteBtn = new JButton("Delete");
        deleteBtn.setForeground(Color.RED);
        toolbar.add(deleteBtn);
        
        JButton publishBtn = new JButton("Publish");
        publishBtn.setBackground(new Color(0, 100, 0));
        toolbar.add(publishBtn);
        
        add(toolbar, BorderLayout.NORTH);
        
        refreshContent();
    }

    private void showItemDetails() {
        int row = contentTable.getSelectedRow();
        if (row < 0) {
            detailsPane.setText("<html><body style='color:white;padding:10px;'>Select an item to view properties</body></html>");
            return;
        }

        String name = (String) tableModel.getValueAt(row, 0);
        String title = (String) tableModel.getValueAt(row, 1);
        String type = (String) tableModel.getValueAt(row, 2);
        String path = (String) tableModel.getValueAt(row, 3);

        StringBuilder html = new StringBuilder();
        html.append("<html><body style='color:white; font-family:sans-serif; padding:10px;'>");
        html.append("<h2 style='color:#ff3333;'>").append(name).append("</h2>");
        html.append("<table border='0' cellspacing='5'>");
        html.append("<tr><td><b>Title:</b></td><td>").append(title).append("</td></tr>");
        html.append("<tr><td><b>Type:</b></td><td>").append(type).append("</td></tr>");
        html.append("<tr><td><b>Path:</b></td><td>").append(path).append("</td></tr>");
        html.append("<tr><td><b>Created:</b></td><td>2026-02-20</td></tr>");
        html.append("<tr><td><b>Last Modified:</b></td><td>2026-02-25</td></tr>");
        html.append("<tr><td><b>Modified By:</b></td><td>admin</td></tr>");
        html.append("</table>");
        
        if ("Asset".equals(type)) {
            html.append("<br><h3>Metadata</h3>");
            html.append("<ul><li>dc:format: image/jpeg</li><li>dam:size: 1.2MB</li><li>tiff:ImageWidth: 1920</li></ul>");
        }
        
        html.append("</body></html>");
        detailsPane.setText(html.toString());
    }

    private void refreshContent() {
        tableModel.setRowCount(0);
        
        if (CliFlags.mockMode) {
            JsonNode assets = MockDataHelper.getAssets();
            for (JsonNode asset : assets) {
                tableModel.addRow(new Object[]{
                    asset.path("name").asText(),
                    asset.path("title").asText(),
                    "Asset",
                    asset.path("path").asText()
                });
            }
            
            JsonNode fragments = MockDataHelper.getContentFragments();
            for (JsonNode cf : fragments) {
                tableModel.addRow(new Object[]{
                    cf.path("name").asText(),
                    cf.path("title").asText(),
                    "Content Fragment",
                    cf.path("path").asText()
                });
            }
        } else {
            // Real API call
            try {
                AemApiClient client = new AemApiClient();
                AssetsApi assetsApi = new AssetsApi(client);
                List<AssetsApi.Asset> assets = assetsApi.list(currentPath, 20);
                for (AssetsApi.Asset asset : assets) {
                    tableModel.addRow(new Object[]{
                        asset.getName(),
                        asset.getTitle(),
                        "Asset",
                        asset.getPath()
                    });
                }
            } catch (Exception e) {
                System.err.println("Error loading content: " + e.getMessage());
                // Fallback to mock in dev
                tableModel.addRow(new Object[]{"Connect Error", e.getMessage(), "ERROR", ""});
            }
        }
    }
}
