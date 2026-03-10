package com.aemtools.aem.commands;

import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * Command to launch the visual interface (AEM API Studio).
 * Provides a Swing-based GUI for AEM operations.
 */
@Command(name = "gui", description = "Launch the visual interface (AEM API Studio)")
public class GuiCommand implements Callable<Integer> {

    /**
     * Launches the AEM API Studio GUI.
     *
     * @return exit code (0 for success, 1 for failure)
     * @throws Exception if GUI launch fails
     */
    @Override
    public Integer call() throws Exception {
        System.out.println("Launching AEM API Studio GUI...");
        try {
            // Use reflection to call GUI without circular dependencies
            Class<?> guiClass = Class.forName("com.aemtools.aem.gui.AemStudioGui");
            java.lang.reflect.Method launchMethod = guiClass.getMethod("launch");
            launchMethod.invoke(null);
        } catch (Exception e) {
            System.err.println("Error: GUI component could not be launched: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
        return 0;
    }
}
