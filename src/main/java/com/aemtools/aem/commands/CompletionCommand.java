package com.aemtools.aem.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

/**
 * Command to generate shell completion scripts.
 * Supports Bash, Zsh, and Fish shells.
 */
@Command(name = "completion", description = "Generate shell completion scripts")
public class CompletionCommand implements Callable<Integer> {

    @Option(names = {"--bash"}, description = "Generate bash completion")
    private boolean bash;

    @Option(names = {"--zsh"}, description = "Generate zsh completion")
    private boolean zsh;

    @Option(names = {"--fish"}, description = "Generate fish completion")
    private boolean fish;

    @Option(names = {"--install"}, description = "Install completions to ~/.completion")
    private boolean install;

    /**
     * Executes the completion command.
     *
     * @return exit code 0
     * @throws Exception if completion generation fails
     */
    @Override
    public Integer call() throws Exception {
        StringBuilder sb = new StringBuilder();

        if (bash || (!zsh && !fish)) {
            sb.append(generateBashCompletion());
        }

        if (zsh) {
            sb.append(generateZshCompletion());
        }

        if (fish) {
            sb.append(generateFishCompletion());
        }

        if (install) {
            return installCompletions();
        }

        if (sb.length() == 0) {
            printUsage();
            return 0;
        }

        System.out.print(sb);
        return 0;
    }

    /**
     * Installs completion scripts to the user's home directory.
     *
     * @return exit code 0
     * @throws Exception if installation fails
     */
    private int installCompletions() throws Exception {
        Path home = Paths.get(System.getProperty("user.home"));
        Path completionDir = home.resolve(".aem-api/completion");
        Files.createDirectories(completionDir);

        Files.writeString(completionDir.resolve("aem-api.bash"), generateBashCompletion());
        Files.writeString(completionDir.resolve("aem-api.zsh"), generateZshCompletion());
        Files.writeString(completionDir.resolve("aem-api.fish"), generateFishCompletion());

        System.out.println("Completions installed to " + completionDir);
        System.out.println("Add to your shell:");
        System.out.println("  Bash: echo 'source " + completionDir.resolve("aem-api.bash") + "' >> ~/.bashrc");
        System.out.println("  Zsh:  echo 'source " + completionDir.resolve("aem-api.zsh") + "' >> ~/.zshrc");
        return 0;
    }

    /**
     * Generates Bash completion script.
     *
     * @return the completion script
     */
    private String generateBashCompletion() {
        return """
            #!/bin/bash
            # AEM API Bash Completion

            _aem_api() {
                local cur prev opts
                COMPREPLY=()
                cur="${COMP_WORDS[COMP_CWORD]}"
                prev="${COMP_WORDS[COMP_CWORD-1]}"

                opts="shell connect cf assets sites forms config graphql translation cloudmgr folders tags workflow users replicate packages models audit agent completion help version -v --verbose --debug"

                COMPREPLY=( $(compgen -W "${opts}" -- ${cur}) )
                return 0
            }

            complete -F _aem_api aem-api
            """;
    }

    /**
     * Generates Zsh completion script.
     *
     * @return the completion script
     */
    private String generateZshCompletion() {
        return """
            #compdef aem-api
            # AEM API Zsh Completion

            _aem_api() {
                local -a commands
                commands=(
                    'shell:Enter interactive shell mode'
                    'connect:Connect to AEM environment'
                    'cf:Content Fragment operations'
                    'assets:Assets operations'
                    'sites:Sites operations'
                    'forms:Forms operations'
                    'config:Configuration management'
                    'graphql:GraphQL operations'
                    'translation:Translation operations'
                    'cloudmgr:Cloud Manager API'
                    'folders:Folder operations'
                    'tags:Tag operations'
                    'workflow:Workflow operations'
                    'users:User operations'
                    'replicate:Replication operations'
                    'packages:Package operations'
                    'models:Content Fragment Models'
                    'audit:View audit logs'
                    'agent:AI-powered AEM assistant'
                    'completion:Generate completions'
                    'help:Show help'
                    'version:Show version'
                )

                _describe 'command' commands
            }

            _aem_api "$@"
            """;
    }

    /**
     * Generates Fish completion script.
     *
     * @return the completion script
     */
    private String generateFishCompletion() {
        return """
            # AEM API Fish Completion

            complete -c aem-api -f -n '__fish_use_subcommand' -a 'shell' -d 'Enter interactive shell mode';
            complete -c aem-api -f -n '__fish_use_subcommand' -a 'connect' -d 'Connect to AEM environment';
            complete -c aem-api -f -n '__fish_use_subcommand' -a 'cf' -d 'Content Fragment operations';
            complete -c aem-api -f -n '__fish_use_subcommand' -a 'assets' -d 'Assets operations';
            complete -c aem-api -f -n '__fish_use_subcommand' -a 'sites' -d 'Sites operations';
            complete -c aem-api -f -n '__fish_use_subcommand' -a 'forms' -d 'Forms operations';
            complete -c aem-api -f -n '__fish_use_subcommand' -a 'config' -d 'Configuration management';
            complete -c aem-api -f -n '__fish_use_subcommand' -a 'graphql' -d 'GraphQL operations';
            complete -c aem-api -f -n '__fish_use_subcommand' -a 'translation' -d 'Translation operations';
            complete -c aem-api -f -n '__fish_use_subcommand' -a 'cloudmgr' -d 'Cloud Manager API';
            complete -c aem-api -f -n '__fish_use_subcommand' -a 'folders' -d 'Folder operations';
            complete -c aem-api -f -n '__fish_use_subcommand' -a 'tags' -d 'Tag operations';
            complete -c aem-api -f -n '__fish_use_subcommand' -a 'workflow' -d 'Workflow operations';
            complete -c aem-api -f -n '__fish_use_subcommand' -a 'users' -d 'User operations';
            complete -c aem-api -f -n '__fish_use_subcommand' -a 'replicate' -d 'Replication operations';
            complete -c aem-api -f -n '__fish_use_subcommand' -a 'packages' -d 'Package operations';
            complete -c aem-api -f -n '__fish_use_subcommand' -a 'models' -d 'Content Fragment Models';
            complete -c aem-api -f -n '__fish_use_subcommand' -a 'audit' -d 'View audit logs';
            complete -c aem-api -f -n '__fish_use_subcommand' -a 'agent' -d 'AI-powered AEM assistant';
            complete -c aem-api -f -n '__fish_use_subcommand' -a 'completion' -d 'Generate completions';
            complete -c aem-api -f -n '__fish_use_subcommand' -a 'help' -d 'Show help';
            complete -c aem-api -f -n '__fish_use_subcommand' -a 'version' -d 'Show version';
            """;
    }

    /**
     * Prints usage information.
     */
    private void printUsage() {
        System.out.println("Usage: aem-api completion [--bash|--zsh|--fish] [--install]");
        System.out.println("\nOptions:");
        System.out.println("  --bash     Generate bash completion script");
        System.out.println("  --zsh      Generate zsh completion script");
        System.out.println("  --fish     Generate fish completion script");
        System.out.println("  --install  Install completions to ~/.aem-api/completion");
        System.out.println("\nTo use:");
        System.out.println("  aem-api completion --bash > /etc/bash_completion.d/aem-api");
        System.out.println("  source <(aem-api completion --bash)");
    }
}
