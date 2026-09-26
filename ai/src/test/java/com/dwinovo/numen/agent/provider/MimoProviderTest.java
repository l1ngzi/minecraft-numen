package com.dwinovo.numen.agent.provider;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MimoProviderTest {
    private record Tool(Map<String, Object> parameterSchema) implements IToolSpec {
        @Override public String name() { return "goto"; }
        @Override public String description() { return "Travel to a destination"; }
    }

    private static JsonObject parameters(OpenAIProvider provider, IToolSpec tool) {
        return provider.buildToolList(List.of(tool)).get(0).getAsJsonObject()
                .getAsJsonObject("function").getAsJsonObject("parameters");
    }

    @Test
    void optionalCoordinatesUsePrimitiveNumbersWithoutChangingOtherProviders() {
        var tool = new Tool(Map.of("type", "object", "properties", Map.of(
                "x", Map.of("type", List.of("number", "null"), "description", "Target X"),
                "z", Map.of("type", List.of("null", "number")),
                "block", Map.of("type", "string")),
                "required", List.of("x", "z", "block"), "additionalProperties", false));
        JsonObject original = parameters(new OpenAIProvider(), tool);
        JsonObject mimo = parameters(new MimoProvider(), tool);
        assertEquals("number", mimo.getAsJsonObject("properties").getAsJsonObject("x").get("type").getAsString());
        assertEquals("number", mimo.getAsJsonObject("properties").getAsJsonObject("z").get("type").getAsString());
        assertEquals("Target X", mimo.getAsJsonObject("properties").getAsJsonObject("x").get("description").getAsString());
        assertEquals("[\"block\"]", mimo.get("required").toString());
        assertFalse(mimo.get("additionalProperties").getAsBoolean());
        assertEquals(original, parameters(new OpenAIProvider(), tool));
        assertTrue(original.getAsJsonObject("properties").getAsJsonObject("x").get("type").isJsonArray());
    }

    @Test
    void nestedObjectsAndArrayItemsKeepTheirRequiredFields() {
        Map<String, Object> object = Map.of("type", "object", "properties", Map.of(
                "timeout", Map.of("type", List.of("integer", "null"), "minimum", 1),
                "item", Map.of("type", "string")), "required", List.of("timeout", "item"));
        var tool = new Tool(Map.of("type", "object", "properties", Map.of(
                "spec", object, "steps", Map.of("type", "array", "items", object)),
                "required", List.of("spec", "steps")));
        JsonObject schema = parameters(new MimoProvider(), tool);
        assertEquals("[\"spec\",\"steps\"]", schema.get("required").toString());
        for (JsonObject nested : List.of(schema.getAsJsonObject("properties").getAsJsonObject("spec"),
                schema.getAsJsonObject("properties").getAsJsonObject("steps").getAsJsonObject("items"))) {
            assertEquals("[\"item\"]", nested.get("required").toString());
            JsonObject timeout = nested.getAsJsonObject("properties").getAsJsonObject("timeout");
            assertEquals("integer", timeout.get("type").getAsString());
            assertEquals(1, timeout.get("minimum").getAsInt());
        }
    }

    @Test
    void chinaEndpointAndThinkingDialectMatchTheRegistry() {
        var provider = new MimoProvider();
        assertEquals(MimoProvider.DEFAULT_BASE_URL, ProviderRegistry.baseUrl(MimoProvider.NAME));
        assertEquals(1000000, ProviderRegistry.contextWindow(MimoProvider.NAME, "mimo-v2.6-flash"));
        JsonObject body = new JsonObject();
        provider.applyReasoning(body, "off");
        assertEquals("disabled", body.getAsJsonObject("thinking").get("type").getAsString());
    }
}
