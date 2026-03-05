package com.internal.bootstrap.geometrypipeline.vaomanager;

import java.io.File;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.internal.bootstrap.geometrypipeline.vao.VAOHandle;
import com.internal.core.engine.BuilderPackage;
import com.internal.core.util.JsonUtility;

public class InternalBuilder extends BuilderPackage {

    // Internal
    private VAOManager vaoManager;

    // Base \\

    @Override
    protected void get() {
        this.vaoManager = get(VAOManager.class);
    }

    // Build \\

    public void build(String resourceName, File file, Map<String, File> registry) {

        // hasVAO — pure lookup, no load trigger. Calling getVAOHandleFromName
        // here would recurse back into the mesh loader that is calling us.
        if (vaoManager.hasVAO(resourceName))
            return;

        JsonObject json = JsonUtility.loadJsonObject(file);

        if (!json.has("vao") || json.get("vao").isJsonNull())
            return;

        JsonElement vaoEl = json.get("vao");

        if (vaoEl.isJsonPrimitive() && vaoEl.getAsJsonPrimitive().isString()) {
            resolveRef(vaoEl.getAsString(), file, registry);
            return;
        }

        if (vaoEl.isJsonArray()) {
            vaoManager.registerVAO(resourceName, buildLayout(vaoEl.getAsJsonArray(), file));
            return;
        }

        throwException("VAO must be a string reference or int array in file: " + file.getName());
    }

    // Resolution \\

    private void resolveRef(String refName, File sourceFile, Map<String, File> registry) {

        // Ref points to a different resource — hasVAO is still correct here.
        // If not present, we load the ref file directly rather than triggering
        // the mesh loader's request path, so no recursion risk.
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

        vaoManager.registerVAO(refName, buildLayout(refEl.getAsJsonArray(), refFile));
    }

    // Creation \\

    private VAOHandle buildLayout(JsonArray jsonArray, File file) {

        if (jsonArray.size() == 0)
            throwException("VAO attribute size array must not be empty in file: " + file.getName());

        int[] attrSizes = new int[jsonArray.size()];
        for (int i = 0; i < jsonArray.size(); i++) {
            attrSizes[i] = jsonArray.get(i).getAsInt();
            if (attrSizes[i] <= 0)
                throwException("VAO attribute size must be positive in file: " + file.getName());
        }

        VAOHandle handle = create(VAOHandle.class);
        handle.constructor(attrSizes);
        return handle;
    }
}