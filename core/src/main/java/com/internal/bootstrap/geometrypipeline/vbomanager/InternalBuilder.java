package com.internal.bootstrap.geometrypipeline.vbomanager;

import java.io.File;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.internal.bootstrap.geometrypipeline.vao.VAOHandle;
import com.internal.bootstrap.geometrypipeline.vao.VAOInstance;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOManager;
import com.internal.bootstrap.geometrypipeline.vbo.VBOHandle;
import com.internal.core.engine.BuilderPackage;
import com.internal.core.util.JsonUtility;

public class InternalBuilder extends BuilderPackage {

    // Internal
    private VBOManager vboManager;
    private VAOManager vaoManager;

    // Base \\

    @Override
    protected void get() {
        this.vboManager = get(VBOManager.class);
        this.vaoManager = get(VAOManager.class);
    }

    // Build \\

    public void build(String resourceName, File file, Map<String, File> registry) {

        if (vboManager.hasVBO(resourceName))
            return;

        JsonObject json = JsonUtility.loadJsonObject(file);

        if (!json.has("vbo") || json.get("vbo").isJsonNull())
            return;

        JsonElement vboEl = json.get("vbo");

        if (vboEl.isJsonArray() && containsQuadObjects(vboEl.getAsJsonArray()))
            return;

        if (vboEl.isJsonPrimitive() && vboEl.getAsJsonPrimitive().isString()) {
            resolveRef(vboEl.getAsString(), resourceName, file, registry);
            return;
        }

        if (vboEl.isJsonArray()) {
            VAOInstance vaoInstance = getVAOInstance(resourceName, file);
            vboManager.registerVBO(resourceName, buildFromData(vboEl.getAsJsonArray(), vaoInstance, file));
            return;
        }

        throwException("VBO must be a string reference or vertex array in file: " + file.getName());
    }

    // Resolution \\

    private void resolveRef(String refName, String sourceResourceName, File sourceFile, Map<String, File> registry) {

        if (vboManager.hasVBO(refName))
            return;

        File refFile = registry.get(refName);
        if (refFile == null)
            throwException("Referenced VBO '" + refName + "' not found. Source: " + sourceFile.getName());

        JsonObject refJson = JsonUtility.loadJsonObject(refFile);

        if (!refJson.has("vbo") || refJson.get("vbo").isJsonNull())
            throwException("Referenced VBO file '" + refName + "' has no 'vbo' field.");

        JsonElement refEl = refJson.get("vbo");

        if (!refEl.isJsonArray())
            throwException("Referenced VBO '" + refName + "' must contain a vertex array.");

        VAOInstance vaoInstance = getVAOInstance(sourceResourceName, sourceFile);
        vboManager.registerVBO(refName, buildFromData(refEl.getAsJsonArray(), vaoInstance, refFile));
    }

    // Creation \\

    private VBOHandle buildFromData(JsonArray verticesArray, VAOInstance vaoInstance, File file) {

        if (verticesArray.size() == 0)
            throwException("Vertex data array cannot be empty in file: " + file.getName());

        int floatsPerVertex = vaoInstance.getVAOStruct().vertStride;
        float[] vertices = new float[verticesArray.size() * floatsPerVertex];
        int index = 0;

        for (JsonElement vertexEl : verticesArray) {
            JsonArray vertex = vertexEl.getAsJsonArray();
            if (vertex.size() != floatsPerVertex)
                throwException("Vertex attribute count mismatch. Expected " + floatsPerVertex
                        + " floats but got " + vertex.size() + " in file: " + file.getName());
            for (JsonElement val : vertex)
                vertices[index++] = val.getAsFloat();
        }

        return GLSLUtility.uploadVertexData(vaoInstance, create(VBOHandle.class), vertices);
    }

    // Utility \\

    private VAOInstance getVAOInstance(String resourceName, File file) {
        VAOHandle vaoHandle = vaoManager.getVAOHandleFromName(resourceName);
        if (vaoHandle == null)
            throwException("VAO not yet registered for '" + resourceName + "' when building VBO in: " + file.getName());
        return vaoManager.createVAOInstance(vaoHandle);
    }

    private boolean containsQuadObjects(JsonArray array) {
        for (JsonElement el : array)
            if (el.isJsonObject())
                return true;
        return false;
    }
}