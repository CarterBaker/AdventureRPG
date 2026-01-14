package com.internal.bootstrap.geometrypipeline.vbomanager;

import java.io.File;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.internal.bootstrap.geometrypipeline.meshmanager.InternalLoadManager;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOHandle;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOManager;
import com.internal.core.engine.SystemPackage;
import com.internal.core.util.FileUtility;
import com.internal.core.util.JsonUtility;

class InternalBuildSystem extends SystemPackage {

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

    public VBOHandle addVBO(File file, InternalLoadManager loadManager) {
        String resourceName = FileUtility.getFileName(file);

        // Always check if VBO already exists first
        VBOHandle existing = vboManager.getVBOHandleFromName(resourceName);
        if (existing != null) {
            return existing;
        }

        JsonObject json = loadJsonObject(file);
        VAOHandle vaoHandle = resolveVAO(json, file, loadManager, resourceName);
        JsonElement vboElement = validateVBOElement(json, file);

        // Handle string reference
        if (vboElement.isJsonPrimitive() && vboElement.getAsJsonPrimitive().isString()) {
            String vboName = vboElement.getAsString();
            return getOrCreateVBO(vboName, loadManager, file);
        }

        // Handle vertex data array
        if (vboElement.isJsonArray()) {
            return createVBOFromData(vaoHandle, vboElement.getAsJsonArray(), file);
        }

        throwException(
                "VBO must be a string reference or vertex data array in file: " + file.getName());

        return null;
    }

    // Validation \\

    private JsonObject loadJsonObject(File file) {
        return JsonUtility.loadJsonObject(file);
    }

    private JsonElement validateVBOElement(JsonObject json, File file) {
        if (!json.has("vbo") || json.get("vbo").isJsonNull()) {
            throwException(
                    "Missing or null 'vbo' field in file: " + file.getName());
        }
        return json.get("vbo");
    }

    // VAO Resolution \\

    private VAOHandle resolveVAO(JsonObject json, File file, InternalLoadManager loadManager,
            String resourceName) {
        if (!json.has("vao") || json.get("vao").isJsonNull()) {
            throwException(
                    "VBO requires 'vao' field in file: " + file.getName());
        }

        JsonElement vaoElement = json.get("vao");

        // Handle string reference
        if (vaoElement.isJsonPrimitive() && vaoElement.getAsJsonPrimitive().isString()) {
            String vaoName = vaoElement.getAsString();
            return getOrCreateVAO(vaoName, loadManager, file);
        }

        // Handle numeric stride - check if already exists first
        VAOHandle existing = vaoManager.getVAOHandleFromName(resourceName);
        if (existing != null) {
            return existing;
        }

        // Create new VAO for this resource
        vaoManager.addVAO(resourceName, file, loadManager);
        return vaoManager.getVAOHandleFromName(resourceName);
    }

    private VAOHandle getOrCreateVAO(String vaoName, InternalLoadManager loadManager, File currentFile) {
        // Check if it already exists
        VAOHandle existing = vaoManager.getVAOHandleFromName(vaoName);
        if (existing != null) {
            return existing;
        }

        // Not found - create it from the referenced file
        File referencedFile = loadManager.getFileByResourceName(vaoName);
        if (referencedFile == null) {
            throwException(
                    "VAO reference '" + vaoName + "' not found in file registry for file: " + currentFile.getName());
        }

        vaoManager.addVAO(vaoName, referencedFile, loadManager);

        existing = vaoManager.getVAOHandleFromName(vaoName);
        if (existing == null) {
            throwException(
                    "Failed to create VAO '" + vaoName + "' from file: " + referencedFile.getName());
        }

        return existing;
    }

    // VBO Resolution \\

    private VBOHandle getOrCreateVBO(String vboName, InternalLoadManager loadManager, File currentFile) {
        // Check if it already exists
        VBOHandle existing = vboManager.getVBOHandleFromName(vboName);
        if (existing != null) {
            return existing;
        }

        // Not found - create it from the referenced file
        File referencedFile = loadManager.getFileByResourceName(vboName);
        if (referencedFile == null) {
            throwException(
                    "VBO reference '" + vboName + "' not found in file registry for file: " + currentFile.getName());
        }

        vboManager.addVBO(vboName, referencedFile, loadManager);

        existing = vboManager.getVBOHandleFromName(vboName);
        if (existing == null) {
            throwException(
                    "Failed to create VBO '" + vboName + "' from file: " + referencedFile.getName());
        }

        return existing;
    }

    // VBO Creation \\

    private VBOHandle createVBOFromData(VAOHandle vaoHandle, JsonArray verticesArray, File file) {
        if (verticesArray.size() == 0) {
            throwException(
                    "Vertex data array cannot be empty in file: " + file.getName());
        }

        float[] vertices = parseVertexData(verticesArray, vaoHandle, file);
        return GLSLUtility.uploadVertexData(vaoHandle, create(VBOHandle.class), vertices);
    }

    private float[] parseVertexData(JsonArray verticesArray, VAOHandle vaoHandle, File file) {
        int vertexCount = verticesArray.size();
        int floatsPerVertex = vaoHandle.getVertStride();
        float[] vertices = new float[vertexCount * floatsPerVertex];

        int index = 0;
        for (JsonElement vertexElement : verticesArray) {
            JsonArray vertex = vertexElement.getAsJsonArray();

            if (vertex.size() != floatsPerVertex) {
                throwException(
                        "Vertex attribute count mismatch. Expected " + floatsPerVertex +
                                " floats but got " + vertex.size() + " in file: " + file.getName());
            }

            for (JsonElement val : vertex) {
                vertices[index++] = val.getAsFloat();
            }
        }

        return vertices;
    }
}