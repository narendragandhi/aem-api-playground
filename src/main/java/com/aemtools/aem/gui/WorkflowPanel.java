package com.aemtools.aem.gui;

import com.aemtools.aem.CliFlags;
import com.aemtools.aem.api.WorkflowApi;
import com.aemtools.aem.api.WorkflowApi.*;
import com.aemtools.aem.client.AemApiClient;
import com.aemtools.aem.config.ConfigManager;
import com.aemtools.aem.util.MockDataHelper;
import com.fasterxml.jackson.databind.JsonNode;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class WorkflowPanel extends JPanel {

    private JTable workflowTable;
    private DefaultTableModel tableModel;
    private JComboBox<String> statusFilter;
    private JLabel statusLabel;

    public WorkflowPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header panel with title and stats
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Workflow Instances");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        headerPanel.add(title, BorderLayout.WEST);

        statusLabel = new JLabel("");
        statusLabel.setForeground(Color.GRAY);
        headerPanel.add(statusLabel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Table
        String[] columns = {"ID", "Model", "Payload", "Status", "Current Step", "Started By", "Started"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        workflowTable = new JTable(tableModel);
        workflowTable.setRowHeight(30);
        workflowTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        workflowTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        workflowTable.getColumnModel().getColumn(2).setPreferredWidth(200);
        workflowTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        workflowTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        workflowTable.getColumnModel().getColumn(5).setPreferredWidth(80);
        workflowTable.getColumnModel().getColumn(6).setPreferredWidth(120);
        add(new JScrollPane(workflowTable), BorderLayout.CENTER);

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));

        toolbar.add(new JLabel("Status: "));
        statusFilter = new JComboBox<>(new String[]{"ALL", "RUNNING", "COMPLETED", "SUSPENDED", "ABORTED", "STALE"});
        statusFilter.addActionListener(e -> refreshWorkflows());
        toolbar.add(statusFilter);

        toolbar.add(Box.createHorizontalStrut(20));

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshWorkflows());
        toolbar.add(refreshBtn);

        JButton startBtn = new JButton("Start Workflow...");
        startBtn.addActionListener(e -> showStartWorkflowDialog());
        toolbar.add(startBtn);

        JButton suspendBtn = new JButton("Suspend");
        suspendBtn.addActionListener(e -> suspendSelectedWorkflow());
        toolbar.add(suspendBtn);

        JButton resumeBtn = new JButton("Resume");
        resumeBtn.addActionListener(e -> resumeSelectedWorkflow());
        toolbar.add(resumeBtn);

        JButton terminateBtn = new JButton("Terminate");
        terminateBtn.setForeground(Color.RED);
        terminateBtn.addActionListener(e -> terminateSelectedWorkflow());
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
                    wf.path("currentStep").asText("N/A"),
                    wf.path("initiator").asText("admin"),
                    wf.path("startTime").asText("2024-01-15 10:30")
                });
            }
            statusLabel.setText("[MOCK MODE] " + workflows.size() + " workflows");
            return;
        }

        ConfigManager config = ConfigManager.getInstance();
        if (config.getActiveEnvironmentUrl() == null) {
            tableModel.addRow(new Object[]{"N/A", "Not connected", "Run 'connect' command", "IDLE", "-", "-", "-"});
            statusLabel.setText("Not connected");
            return;
        }

        try {
            AemApiClient client = new AemApiClient();
            WorkflowApi workflowApi = new WorkflowApi(client);

            String selectedStatus = (String) statusFilter.getSelectedItem();
            WorkflowApi.WorkflowStatus status = null;
            if (selectedStatus != null && !selectedStatus.equals("ALL")) {
                status = WorkflowApi.WorkflowStatus.valueOf(selectedStatus);
            }

            List<WorkflowInstance> instances = workflowApi.listInstances(status, 100);

            for (WorkflowInstance wf : instances) {
                tableModel.addRow(new Object[]{
                    wf.getId(),
                    wf.getModelTitle(),
                    wf.getPayload(),
                    wf.getStatus(),
                    wf.getCurrentStep() != null ? wf.getCurrentStep() : "-",
                    wf.getInitiator(),
                    wf.getStartTime()
                });
            }

            statusLabel.setText(instances.size() + " workflows found");
        } catch (Exception e) {
            tableModel.addRow(new Object[]{"Error", e.getMessage(), "", "", "", "", ""});
            statusLabel.setText("Error loading workflows");
        }
    }

    private void showStartWorkflowDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Start Workflow", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(500, 250);
        dialog.setLocationRelativeTo(this);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Workflow Model:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JTextField modelField = new JTextField("/var/workflow/models/request_for_activation");
        formPanel.add(modelField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        formPanel.add(new JLabel("Payload Path:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JTextField payloadField = new JTextField("/content/dam/");
        formPanel.add(payloadField, gbc);

        dialog.add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());
        JButton startBtn = new JButton("Start");
        startBtn.addActionListener(e -> {
            startWorkflow(modelField.getText(), payloadField.getText());
            dialog.dispose();
        });
        buttonPanel.add(cancelBtn);
        buttonPanel.add(startBtn);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void startWorkflow(String model, String payload) {
        if (CliFlags.mockMode) {
            JOptionPane.showMessageDialog(this,
                "[MOCK] Workflow started!\nModel: " + model + "\nPayload: " + payload,
                "Success", JOptionPane.INFORMATION_MESSAGE);
            refreshWorkflows();
            return;
        }

        try {
            AemApiClient client = new AemApiClient();
            WorkflowApi workflowApi = new WorkflowApi(client);
            WorkflowInstance instance = workflowApi.startWorkflow(model, payload);
            JOptionPane.showMessageDialog(this,
                "Workflow started successfully!\nInstance ID: " + instance.getId(),
                "Success", JOptionPane.INFORMATION_MESSAGE);
            refreshWorkflows();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Failed to start workflow: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String getSelectedWorkflowId() {
        int selectedRow = workflowTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a workflow first.",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return (String) tableModel.getValueAt(selectedRow, 0);
    }

    private void suspendSelectedWorkflow() {
        String workflowId = getSelectedWorkflowId();
        if (workflowId == null) return;

        if (CliFlags.mockMode) {
            JOptionPane.showMessageDialog(this, "[MOCK] Workflow suspended: " + workflowId,
                "Success", JOptionPane.INFORMATION_MESSAGE);
            refreshWorkflows();
            return;
        }

        try {
            AemApiClient client = new AemApiClient();
            WorkflowApi workflowApi = new WorkflowApi(client);
            workflowApi.suspendWorkflow(workflowId);
            JOptionPane.showMessageDialog(this, "Workflow suspended: " + workflowId,
                "Success", JOptionPane.INFORMATION_MESSAGE);
            refreshWorkflows();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to suspend workflow: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void resumeSelectedWorkflow() {
        String workflowId = getSelectedWorkflowId();
        if (workflowId == null) return;

        if (CliFlags.mockMode) {
            JOptionPane.showMessageDialog(this, "[MOCK] Workflow resumed: " + workflowId,
                "Success", JOptionPane.INFORMATION_MESSAGE);
            refreshWorkflows();
            return;
        }

        try {
            AemApiClient client = new AemApiClient();
            WorkflowApi workflowApi = new WorkflowApi(client);
            workflowApi.resumeWorkflow(workflowId);
            JOptionPane.showMessageDialog(this, "Workflow resumed: " + workflowId,
                "Success", JOptionPane.INFORMATION_MESSAGE);
            refreshWorkflows();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to resume workflow: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void terminateSelectedWorkflow() {
        String workflowId = getSelectedWorkflowId();
        if (workflowId == null) return;

        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to terminate workflow: " + workflowId + "?",
            "Confirm Termination", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        if (CliFlags.mockMode) {
            JOptionPane.showMessageDialog(this, "[MOCK] Workflow terminated: " + workflowId,
                "Success", JOptionPane.INFORMATION_MESSAGE);
            refreshWorkflows();
            return;
        }

        try {
            AemApiClient client = new AemApiClient();
            WorkflowApi workflowApi = new WorkflowApi(client);
            workflowApi.terminateWorkflow(workflowId);
            JOptionPane.showMessageDialog(this, "Workflow terminated: " + workflowId,
                "Success", JOptionPane.INFORMATION_MESSAGE);
            refreshWorkflows();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to terminate workflow: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
