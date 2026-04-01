package program.bootstrap.shaderpipeline.ubomanager;

import java.io.File;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import program.bootstrap.shaderpipeline.ubo.UBOData;
import program.bootstrap.shaderpipeline.ubo.UBOHandle;
import program.bootstrap.shaderpipeline.uniforms.UniformData;
import program.bootstrap.shaderpipeline.uniforms.UniformType;
import program.core.engine.BuilderPackage;
import program.core.util.JsonUtility;

/*
 * Parses UBO JSON descriptors into UBOHandles during bootstrap. Checks the
 * manager palette before creating anything — if the block is already registered
 * the existing handle is returned immediately and nothing is allocated.
 */
class InternalBuilder extends BuilderPackage {

    // Internal
    private UBOManager uboManager;

    // Base \\

    @Override
    protected void get() {
        this.uboManager = get(UBOManager.class);
    }

    // Build \\

    UBOHandle parse(File file) {

        JsonObject json = JsonUtility.loadJsonObject(file);
        String blockName = JsonUtility.validateString(json, "blockName");

        if (uboManager.hasUBO(blockName))
            return uboManager.getUBOHandleFromUBOName(blockName);

        int binding = json.has("binding")
                ? json.get("binding").getAsInt()
                : UBOData.UNSPECIFIED_BINDING;

        UBOData data = new UBOData(blockName, binding);
        UBOHandle handle = create(UBOHandle.class);
        handle.constructor(data);

        parseUniforms(json, handle, blockName);

        return handle;
    }

    private void parseUniforms(JsonObject json, UBOHandle handle, String blockName) {

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

            handle.addUniformDeclaration(
                    new UniformData(parseUniformType(blockName, name, type), name, count));
        }
    }

    private UniformType parseUniformType(String blockName, String uniformName, String raw) {

        try {
            return UniformType.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throwException("UBO '" + blockName + "' uniform '" + uniformName + "' has unknown type: " + raw);
            return null;
        }
    }
}