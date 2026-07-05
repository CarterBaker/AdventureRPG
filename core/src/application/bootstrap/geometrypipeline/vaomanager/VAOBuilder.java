package application.bootstrap.geometrypipeline.vaomanager;

import java.io.File;
import java.util.Map;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import application.bootstrap.geometrypipeline.vao.VAOHandle;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.io.JsonUtility;

public class VAOBuilder extends BuilderPackage {

    /*
     * Parses the 'vao' field from mesh JSON and constructs a VAOHandle layout
     * template. Supports direct attribute size arrays and string references to
     * other registered VAOs. When the mesh JSON declares a "rig", two extra
     * trailing attributes — bone indices and bone weights, each
     * EngineSetting.MAX_BONE_INFLUENCES floats wide — are appended after the
     * declared attributes. Bootstrap-only.
     */

    // Internal
    private VAOManager vaoManager;

    // Base \\

    @Override
    protected void get() {

        // Internal
        this.vaoManager = get(VAOManager.class);
    }

    // Build \\

    public void build(
            String resourceName,
            File file,
            Map<String, File> registry) {

        if (vaoManager.hasVAO(resourceName))
            return;

        JsonObject json = JsonUtility.loadJsonObject(file);

        if (!json.has("vao") || json.get("vao").isJsonNull())
            return;

        JsonElement vaoEl = json.get("vao");
        boolean hasBones = hasRig(json);

        if (vaoEl.isJsonPrimitive() && vaoEl.getAsJsonPrimitive().isString()) {

            if (hasBones)
                throwException("Bone-weighted mesh must declare its own inline \"vao\" array — "
                        + "cannot reference a shared VAO template, since that would change the "
                        + "stride for every other mesh sharing it. File: " + file.getName());

            String refName = vaoEl.getAsString();
            resolveRef(refName, file, registry);
            vaoManager.registerVAO(resourceName, vaoManager.getVAOHandleDirect(refName));
            return;
        }

        if (vaoEl.isJsonArray()) {
            vaoManager.registerVAO(resourceName, buildLayout(vaoEl.getAsJsonArray(), file, hasBones));
            return;
        }

        throwException("VAO must be a string reference or int array in file: " + file.getName());
    }

    // Resolution \\

    private void resolveRef(
            String refName,
            File sourceFile,
            Map<String, File> registry) {

        if (vaoManager.hasVAO(refName))
            return;

        File refFile = registry.get(refName);

        if (refFile == null)
            throwException("Referenced VAO '" + refName + "' not found. Source: " + sourceFile.getName());

        JsonObject refJson = JsonUtility.loadJsonObject(refFile);

        if (!refJson.has("vao") || refJson.get("vao").isJsonNull())
            throwException("Referenced VAO file '" + refName + "' has no 'vao' field.");

        JsonElement refEl = refJson.get("vao");

        if (!refEl.isJsonArray())
            throwException("Referenced VAO '" + refName + "' must contain an int array.");

        vaoManager.registerVAO(refName, buildLayout(refEl.getAsJsonArray(), refFile, hasRig(refJson)));
    }

    // Creation \\

    private VAOHandle buildLayout(JsonArray jsonArray, File file, boolean hasBones) {

        if (jsonArray.size() == 0)
            throwException("VAO attribute size array must not be empty in file: " + file.getName());

        int declaredCount = jsonArray.size();
        int totalCount = hasBones ? declaredCount + 2 : declaredCount;
        int[] attrSizes = new int[totalCount];

        for (int i = 0; i < declaredCount; i++) {
            attrSizes[i] = jsonArray.get(i).getAsInt();
            if (attrSizes[i] <= 0)
                throwException("VAO attribute size must be positive in file: " + file.getName());
        }

        if (hasBones) {
            attrSizes[declaredCount] = EngineSetting.MAX_BONE_INFLUENCES;
            attrSizes[declaredCount + 1] = EngineSetting.MAX_BONE_INFLUENCES;
        }

        VAOHandle handle = create(VAOHandle.class);
        handle.constructor(attrSizes);

        return handle;
    }

    // Utility \\

    private boolean hasRig(JsonObject json) {
        return json.has("rig") && !json.get("rig").isJsonNull();
    }
}