package com.aemtools.aem.gui;

import com.aemtools.aem.RecipeCommand;
import picocli.CommandLine;

import javax.swing.*;
import java.awt.*;

public class RecipePanel extends JPanel {

    private JList<String> recipeList;
    private JTextArea logArea;
    private JButton runBtn;

    public RecipePanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Automation Recipes");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        add(title, BorderLayout.NORTH);

        // Sidebar for Recipes
        DefaultListModel<String> model = new DefaultListModel<>();
        model.addElement("Content Migration (Sample)");
        model.addElement("Site Activation Pipeline");
        model.addElement("Metadata Bulk Update");
        model.addElement("Security Audit Scan");
        
        recipeList = new JList<>(model);
        recipeList.setFixedCellHeight(40);
        recipeList.setSelectedIndex(0);
        
        // Log Area
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(Color.BLACK);
        logArea.setForeground(new Color(0, 255, 0));
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, 
                new JScrollPane(recipeList), new JScrollPane(logArea));
        split.setDividerLocation(250);
        add(split, BorderLayout.CENTER);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        runBtn = new JButton("Run Recipe");
        runBtn.addActionListener(e -> runRecipe());
        toolbar.add(runBtn);
        
        add(toolbar, BorderLayout.SOUTH);
    }

    private void runRecipe() {
        String selectedRecipe = recipeList.getSelectedValue();
        if (selectedRecipe == null) return;

        logArea.append("\n>>> Starting recipe: " + selectedRecipe + "\n");
        runBtn.setEnabled(false);
        
        new SwingWorker<Integer, String>() {
            @Override
            protected Integer doInBackground() throws Exception {
                // Redirect stdout/stderr to this log area for the duration of the recipe
                java.io.PrintStream originalOut = System.out;
                java.io.PrintStream originalErr = System.err;
                
                try {
                    // Create a stream that writes to both the original output (for ConsolePanel) and our logArea
                    java.io.OutputStream multiOut = new java.io.OutputStream() {
                        @Override
                        public void write(int b) {
                            originalOut.write(b);
                            publish(String.valueOf((char) b));
                        }
                        @Override
                        public void write(byte[] b, int off, int len) {
                            originalOut.write(b, off, len);
                            publish(new String(b, off, len));
                        }
                    };
                    
                    System.setOut(new java.io.PrintStream(multiOut, true));
                    System.setErr(new java.io.PrintStream(multiOut, true));

                    if (selectedRecipe.contains("Site Launch")) {
                        // Simulate args: -p /content/sites/mysite -t "My New Site"
                        com.aemtools.aem.RecipeCommand.SiteLaunchRecipe cmd = new com.aemtools.aem.RecipeCommand.SiteLaunchRecipe();
                        // Reflection or direct field access would be needed to set args if not using CommandLine.parse
                        // For this demo integration, we'll use the CLI parser to populate the object
                        return new CommandLine(cmd).execute("-p", "/content/sites/gui-site", "-t", "Created from GUI");
                    } else if (selectedRecipe.contains("Content Backup")) {
                        return new CommandLine(new com.aemtools.aem.RecipeCommand.ContentBackupRecipe())
                            .execute("-p", "/content/dam", "-o", "./gui-backup");
                    } else if (selectedRecipe.contains("Asset Batch")) {
                        return new CommandLine(new com.aemtools.aem.RecipeCommand.AssetBatchRecipe())
                            .execute("-p", "/content/dam/incoming", "-t", "gui-processed");
                    } else if (selectedRecipe.contains("User Onboarding")) {
                        return new CommandLine(new com.aemtools.aem.RecipeCommand.UserOnboardingRecipe())
                            .execute("-u", "gui-user", "-e", "user@example.com");
                    } else if (selectedRecipe.contains("Package Migrate")) {
                        return new CommandLine(new com.aemtools.aem.RecipeCommand.PackageMigrateRecipe())
                            .execute("-n", "gui-package", "-s", "dev", "-t", "stage");
                    }
                    return 0;
                } catch (Exception e) {
                    publish("\nError: " + e.getMessage() + "\n");
                    e.printStackTrace();
                    return 1;
                } finally {
                    System.setOut(originalOut);
                    System.setErr(originalErr);
                }
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                for (String text : chunks) {
                    logArea.append(text);
                    logArea.setCaretPosition(logArea.getDocument().getLength());
                }
            }

            @Override
            protected void done() {
                try {
                    int result = get();
                    logArea.append("\n>>> Recipe finished with code: " + result + "\n");
                } catch (Exception e) {
                    logArea.append("\n>>> Execution failed: " + e.getMessage() + "\n");
                } finally {
                    runBtn.setEnabled(true);
                }
            }
        }.execute();
    }
}
