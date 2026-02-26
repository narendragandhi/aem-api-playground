package com.aemtools.aem.gui;

import com.aemtools.aem.CliFlags;
import com.aemtools.aem.util.MockDataHelper;
import com.fasterxml.jackson.databind.JsonNode;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class WorkflowPanel extends JPanel {

    private JTable workflowTable;
    private DefaultTableModel tableModel;

    public WorkflowPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Workflow Instances");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        add(title, BorderLayout.NORTH);

        String[] columns = {"ID", "Model", "Payload", "Status", "Started By"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        workflowTable = new JTable(tableModel);
        workflowTable.setRowHeight(30);
        add(new JScrollPane(workflowTable), BorderLayout.CENTER);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refreshBtn = new JButton("Refresh");
        JButton terminateBtn = new JButton("Terminate");
        terminateBtn.setForeground(Color.RED);
        
        refreshBtn.addActionListener(e -> refreshWorkflows());
        toolbar.add(refreshBtn);
        toolbar.add(terminateBtn);
        
        add(toolbar, BorderLayout.SOUTH);
        
        refreshWorkflows();
    }

    private void refreshWorkflows() {
        tableModel.setRowCount(0);
        
        if (CliFlags.mockMode) {
            JsonNode workflows = MockDataHelper.getWorkflows();
            for (JsonNode wf : workflows) {
                tableModel.addRow(new Object[]{
                    wf.path("id").asText(),
                    wf.path("modelTitle").asText(),
                    wf.path("payload").asText("/content/dam/sample.jpg"),
                    wf.path("status").asText(),
                    wf.path("initiator").asText("admin")
                });
            }
        } else {
            tableModel.addRow(new Object[]{"N/A", "Real API not connected", "Run with --mock", "IDLE", "-"});
        }
    }
}
