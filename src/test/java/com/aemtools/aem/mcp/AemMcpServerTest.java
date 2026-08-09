package com.aemtools.aem.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AemMcpServerTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void initializeReturnsProtocolVersion() throws Exception {
    JsonNode response = drive("initialize", 1).get(0);
    assertEquals("2024-11-05", response.path("result").path("protocolVersion").asText());
    assertEquals("aem-mcp-server", response.path("result").path("serverInfo").path("name").asText());
  }

  @Test
  void toolsListContainsPackagerAndRecipeTools() throws Exception {
    JsonNode response = drive("tools/list", 2).get(0);
    JsonNode tools = response.path("result").path("tools");
    assertTrue(tools.isArray());

    List<String> names = new ArrayList<>();
    tools.forEach(tool -> names.add(tool.path("name").asText()));

    for (String tool : List.of(
        "aem_packages_get",
        "aem_packages_delete",
        "aem_packages_uninstall",
        "aem_packages_upload",
        "aem_packages_download",
        "aem_recipe_site_launch",
        "aem_recipe_content_backup",
        "aem_recipe_asset_batch",
        "aem_recipe_user_onboard",
        "aem_recipe_package_migrate")) {
      assertTrue(names.contains(tool), "Missing tool: " + tool);
    }
  }

  @Test
  void unknownToolReturnsError() throws Exception {
    String params = "{\"name\":\"aem_does_not_exist\",\"arguments\":{}}";
    JsonNode response = drive("tools/call", 3, params).get(0);
    assertTrue(response.has("error"));
    assertEquals(-32603, response.path("error").path("code").asInt());
  }

  @Test
  void packageMigrateWithoutAuthValidatesWithoutNetwork() throws Exception {
    String params = "{\"name\":\"aem_recipe_package_migrate\",\"arguments\":{"
        + "\"name\":\"test\",\"group\":\"my_packages\",\"targetUrl\":\"http://localhost:4502\","
        + "\"targetAuth\":\"\",\"targetToken\":\"\"}}";
    JsonNode response = drive("tools/call", 4, params).get(0);
    JsonNode result = response.path("result");
    assertEquals(false, result.path("isError").asBoolean());
    String text = result.path("content").get(0).path("text").asText();
    JsonNode payload = mapper.readTree(text);
    assertEquals(false, payload.path("success").asBoolean());
    assertTrue(payload.path("error").asText().contains("target-auth"), text);
  }

  private List<JsonNode> drive(String method, int id) throws Exception {
    return drive(method, id, "{}");
  }

  private List<JsonNode> drive(String method, int id, String params) throws Exception {
    String line = "{\"jsonrpc\":\"2.0\",\"id\":" + id
        + ",\"method\":\"" + method + "\",\"params\":" + params + "}";
    return runLines(List.of(line));
  }

  private List<JsonNode> runLines(List<String> lines) throws Exception {
    String input = String.join("\n", lines) + "\n";
    StringWriter writer = new StringWriter();
    AemMcpServer server = new AemMcpServer(new StringReader(input), new PrintWriter(writer));
    server.run();
    writer.flush();

    List<JsonNode> responses = new ArrayList<>();
    for (String line : writer.toString().split("\\n")) {
      if (!line.trim().isEmpty()) {
        responses.add(mapper.readTree(line));
      }
    }
    return responses;
  }
}
