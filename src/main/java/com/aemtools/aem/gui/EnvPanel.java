package com.aemtools.aem.gui;

import com.aemtools.aem.config.ConfigManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Map;

public class EnvPanel extends JPanel {

    private JTable envTable;
    private DefaultTableModel tableModel;

    public EnvPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Environments");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        add(title, BorderLayout.NORTH);

        // Table
        String[] columns = {"Name", "URL", "Status", "Active"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        envTable = new JTable(tableModel);
        envTable.setRowHeight(30);
        envTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        refreshTable();

        JScrollPane scrollPane = new JScrollPane(envTable);
        add(scrollPane, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton connectBtn = new JButton("Connect");
        JButton addBtn = new JButton("Add Environment");
        addBtn.addActionListener(e -> {
            ConnectionDialog dialog = new ConnectionDialog((Frame) SwingUtilities.getWindowAncestor(this));
            dialog.setVisible(true);
            if (dialog.isSucceeded()) {
                refreshTable();
            }
        });
        
        JButton deleteBtn = new JButton("Delete");
        
        connectBtn.addActionListener(e -> {
            int row = envTable.getSelectedRow();
            if (row >= 0) {
                String envName = (String) tableModel.getValueAt(row, 0);
                ConfigManager.getInstance().setActiveEnvironment(envName);
                ConfigManager.getInstance().save();
                refreshTable();
                JOptionPane.showMessageDialog(this, "Active environment set to: " + envName);
            }
        });
        
        buttonPanel.add(connectBtn);
        buttonPanel.add(addBtn);
        buttonPanel.add(deleteBtn);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        ConfigManager config = ConfigManager.getInstance();
        String active = config.getActiveEnvironment();
        
        // This is a bit of a hack since ConfigManager doesn't expose all envs easily
        // but let's try to get them from the config map
        Map<String, String> urls = config.getEnvironmentUrls();
        if (urls != null) {
            for (Map.Entry<String, String> entry : urls.entrySet()) {
                String env = entry.getKey();
                String url = entry.getValue();
                String status = "Configured";
                String isActive = env.equals(active) ? "✅" : "";
                tableModel.addRow(new Object[]{env, url, status, isActive});
            }
        }
    }
}
