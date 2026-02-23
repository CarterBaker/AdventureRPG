package com.internal.bootstrap.geometrypipeline.vaomanager;

import java.io.File;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.internal.bootstrap.geometrypipeline.meshmanager.InternalLoadManager;
import com.internal.core.engine.SystemPackage;
import com.internal.core.util.FileUtility;
import com.internal.core.util.JsonUtility;

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

        VAOHandle existing = vaoManager.getVAOHandleFromName(resourceName);
        if (existing != null)
            return existing;

        JsonObject json = loadJsonObject(file);
        JsonElement vaoElement = validateVAOElement(json, file);

        // Handle string reference
        if (vaoElement.isJsonPrimitive() && vaoElement.getAsJsonPrimitive().isString()) {
            String vaoName = vaoElement.getAsString();
            return getOrCreateVAO(vaoName, loadManager, file);
        }

        // Handle int array of attribute sizes e.g. [3, 3, 1, 2]
        if (vaoElement.isJsonArray()) {
            return createVAOFromAttrSizes(vaoElement.getAsJsonArray(), file);
        }

        throwException("VAO must be a string reference or int array of attribute sizes in file: " + file.getName());
        return null;
    }

    // Validation \\

    private JsonObject loadJsonObject(File file) {
        return JsonUtility.loadJsonObject(file);
    }

    private JsonElement validateVAOElement(JsonObject json, File file) {
        if (!json.has("vao") || json.get("vao").isJsonNull())
            throwException("Missing or null 'vao' field in file: " + file.getName());
        return json.get("vao");
    }

    // Resolution \\

    private VAOHandle getOrCreateVAO(String vaoName, InternalLoadManager loadManager, File currentFile) {

        VAOHandle existing = vaoManager.getVAOHandleFromName(vaoName);
        if (existing != null)
            return existing;

        File referencedFile = loadManager.getFileByResourceName(vaoName);
        if (referencedFile == null)
            throwException(
                    "VAO reference '" + vaoName + "' not found in file registry for file: " + currentFile.getName());

        vaoManager.addVAO(vaoName, referencedFile, loadManager);

        existing = vaoManager.getVAOHandleFromName(vaoName);
        if (existing == null)
            throwException("Failed to create VAO '" + vaoName + "' from file: " + referencedFile.getName());

        return existing;
    }

    // Creation \\

    private VAOHandle createVAOFromAttrSizes(JsonArray jsonArray, File file) {

        if (jsonArray.size() == 0)
            throwException("VAO attribute size array must not be empty in file: " + file.getName());

        int[] attrSizes = new int[jsonArray.size()];
        for (int i = 0; i < jsonArray.size(); i++) {
            attrSizes[i] = jsonArray.get(i).getAsInt();
            if (attrSizes[i] <= 0)
                throwException("VAO attribute size must be positive in file: " + file.getName());
        }

        return GLSLUtility.createVAO(create(VAOHandle.class), attrSizes);
    }
}