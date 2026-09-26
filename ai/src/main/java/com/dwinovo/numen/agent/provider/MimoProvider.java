package com.dwinovo.numen.agent.provider;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/** MiMo's tool parser needs optional primitive fields instead of nullable type unions. */
public final class MimoProvider extends OpenAIProvider {
    public static final String NAME = "mimo-token-plan-cn";
    public static final String DEFAULT_BASE_URL = "https://token-plan-cn.xiaomimimo.com/v1";

    public MimoProvider() {
        super(NAME, DEFAULT_BASE_URL, LlmProvider.THINKING_TYPE);
    }

    @Override
    public JsonArray buildToolList(Collection<? extends IToolSpec> tools) {
        JsonArray out = super.buildToolList(tools);
        for (JsonElement tool : out) {
            optionalNullableFields(tool.getAsJsonObject().getAsJsonObject("function")
                    .getAsJsonObject("parameters"));
        }
        return out;
    }

    private static void optionalNullableFields(JsonObject schema) {
        Set<String> optional = new HashSet<>();
        if (schema.has("properties") && schema.get("properties").isJsonObject()) {
            for (var entry : schema.getAsJsonObject("properties").entrySet()) {
                if (!entry.getValue().isJsonObject()) continue;
                JsonObject field = entry.getValue().getAsJsonObject();
                JsonElement type = field.get("type");
                if (type != null && type.isJsonArray() && type.getAsJsonArray().size() == 2) {
                    JsonArray types = type.getAsJsonArray();
                    JsonElement primitive = null;
                    for (JsonElement candidate : types) {
                        if (candidate.isJsonPrimitive() && !"null".equals(candidate.getAsString())) {
                            primitive = candidate;
                        }
                    }
                    if (primitive != null && types.contains(new com.google.gson.JsonPrimitive("null"))) {
                        // MiMo emits incomplete arguments for type:[number,null]. Omission has
                        // the same meaning to Numen's nullable arguments, without parsing text
                        // as an action or changing the schemas sent to other providers.
                        field.add("type", primitive);
                        optional.add(entry.getKey());
                    }
                }
                optionalNullableFields(field);
            }
        }
        if (!optional.isEmpty() && schema.has("required") && schema.get("required").isJsonArray()) {
            JsonArray required = new JsonArray();
            for (JsonElement name : schema.getAsJsonArray("required")) {
                if (!optional.contains(name.getAsString())) required.add(name);
            }
            schema.add("required", required);
        }
        for (String key : new String[]{"items", "additionalProperties"}) {
            if (schema.has(key) && schema.get(key).isJsonObject()) {
                optionalNullableFields(schema.getAsJsonObject(key));
            }
        }
        for (String key : new String[]{"anyOf", "oneOf", "allOf"}) {
            if (!schema.has(key) || !schema.get(key).isJsonArray()) continue;
            for (JsonElement branch : schema.getAsJsonArray(key)) {
                if (branch.isJsonObject()) optionalNullableFields(branch.getAsJsonObject());
            }
        }
    }
}
