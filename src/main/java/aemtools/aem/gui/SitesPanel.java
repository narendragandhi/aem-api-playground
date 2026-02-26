package com.aemtools.aem.gui;

import com.aemtools.aem.CliFlags;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class SitesPanel extends JPanel {

    private JTable sitesTable;
    private DefaultTableModel tableModel;

    public SitesPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Sites & Pages");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        add(title, BorderLayout.NORTH);

        String[] columns = {"Name", "Template", "Language", "Path", "Status"};
        tableModel = new DefaultTableModel(columns, 0);
        sitesTable = new JTable(tableModel);
        sitesTable.setRowHeight(30);
        
        add(new JScrollPane(sitesTable), BorderLayout.CENTER);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton createBtn = new JButton("Create Page");
        JButton rolloutBtn = new JButton("Rollout");
        JButton refreshBtn = new JButton("Refresh");
        
        refreshBtn.addActionListener(e -> refreshSites());
        
        toolbar.add(refreshBtn);
        toolbar.add(createBtn);
        toolbar.add(rolloutBtn);
        
        add(toolbar, BorderLayout.SOUTH);
        
        refreshSites();
    }

    private void refreshSites() {
        tableModel.setRowCount(0);
        if (CliFlags.mockMode) {
            tableModel.addRow(new Object[]{"WKND Site", "Home Page", "en", "/content/wknd", "PUBLISHED"});
            tableModel.addRow(new Object[]{"WKND Site", "Adventures", "en", "/content/wknd/adventures", "MODIFIED"});
            tableModel.addRow(new Object[]{"Retail", "Products", "de", "/content/retail/de/products", "DRAFT"});
        } else {
            tableModel.addRow(new Object[]{"N/A", "Sites API not live", "Use --mock", "", "-"});
        }
    }
}
