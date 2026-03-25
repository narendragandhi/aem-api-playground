package com.aemtools.aem.gui;

import com.aemtools.aem.agent.AemAgent;

import javax.swing.*;
import java.awt.*;

public class AgentPanel extends JPanel {

    private JTextArea chatArea;
    private JTextField inputField;
    private AemAgent agent;

    public AgentPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("AI Agent Assistant");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        add(title, BorderLayout.NORTH);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        
        inputField = new JTextField();
        inputField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        inputField.addActionListener(e -> sendMessage());
        
        JButton sendBtn = new JButton("Send");
        sendBtn.addActionListener(e -> sendMessage());
        
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendBtn, BorderLayout.EAST);
        
        add(inputPanel, BorderLayout.SOUTH);
        
        chatArea.append("Agent: Hello! I'm your AEM AI assistant. How can I help you today?\n");
    }

    private void sendMessage() {
        String message = inputField.getText().trim();
        if (message.isEmpty()) return;

        chatArea.append("\nYou: " + message + "\n");
        inputField.setText("");
        
        // Run in background to not freeze UI
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                if (agent == null) {
                    String apiKey = AemAgent.getApiKey();
                    if (apiKey == null || apiKey.isEmpty()) {
                        return "Error: OpenAI API key not found. Please set OPENAI_API_KEY environment variable.";
                    }
                    agent = new AemAgent(apiKey, "gpt-4", AemAgent.LlmProvider.OPENAI);
                }
                return agent.chat(message);
            }

            @Override
            protected void done() {
                try {
                    String response = get();
                    chatArea.append("\nAgent: " + response + "\n");
                    chatArea.setCaretPosition(chatArea.getDocument().getLength());
                } catch (Exception e) {
                    chatArea.append("\nError: " + e.getMessage() + "\n");
                }
            }
        }.execute();
    }
}
