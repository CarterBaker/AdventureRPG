package application.bootstrap.shaderpipeline.ubomanager;

import java.io.File;

import application.bootstrap.shaderpipeline.ubo.UBOData;
import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.uniforms.UniformData;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.BuilderPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;

class UBOBuilder extends BuilderPackage {

    /*
     * Parses UBO ARPG descriptors into UBOHandles during bootstrap. Checks the
     * manager palette before creating anything — if the block is already registered
     * the existing handle is returned immediately and nothing is allocated.
     */

    // Internal
    private UBOManager uboManager;

    // Base \\

    @Override
    protected void get() {
        this.uboManager = get(UBOManager.class);
    }

    // Build \\

    UBOHandle parse(File file) {

        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);
        String blockName = ArpgUtility.validateString(arpg, "blockName");

        if (uboManager.hasUBO(blockName))
            return uboManager.getUBOHandleFromUBOName(blockName);

        int binding = arpg.has("binding")
                ? arpg.get("binding").getAsInt()
                : UBOData.UNSPECIFIED_BINDING;

        UBOData data = new UBOData(blockName, binding);
        UBOHandle handle = create(UBOHandle.class);
        handle.constructor(data);

        parseUniforms(arpg, handle, blockName);

        return handle;
    }

    private void parseUniforms(ArpgObjectStruct arpg, UBOHandle handle, String blockName) {

        if (!arpg.has("uniforms"))
            throwException("UBO '" + blockName + "' ARPG is missing required 'uniforms' array");

        ArpgArrayStruct array = arpg.getAsArray("uniforms");

        for (int i = 0; i < array.size(); i++) {

            ArpgObjectStruct entry = array.get(i).getAsObject();

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