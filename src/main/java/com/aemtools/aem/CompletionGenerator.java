package com.aemtools.aem;

import picocli.AutoComplete;
import picocli.CommandLine;
import picocli.CommandLine.Model;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CompletionGenerator {

    public static void main(String[] args) throws IOException {
        if (args.length > 0 && args[0].equals("generate")) {
            generateCompletionScripts();
        } else {
            System.out.println("Usage: java -jar aem-api.jar completion generate");
        }
    }

    private static void generateCompletionScripts() throws IOException {
        Path bashDir = Paths.get(".completion/bash");
        Path zshDir = Paths.get(".completion/zsh");
        
        Files.createDirectories(bashDir);
        Files.createDirectories(zshDir);

        generateBashCompletion(bashDir.resolve("aem-api"));
        generateZshCompletion(zshDir.resolve("_aem-api"));
        
        System.out.println("Generated completion scripts:");
        System.out.println("  .completion/bash/aem-api");
        System.out.println("  .completion/zsh/_aem-api");
        System.out.println("\nTo install:");
        System.out.println("  Bash: source .completion/bash/aem-api");
        System.out.println("  Zsh:  source .completion/zsh/_aem-api");
        System.out.println("  Or add to ~/.bashrc or ~/.zshrc");
    }

    private static void generateBashCompletion(Path file) throws IOException {
        String script = """
            #!/bin/bash

            _aem-api_completions() {
                local IFS=$'\\n'
                local cur prev words cword
                _init_completion || return

                local commands="${COMMANDS}"
                local options="${OPTIONS}"

                case "${cword}" in
                    1)
                        COMPREPLY=($(compgen -W "${commands}" -- "${cur}"))
                        ;;
                    *)
                        COMPREPLY=($(compgen -W "${options}" -- "${cur}"))
                        ;;
                esac
            }

            complete -F _aem-api_completions aem-api
            """.replace("${COMMANDS}", getCommands()).replace("${OPTIONS}", getOptions());

        Files.writeString(file, script);
    }

    private static void generateZshCompletion(Path file) throws IOException {
        String script = """
            #compdef aem-api

            local -a commands
            local -a options

            commands=(
                """ + getCommandsZsh() + """
            )

            options=(
                """ + getOptionsZsh() + """
            )

            _arguments -C \\
                '1: :_guard "^-*" command' \\
                '*:: :->args' \\
                && return

            case $line[1] in
                """ + getCommandCases() + """
            esac
            """;

        Files.writeString(file, script);
    }

    private static String getCommands() {
        return "shell connect cf assets sites forms config graphql translation cloudmgr folders tags workflow users replicate packages models audit agent help version";
    }

    private static String getOptions() {
        return "-v --verbose --debug -h --help";
    }

    private static String getCommandsZsh() {
        return """
            'shell:Enter interactive shell mode'
            'connect:Connect to an AEM environment'
            'cf:Content Fragment operations'
            'assets:Assets operations'
            'sites:Sites operations'
            'forms:Forms operations'
            'config:Configuration management'
            'graphql:GraphQL operations'
            'translation:Translation operations'
            'cloudmgr:Cloud Manager API operations'
            'folders:Folder operations'
            'tags:Tag operations'
            'workflow:Workflow operations'
            'users:User operations'
            'replicate:Replication operations'
            'packages:Package operations'
            'models:Content Fragment Models operations'
            'audit:View audit logs'
            'agent:AI-powered AEM assistant'
            'help:Show help'
            'version:Show version'""";
    }

    private static String getOptionsZsh() {
        return """
            '-v[Enable verbose output]'
            '--verbose[Enable verbose output]'
            '--debug[Enable debug mode]'
            '-h[Show help]'
            '--help[Show help]'""";
    }

    private static String getCommandCases() {
        return """
            shell)
                _arguments '-h[Help]'
                ;;
            connect)
                _arguments '-e[Environment]' '-u[URL]' '--local[Local SDK]' '--save[Save config]' '-h[Help]'
                ;;
            cf)
                _arguments '-p[Path]' '-m[Max results]' '-h[Help]'
                ;;
            assets)
                _arguments '-p[Path]' '-f[File]' '-h[Help]'
                ;;
            agent)
                _arguments '-m[Message]' '-i[Interactive]' '--api-key[API key]' '--model[Model]' '--stats[Stats]' '-h[Help]'
                ;;""";
    }
}
