package com.internal.bootstrap.geometrypipeline.ibomanager;

import java.io.File;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.internal.bootstrap.geometrypipeline.ibo.IBOHandle;
import com.internal.bootstrap.geometrypipeline.vao.VAOHandle;
import com.internal.bootstrap.geometrypipeline.vao.VAOInstance;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOManager;
import com.internal.core.engine.BuilderPackage;
import com.internal.core.util.JsonUtility;

public class InternalBuilder extends BuilderPackage {

    // Internal
    private IBOManager iboManager;
    private VAOManager vaoManager;

    // Base \\

    @Override
    protected void get() {
        this.iboManager = get(IBOManager.class);
        this.vaoManager = get(VAOManager.class);
    }

    // Build \\

    public void build(String resourceName, File file, Map<String, File> registry) {

        if (iboManager.hasIBO(resourceName))
            return;

        JsonObject json = JsonUtility.loadJsonObject(file);

        if (hasQuadEntries(json))
            return;

        if (!json.has("ibo") || json.get("ibo").isJsonNull())
            return;

        JsonElement iboEl = json.get("ibo");

        if (iboEl.isJsonPrimitive() && iboEl.getAsJsonPrimitive().isString()) {
            resolveRef(iboEl.getAsString(), resourceName, file, registry);
            return;
        }

        if (iboEl.isJsonArray()) {
            VAOInstance vaoInstance = getVAOInstance(resourceName, file);
            iboManager.registerIBO(resourceName, buildFromData(iboEl.getAsJsonArray(), vaoInstance, file));
            return;
        }

        throwException("IBO must be a string reference or index array in file: " + file.getName());
    }

    // Resolution \\

    private void resolveRef(String refName, String sourceResourceName, File sourceFile, Map<String, File> registry) {

        if (iboManager.hasIBO(refName))
            return;

        File refFile = registry.get(refName);
        if (refFile == null)
            throwException("Referenced IBO '" + refName + "' not found. Source: " + sourceFile.getName());

        JsonObject refJson = JsonUtility.loadJsonObject(refFile);

        if (!refJson.has("ibo") || refJson.get("ibo").isJsonNull())
            throwException("Referenced IBO file '" + refName + "' has no 'ibo' field.");

        JsonElement refEl = refJson.get("ibo");

        if (!refEl.isJsonArray())
            throwException("Referenced IBO '" + refName + "' must contain an index array.");

        VAOInstance vaoInstance = getVAOInstance(sourceResourceName, sourceFile);
        iboManager.registerIBO(refName, buildFromData(refEl.getAsJsonArray(), vaoInstance, refFile));
    }

    // Creation \\

    private IBOHandle buildFromData(JsonArray indicesArray, VAOInstance vaoInstance, File file) {

        if (indicesArray.size() == 0)
            throwException("Index data array cannot be empty in file: " + file.getName());

        short[] indices = new short[indicesArray.size()];
        int index = 0;

        for (JsonElement indexEl : indicesArray) {
            int value = indexEl.getAsInt();
            if (value < 0 || value > 65535)
                throwException("Index out of 16-bit range: " + value + " in file: " + file.getName());
            indices[index++] = (short) value;
        }

        return GLSLUtility.uploadIndexData(vaoInstance, create(IBOHandle.class), indices);
    }

    // Utility \\

    private VAOInstance getVAOInstance(String resourceName, File file) {
        VAOHandle vaoHandle = vaoManager.getVAOHandleFromName(resourceName);
        if (vaoHandle == null)
            throwException("VAO not yet registered for '" + resourceName + "' when building IBO in: " + file.getName());
        return vaoManager.createVAOInstance(vaoHandle);
    }

    private boolean hasQuadEntries(JsonObject json) {
        if (!json.has("vbo") || json.get("vbo").isJsonNull())
            return false;
        JsonElement vboEl = json.get("vbo");
        if (!vboEl.isJsonArray())
            return false;
        for (JsonElement el : vboEl.getAsJsonArray())
            if (el.isJsonObject())
                return true;
        return false;
    }
}