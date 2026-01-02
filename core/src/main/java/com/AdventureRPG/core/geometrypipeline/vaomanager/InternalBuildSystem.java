package com.AdventureRPG.core.geometrypipeline.vaomanager;

import java.io.File;

import com.AdventureRPG.core.util.FileUtility;
import com.AdventureRPG.core.util.JsonUtility;
import com.AdventureRPG.core.engine.SystemPackage;
import com.AdventureRPG.core.geometrypipeline.modelmanager.InternalLoadManager;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

class InternalBuildSystem extends SystemPackage {

    // Internal
    private VAOManager vaoManager;

    // Base \\

    @Override
    protected void get() {
        this.vaoManager = get(VAOManager.class);
    }

    // Build \\

    public VAOHandle addVAO(File file, InternalLoadManager loadManager) {
        String resourceName = FileUtility.getFileName(file);

        // Always check if VAO already exists first
        VAOHandle existing = vaoManager.getVAOHandleFromName(resourceName);
        if (existing != null) {
            return existing;
        }

        JsonObject json = loadJsonObject(file);
        JsonElement vaoElement = validateVAOElement(json, file);

        // Handle string reference
        if (vaoElement.isJsonPrimitive() && vaoElement.getAsJsonPrimitive().isString()) {
            String vaoName = vaoElement.getAsString();
            return getOrCreateVAO(vaoName, loadManager, file);
        }

        // Handle numeric stride
        if (vaoElement.isJsonPrimitive() && vaoElement.getAsJsonPrimitive().isNumber()) {
            return createVAOFromStride(vaoElement.getAsInt());
        }

        throwException(
                "VAO must be a string reference or numeric stride value in file: " + file.getName());

        return null;
    }

    // Validation \\

    private JsonObject loadJsonObject(File file) {
        return JsonUtility.loadJsonObject(file);
    }

    private JsonElement validateVAOElement(JsonObject json, File file) {
        if (!json.has("vao") || json.get("vao").isJsonNull()) {
            throwException(
                    "Missing or null 'vao' field in file: " + file.getName());
        }
        return json.get("vao");
    }

    // Resolution \\

    private VAOHandle getOrCreateVAO(String vaoName, InternalLoadManager loadManager, File currentFile) {
        // Check if it already exists in the manager
        VAOHandle existing = vaoManager.getVAOHandleFromName(vaoName);
        if (existing != null) {
            return existing;
        }

        // Not found - need to create it from the referenced file
        File referencedFile = loadManager.getFileByResourceName(vaoName);
        if (referencedFile == null) {
            throwException(
                    "VAO reference '" + vaoName + "' not found in file registry for file: " + currentFile.getName());
        }

        // Recursively build the referenced VAO
        vaoManager.addVAO(vaoName, referencedFile, loadManager);

        // Verify it was created
        existing = vaoManager.getVAOHandleFromName(vaoName);
        if (existing == null) {
            throwException(
                    "Failed to create VAO '" + vaoName + "' from file: " + referencedFile.getName());
        }

        return existing;
    }

    // Creation \\

    private VAOHandle createVAOFromStride(int vertStride) {
        if (vertStride <= 0) {
            throwException(
                    "VAO stride must be positive, got: " + vertStride);
        }
        return GLSLUtility.createVAO(vertStride);
    }
}