package com.internal.bootstrap.geometrypipeline.vbomanager;

import java.io.File;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.internal.bootstrap.geometrypipeline.meshmanager.InternalLoadManager;
import com.internal.bootstrap.geometrypipeline.vao.VAOInstance;
import com.internal.bootstrap.geometrypipeline.vbo.VBOHandle;
import com.internal.core.engine.SystemPackage;
import com.internal.core.util.FileUtility;
import com.internal.core.util.JsonUtility;

class InternalBuildSystem extends SystemPackage {

    // Internal
    private VBOManager vboManager;

    // Base \\

    @Override
    protected void get() {
        this.vboManager = get(VBOManager.class);
    }

    // Build \\

    VBOHandle addVBO(File file, InternalLoadManager loadManager, VAOInstance vaoInstance) {

        String resourceName = FileUtility.getFileName(file);

        VBOHandle existing = vboManager.getVBOHandleFromName(resourceName);
        if (existing != null)
            return existing;

        JsonObject json = JsonUtility.loadJsonObject(file);
        JsonElement vboElement = validateVBOElement(json, file);

        if (vboElement.isJsonPrimitive() && vboElement.getAsJsonPrimitive().isString()) {
            String vboName = vboElement.getAsString();
            return getOrCreateVBO(vboName, loadManager, file, vaoInstance);
        }

        if (vboElement.isJsonArray())
            return createVBOFromData(vaoInstance, vboElement.getAsJsonArray(), file);

        throwException("VBO must be a string reference or vertex data array in file: " + file.getName());
        return null;
    }

    // Validation \\

    private JsonElement validateVBOElement(JsonObject json, File file) {

        if (!json.has("vbo") || json.get("vbo").isJsonNull())
            throwException("Missing or null 'vbo' field in file: " + file.getName());

        return json.get("vbo");
    }

    // Resolution \\

    private VBOHandle getOrCreateVBO(
            String vboName,
            InternalLoadManager loadManager,
            File currentFile,
            VAOInstance vaoInstance) {

        VBOHandle existing = vboManager.getVBOHandleFromName(vboName);
        if (existing != null)
            return existing;

        File referencedFile = loadManager.getFileByResourceName(vboName);
        if (referencedFile == null)
            throwException(
                    "VBO reference '" + vboName + "' not found in file registry for file: " + currentFile.getName());

        vboManager.addVBO(vboName, referencedFile, loadManager, vaoInstance);

        existing = vboManager.getVBOHandleFromName(vboName);
        if (existing == null)
            throwException("Failed to create VBO '" + vboName + "' from file: " + referencedFile.getName());

        return existing;
    }

    // Creation \\

    private VBOHandle createVBOFromData(VAOInstance vaoInstance, JsonArray verticesArray, File file) {

        if (verticesArray.size() == 0)
            throwException("Vertex data array cannot be empty in file: " + file.getName());

        float[] vertices = parseVertexData(verticesArray, vaoInstance, file);
        return GLSLUtility.uploadVertexData(vaoInstance, create(VBOHandle.class), vertices);
    }

    private float[] parseVertexData(JsonArray verticesArray, VAOInstance vaoInstance, File file) {

        int floatsPerVertex = vaoInstance.getVAOStruct().vertStride;
        float[] vertices = new float[verticesArray.size() * floatsPerVertex];
        int index = 0;

        for (JsonElement vertexElement : verticesArray) {

            JsonArray vertex = vertexElement.getAsJsonArray();

            if (vertex.size() != floatsPerVertex)
                throwException("Vertex attribute count mismatch. Expected " + floatsPerVertex +
                        " floats but got " + vertex.size() + " in file: " + file.getName());

            for (JsonElement val : vertex)
                vertices[index++] = val.getAsFloat();
        }

        return vertices;
    }
}