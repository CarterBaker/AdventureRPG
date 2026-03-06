package com.internal.bootstrap.shaderpipeline.ubomanager;

import java.io.File;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.internal.bootstrap.shaderpipeline.ubo.UBOData;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformData;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformType;
import com.internal.core.engine.BuilderPackage;
import com.internal.core.util.JsonUtility;

/*
 * Parses UBO JSON descriptors into UBOData during bootstrap.
 * Owned by InternalLoadManager and self-releases with it when the queue empties.
 */
class InternalBuilder extends BuilderPackage {

    // Build \\

    UBOData parse(File file) {

        JsonObject json = JsonUtility.loadJsonObject(file);
        String blockName = JsonUtility.validateString(json, "blockName");
        int binding = json.has("binding") ? json.get("binding").getAsInt() : UBOData.UNSPECIFIED_BINDING;

        UBOData data = create(UBOData.class);
        data.constructor(blockName, binding);

        parseUniforms(json, data, blockName);

        return data;
    }

    private void parseUniforms(JsonObject json, UBOData data, String blockName) {

        if (!json.has("uniforms"))
            throwException("UBO '" + blockName + "' JSON is missing required 'uniforms' array");

        JsonArray array = json.getAsJsonArray("uniforms");

        for (int i = 0; i < array.size(); i++) {

            JsonObject entry = array.get(i).getAsJsonObject();

            if (!entry.has("name"))
                throwException("UBO '" + blockName + "' uniform entry [" + i + "] missing 'name'");

            if (!entry.has("type"))
                throwException("UBO '" + blockName + "' uniform entry [" + i + "] missing 'type'");

            String name = entry.get("name").getAsString();
            String type = entry.get("type").getAsString();
            int count = entry.has("count") ? entry.get("count").getAsInt() : 1;
            UniformType uniformType = parseUniformType(blockName, name, type);

            UniformData uniform = create(UniformData.class);
            uniform.constructor(uniformType, name, count);

            data.addUniform(uniform);
        }
    }

    private UniformType parseUniformType(String blockName, String uniformName, String raw) {
        try {
            return UniformType.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throwException("UBO '" + blockName + "' uniform '" + uniformName + "' has unknown type: " + raw);
            return null; // unreachable
        }
    }
}