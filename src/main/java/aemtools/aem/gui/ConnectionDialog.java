package com.aemtools.aem.gui;

import com.aemtools.aem.config.ConfigManager;

import javax.swing.*;
import java.awt.*;
import java.util.Base64;

public class ConnectionDialog extends JDialog {

    private JTextField nameField;
    private JTextField urlField;
    private JTextField userField;
    private JPasswordField passField;
    private JCheckBox saveCheck;
    private boolean succeeded = false;

    public ConnectionDialog(Frame parent) {
        super(parent, "Add AEM Environment", true);
        setLayout(new BorderLayout());
        setResizable(false);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints cs = new GridBagConstraints();
        cs.fill = GridBagConstraints.HORIZONTAL;
        cs.insets = new Insets(10, 10, 10, 10);

        // Name
        cs.gridx = 0; cs.gridy = 0;
        panel.add(new JLabel("Env Name: "), cs);
        nameField = new JTextField(15);
        cs.gridx = 1; panel.add(nameField, cs);

        // URL
        cs.gridx = 0; cs.gridy = 1;
        panel.add(new JLabel("AEM URL: "), cs);
        urlField = new JTextField("http://localhost:4502", 15);
        cs.gridx = 1; panel.add(urlField, cs);

        // User
        cs.gridx = 0; cs.gridy = 2;
        panel.add(new JLabel("Username: "), cs);
        userField = new JTextField("admin", 15);
        cs.gridx = 1; panel.add(userField, cs);

        // Password
        cs.gridx = 0; cs.gridy = 3;
        panel.add(new JLabel("Password: "), cs);
        passField = new JPasswordField(15);
        cs.gridx = 1; panel.add(passField, cs);

        saveCheck = new JCheckBox("Save to configuration", true);
        cs.gridx = 0; cs.gridy = 4; cs.gridwidth = 2;
        panel.add(saveCheck, cs);

        add(panel, BorderLayout.CENTER);

        JPanel bp = new JPanel();
        JButton btnConnect = new JButton("Connect");
        JButton btnCancel = new JButton("Cancel");

        btnConnect.addActionListener(e -> {
            if (validateInput()) {
                saveConnection();
                succeeded = true;
                dispose();
            }
        });

        btnCancel.addActionListener(e -> dispose());

        bp.add(btnConnect);
        bp.add(btnCancel);
        add(bp, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(parent);
    }

    private boolean validateInput() {
        if (nameField.getText().trim().isEmpty() || urlField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name and URL are required", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private void saveConnection() {
        ConfigManager config = ConfigManager.getInstance();
        String name = nameField.getText().trim();
        String url = urlField.getText().trim();
        String user = userField.getText().trim();
        String pass = new String(passField.getPassword());

        config.setEnvironmentUrl(name, url);
        if (!user.isEmpty() && !pass.isEmpty()) {
            String auth = Base64.getEncoder().encodeToString((user + ":" + pass).getBytes());
            config.setBasicAuth(name, auth);
        }
        config.setActiveEnvironment(name);
        
        if (saveCheck.isSelected()) {
            config.save();
        }
    }

    public boolean isSucceeded() {
        return succeeded;
    }
}
