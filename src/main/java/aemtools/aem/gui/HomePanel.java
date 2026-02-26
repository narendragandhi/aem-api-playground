package com.aemtools.aem.gui;

import com.aemtools.aem.config.ConfigManager;

import javax.swing.*;
import java.awt.*;

public class HomePanel extends JPanel {

    public HomePanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(30, 30, 30));
        setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        JPanel welcomePanel = new JPanel();
        welcomePanel.setLayout(new BoxLayout(welcomePanel, BoxLayout.Y_AXIS));
        welcomePanel.setOpaque(false);

        JLabel title = new JLabel("Welcome to AEM API Studio");
        title.setFont(new Font("SansSerif", Font.BOLD, 36));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Visual playground for Adobe Experience Manager APIs");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 18));
        subtitle.setForeground(Color.LIGHT_GRAY);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        welcomePanel.add(title);
        welcomePanel.add(Box.createVerticalStrut(10));
        welcomePanel.add(subtitle);
        welcomePanel.add(Box.createVerticalStrut(50));

        // Stats Cards
        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        statsPanel.setOpaque(false);
        statsPanel.add(createStatCard("Environments", "4 Configured", new Color(0, 120, 215)));
        statsPanel.add(createStatCard("Active Tasks", "2 Running", new Color(0, 150, 136)));
        statsPanel.add(createStatCard("Cache Health", "95% Hit Rate", new Color(255, 152, 0)));

        welcomePanel.add(statsPanel);
        welcomePanel.add(Box.createVerticalStrut(50));

        // Quick Actions
        JPanel quickPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        quickPanel.setOpaque(false);
        quickPanel.add(new JButton("New GraphQL Query"));
        quickPanel.add(new JButton("Connect to Local SDK"));
        quickPanel.add(new JButton("Open Documentation"));

        welcomePanel.add(quickPanel);

        add(welcomePanel, BorderLayout.NORTH);
        
        // Environment Status
        String activeEnv = ConfigManager.getInstance().getActiveEnvironment();
        JLabel footer = new JLabel("Currently managing: " + (activeEnv != null ? activeEnv : "None"));
        footer.setForeground(Color.GRAY);
        footer.setHorizontalAlignment(SwingConstants.CENTER);
        add(footer, BorderLayout.SOUTH);
    }

    private JPanel createStatCard(String title, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(new Color(45, 45, 45));
        card.setBorder(BorderFactory.createLineBorder(color, 2));
        card.setPreferredSize(new Dimension(200, 120));

        JLabel tLabel = new JLabel(title);
        tLabel.setForeground(Color.LIGHT_GRAY);
        tLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        
        JLabel vLabel = new JLabel(value);
        vLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        vLabel.setForeground(Color.WHITE);
        vLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        card.add(tLabel, BorderLayout.NORTH);
        card.add(vLabel, BorderLayout.CENTER);
        return card;
    }
}
