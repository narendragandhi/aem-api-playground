package com.aemtools.aem.gui;

import com.aemtools.aem.api.GraphQLApi;
import com.aemtools.aem.client.AemApiClient;
import com.aemtools.aem.config.ConfigManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.swing.*;
import java.awt.*;

public class GraphQLPanel extends JPanel {

    private JTextArea queryArea;
    private JTextArea variablesArea;
    private JTextArea resultArea;
    private JButton executeBtn;

    public GraphQLPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("GraphQL Query Editor");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        add(title, BorderLayout.NORTH);

        // Input Split (Query and Variables)
        queryArea = new JTextArea("{ \n  articleList {\n    items {\n      _path\n      title\n    }\n  }\n}");
        queryArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        
        variablesArea = new JTextArea("{}");
        variablesArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        
        JSplitPane inputSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, 
                createTitledPanel("Query", queryArea), 
                createTitledPanel("Variables (JSON)", variablesArea));
        inputSplit.setDividerLocation(400);

        // Result Area
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        resultArea.setBackground(new Color(30, 30, 30));
        resultArea.setForeground(new Color(0, 200, 0));

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, 
                inputSplit, 
                createTitledPanel("Result", resultArea));
        mainSplit.setDividerLocation(500);
        
        add(mainSplit, BorderLayout.CENTER);

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        executeBtn = new JButton("Execute Query");
        executeBtn.setBackground(new Color(0, 120, 215));
        executeBtn.setForeground(Color.WHITE);
        executeBtn.addActionListener(e -> runQuery());
        
        toolbar.add(executeBtn);
        add(toolbar, BorderLayout.SOUTH);
    }

    private JPanel createTitledPanel(String title, JComponent component) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(new JScrollPane(component), BorderLayout.CENTER);
        return panel;
    }

    private void runQuery() {
        String query = queryArea.getText().trim();
        String variables = variablesArea.getText().trim();
        
        resultArea.setText("Executing query...");
        executeBtn.setEnabled(false);

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                ConfigManager config = ConfigManager.getInstance();
                if (config.getActiveEnvironmentUrl() == null) {
                    return "Error: Not connected. Go to Environments panel first.";
                }

                AemApiClient client = new AemApiClient();
                ObjectMapper mapper = client.getObjectMapper();
                
                ObjectNode request = mapper.createObjectNode();
                request.put("query", query);
                if (!variables.isEmpty() && !variables.equals("{}")) {
                    request.set("variables", mapper.readTree(variables));
                }
                
                JsonNode response = client.post("/graphql/execute.json", request);
                return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
            }

            @Override
            protected void done() {
                try {
                    resultArea.setText(get());
                } catch (Exception e) {
                    resultArea.setForeground(Color.RED);
                    resultArea.setText("Query Failed:\n" + e.getMessage());
                } finally {
                    executeBtn.setEnabled(true);
                }
            }
        }.execute();
    }
}
