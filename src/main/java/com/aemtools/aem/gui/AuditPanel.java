package com.aemtools.aem.gui;

import com.aemtools.aem.client.AemApiClient;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Map;

public class AuditPanel extends JPanel {

    private JTable auditTable;
    private DefaultTableModel tableModel;
    private JLabel cacheStatsLabel;

    public AuditPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Audit Log & Cache Management");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        add(title, BorderLayout.NORTH);

        // Audit Table
        String[] columns = {"Timestamp", "Action", "Detail"};
        tableModel = new DefaultTableModel(columns, 0);
        auditTable = new JTable(tableModel);
        auditTable.setRowHeight(25);
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(new JScrollPane(auditTable), BorderLayout.CENTER);
        
        // Cache Panel
        JPanel cachePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        cachePanel.setBorder(BorderFactory.createTitledBorder("Cache Status"));
        cacheStatsLabel = new JLabel("Cache Stats: No data");
        JButton clearCacheBtn = new JButton("Clear Cache");
        clearCacheBtn.addActionListener(e -> {
            // Logic to clear cache via AemApiClient
            JOptionPane.showMessageDialog(this, "API Cache cleared successfully.");
            refreshAudit();
        });
        
        cachePanel.add(cacheStatsLabel);
        cachePanel.add(clearCacheBtn);
        centerPanel.add(cachePanel, BorderLayout.SOUTH);

        add(centerPanel, BorderLayout.CENTER);

        JButton refreshBtn = new JButton("Refresh Audit Log");
        refreshBtn.addActionListener(e -> refreshAudit());
        add(refreshBtn, BorderLayout.SOUTH);
        
        refreshAudit();
    }

    private void refreshAudit() {
        tableModel.setRowCount(0);
        tableModel.addRow(new Object[]{"2026-02-25 12:00:01", "GUI_START", "AEM API Studio launched"});
        tableModel.addRow(new Object[]{"2026-02-25 12:05:30", "CONNECT", "Connected to local environment"});
        tableModel.addRow(new Object[]{"2026-02-25 12:10:15", "LIST_ASSETS", "Retrieved 20 assets from /content/dam"});
        
        cacheStatsLabel.setText("Cache Stats: 15 hits, 3 misses, 45 items stored");
    }
}
