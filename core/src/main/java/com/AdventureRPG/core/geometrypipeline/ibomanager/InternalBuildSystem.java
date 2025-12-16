package com.AdventureRPG.core.geometrypipeline.ibomanager;

import java.io.File;

import com.AdventureRPG.core.kernel.SystemFrame;
import com.AdventureRPG.core.util.FileUtility;
import com.AdventureRPG.core.util.JsonUtility;
import com.AdventureRPG.core.geometrypipeline.vaomanager.VAOHandle;
import com.AdventureRPG.core.geometrypipeline.vaomanager.VAOManager;
import com.AdventureRPG.core.geometrypipeline.modelmanager.InternalLoadManager;
import com.AdventureRPG.core.util.Exceptions.FileException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

class InternalBuildSystem extends SystemFrame {

    // Internal
    private IBOManager iboManager;
    private VAOManager vaoManager;

    // Base \\

    @Override
    protected void init() {
        this.iboManager = gameEngine.get(IBOManager.class);
        this.vaoManager = gameEngine.get(VAOManager.class);
    }

    // Build \\

    public IBOHandle addIBO(File file, InternalLoadManager loadManager) {
        String resourceName = FileUtility.getFileName(file);

        // Always check if IBO already exists first
        IBOHandle existing = iboManager.getIBOHandleFromName(resourceName);
        if (existing != null) {
            return existing;
        }

        JsonObject json = loadJsonObject(file);
        VAOHandle vaoHandle = resolveVAO(json, file, loadManager, resourceName);
        JsonElement iboElement = validateIBOElement(json, file);

        // Handle string reference
        if (iboElement.isJsonPrimitive() && iboElement.getAsJsonPrimitive().isString()) {
            String iboName = iboElement.getAsString();
            return getOrCreateIBO(iboName, loadManager, file);
        }

        // Handle index data array
        if (iboElement.isJsonArray()) {
            return createIBOFromData(vaoHandle, iboElement.getAsJsonArray(), file);
        }

        throw new FileException.FileReadException(
                "IBO must be a string reference or index data array in file: " + file.getName());
    }

    // Validation \\

    private JsonObject loadJsonObject(File file) {
        return JsonUtility.loadJsonObject(file);
    }

    private JsonElement validateIBOElement(JsonObject json, File file) {
        if (!json.has("ibo") || json.get("ibo").isJsonNull()) {
            throw new FileException.FileReadException(
                    "Missing or null 'ibo' field in file: " + file.getName());
        }
        return json.get("ibo");
    }

    // VAO Resolution \\

    private VAOHandle resolveVAO(JsonObject json, File file, InternalLoadManager loadManager, String resourceName) {
        if (!json.has("vao") || json.get("vao").isJsonNull()) {
            throw new FileException.FileReadException(
                    "IBO requires 'vao' field in file: " + file.getName());
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
            throw new FileException.FileReadException(
                    "VAO reference '" + vaoName + "' not found in file registry for file: " + currentFile.getName());
        }

        vaoManager.addVAO(vaoName, referencedFile, loadManager);

        existing = vaoManager.getVAOHandleFromName(vaoName);
        if (existing == null) {
            throw new FileException.FileReadException(
                    "Failed to create VAO '" + vaoName + "' from file: " + referencedFile.getName());
        }

        return existing;
    }

    // IBO Resolution \\

    private IBOHandle getOrCreateIBO(String iboName, InternalLoadManager loadManager, File currentFile) {
        // Check if it already exists
        IBOHandle existing = iboManager.getIBOHandleFromName(iboName);
        if (existing != null) {
            return existing;
        }

        // Not found - create it from the referenced file
        File referencedFile = loadManager.getFileByResourceName(iboName);
        if (referencedFile == null) {
            throw new FileException.FileReadException(
                    "IBO reference '" + iboName + "' not found in file registry for file: " + currentFile.getName());
        }

        iboManager.addIBO(iboName, referencedFile, loadManager);

        existing = iboManager.getIBOHandleFromName(iboName);
        if (existing == null) {
            throw new FileException.FileReadException(
                    "Failed to create IBO '" + iboName + "' from file: " + referencedFile.getName());
        }

        return existing;
    }

    // IBO Creation \\

    private IBOHandle createIBOFromData(VAOHandle vaoHandle, JsonArray indicesArray, File file) {
        if (indicesArray.size() == 0) {
            throw new FileException.FileReadException(
                    "Index data array cannot be empty in file: " + file.getName());
        }

        short[] indices = parseIndexData(indicesArray, file);
        return GLSLUtility.uploadIndexData(vaoHandle, indices);
    }

    private short[] parseIndexData(JsonArray indicesArray, File file) {
        int indexCount = indicesArray.size();
        short[] indices = new short[indexCount];

        int index = 0;
        for (JsonElement indexElement : indicesArray) {
            int value = indexElement.getAsInt();

            if (value < 0 || value > 65535) {
                throw new FileException.FileReadException(
                        "Index value out of range for 16-bit: " + value + " in file: " + file.getName());
            }

            indices[index++] = (short) value;
        }

        return indices;
    }
}