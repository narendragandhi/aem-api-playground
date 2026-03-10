package com.aemtools.aem.commands;

import com.aemtools.aem.config.ConfigManager;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.Callable;

/**
 * Command to connect to an AEM environment.
 * Supports multiple authentication methods including basic auth,
 * OAuth tokens, and Adobe IMS integration.
 */
@Command(name = "connect", description = "Connect to an AEM environment")
public class ConnectCommand implements Callable<Integer> {

    @Option(names = {"-e", "--env"}, description = "Environment name (dev, staging, prod, local)", required = true)
    private String env;

    @Option(names = {"-u", "--url"}, description = "AEM host URL")
    private String url;

    @Option(names = {"--local"}, description = "Connect to local AEM SDK (http://localhost:4502)")
    private boolean local;

    @Option(names = {"--https-only"}, description = "Enforce HTTPS (reject HTTP connections)")
    private boolean httpsOnly;

    @Option(names = {"--ims-endpoint"}, description = "Adobe IMS endpoint")
    private String imsEndpoint = "https://ims-na1.adobelogin.com/ims/token/v3";

    @Option(names = {"--client-id"}, description = "Adobe IO Client ID")
    private String clientId;

    @Option(names = {"--client-secret"}, description = "Adobe IO Client Secret")
    private String clientSecret;

    @Option(names = {"--access-token"}, description = "Direct access token (bypasses OAuth flow)")
    private String accessToken;

    @Option(names = {"-u2", "--user"}, description = "Username for basic auth (local AEM)")
    private String username;

    @Option(names = {"-p2", "--password"}, description = "Password for basic auth (local AEM)")
    private String password;

    @Option(names = {"--save"}, description = "Save configuration for future use")
    private boolean save;

    /**
     * Executes the connect command, establishing connection to the AEM environment.
     *
     * @return exit code (0 for success, 1 for failure)
     * @throws Exception if connection fails
     */
    @Override
    public Integer call() throws Exception {
        ConfigManager config = ConfigManager.getInstance();

        if (local) {
            url = "http://localhost:4502";
            System.out.println("Using local AEM SDK: " + url);
        }

        if (url == null || url.isEmpty()) {
            System.out.println("Error: --url or --local required");
            return 1;
        }

        if (httpsOnly && url.startsWith("http://")) {
            System.out.println("Error: HTTPS enforcement enabled. Use HTTPS URL.");
            return 1;
        }

        if (username != null && password != null) {
            String encoded = Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
            config.setBasicAuth(env, encoded);
            System.out.println("Using basic auth for: " + env);
        }

        if (save) {
            config.setActiveEnvironment(env);
            config.setEnvironmentUrl(env, url);
            if (accessToken != null) {
                config.setAccessToken(env, accessToken);
            }
            if (username != null && password != null) {
                String encoded = Base64.getEncoder()
                    .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
                config.setBasicAuth(env, encoded);
            }
            config.save();
            System.out.println("Configuration saved for environment: " + env);
        } else if (accessToken != null) {
            config.setActiveEnvironment(env);
            config.setEnvironmentUrl(env, url);
            config.setAccessToken(env, accessToken);
            System.out.println("Connected to " + env + " (" + url + ") with access token");
        } else if (username != null && password != null) {
            config.setActiveEnvironment(env);
            config.setEnvironmentUrl(env, url);
            config.setBasicAuth(env, Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8)));
            System.out.println("Connected to " + env + " (" + url + ") with basic auth");
        } else {
            config.setActiveEnvironment(env);
            config.setEnvironmentUrl(env, url);
            System.out.println("Connected to " + env + " (" + url + ")");
        }
        return 0;
    }
}
