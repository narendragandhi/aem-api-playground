package com.aemtools.aem.gui;

import com.aemtools.aem.CliFlags;
import com.aemtools.aem.util.MockDataHelper;
import com.fasterxml.jackson.databind.JsonNode;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class PackageManagerPanel extends JPanel {

    private JTable packageTable;
    private DefaultTableModel tableModel;

    public PackageManagerPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("CRX Package Manager");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        add(title, BorderLayout.NORTH);

        String[] columns = {"Group", "Name", "Version", "Size", "Status"};
        tableModel = new DefaultTableModel(columns, 0);
        packageTable = new JTable(tableModel);
        packageTable.setRowHeight(30);
        
        add(new JScrollPane(packageTable), BorderLayout.CENTER);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton uploadBtn = new JButton("Upload Package");
        JButton buildBtn = new JButton("Build");
        JButton installBtn = new JButton("Install");
        JButton refreshBtn = new JButton("Refresh");
        
        refreshBtn.addActionListener(e -> refreshPackages());
        
        toolbar.add(refreshBtn);
        toolbar.add(uploadBtn);
        toolbar.add(new JSeparator(JSeparator.VERTICAL));
        toolbar.add(buildBtn);
        toolbar.add(installBtn);
        
        add(toolbar, BorderLayout.SOUTH);
        
        refreshPackages();
    }

    private void refreshPackages() {
        tableModel.setRowCount(0);
        if (CliFlags.mockMode) {
            JsonNode pkgs = MockDataHelper.getPackages();
            for (JsonNode pkg : pkgs) {
                tableModel.addRow(new Object[]{
                    pkg.path("group").asText(),
                    pkg.path("name").asText(),
                    pkg.path("version").asText(),
                    "1.2 MB",
                    "INSTALLED"
                });
            }
        } else {
            tableModel.addRow(new Object[]{"N/A", "Please run with --mock", "to see data", "0", "OFFLINE"});
        }
    }
}
