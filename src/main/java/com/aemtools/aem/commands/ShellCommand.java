package com.aemtools.aem.commands;

import com.aemtools.aem.shell.InteractiveShell;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * Command to enter interactive shell mode.
 * Provides a full-featured shell experience with command history,
 * auto-completion, and pipe processing support.
 */
@Command(name = "shell", description = "Enter interactive shell mode")
public class ShellCommand implements Callable<Integer> {

    @Option(names = {"--history-file"}, description = "Path to history file")
    private String historyFile = "~/.aem-api/history.txt";

    /**
     * Executes the shell command, starting the interactive shell.
     *
     * @return exit code (0 for success)
     * @throws Exception if shell initialization fails
     */
    @Override
    public Integer call() throws Exception {
        InteractiveShell shell = new InteractiveShell(historyFile);
        return shell.run();
    }
}
