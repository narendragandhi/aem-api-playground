package com.aemtools.aem.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * Command for AEM Forms operations.
 * Supports listing and submitting adaptive forms.
 */
@Command(name = "forms", description = "Forms operations", subcommands = {
    FormsCommand.ListCommand.class,
    FormsCommand.SubmitCommand.class
})
public class FormsCommand implements Callable<Integer> {

    /**
     * Shows usage information when called without subcommand.
     *
     * @return exit code 0
     */
    @Override
    public Integer call() throws Exception {
        System.out.println("Use 'forms list' or 'forms submit' for operations");
        return 0;
    }

    /**
     * Lists adaptive forms.
     */
    @Command(name = "list", description = "List adaptive forms")
    public static class ListCommand implements Callable<Integer> {
        @Option(names = {"-p", "--path"}, description = "Forms folder path", defaultValue = "/content/forms/af")
        private String path;

        /**
         * Executes the list forms command.
         *
         * @return exit code 0
         * @throws Exception if listing fails
         */
        @Override
        public Integer call() throws Exception {
            System.out.println("Listing forms in: " + path);
            System.out.println("(Demo mode - API client not fully implemented)");
            return 0;
        }
    }

    /**
     * Submits a form for testing.
     */
    @Command(name = "submit", description = "Submit a form (for testing)")
    public static class SubmitCommand implements Callable<Integer> {
        @Option(names = {"-f", "--form"}, description = "Form path", required = true)
        private String formPath;

        @Option(names = {"-d", "--data"}, description = "JSON form data", required = true)
        private String data;

        /**
         * Executes the submit form command.
         *
         * @return exit code 0
         * @throws Exception if submission fails
         */
        @Override
        public Integer call() throws Exception {
            System.out.println("Submitting form: " + formPath);
            System.out.println("Data: " + data);
            System.out.println("(Demo mode - API client not fully implemented)");
            return 0;
        }
    }
}
