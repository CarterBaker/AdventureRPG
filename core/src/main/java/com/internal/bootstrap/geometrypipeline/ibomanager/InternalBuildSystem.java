package com.internal.bootstrap.geometrypipeline.ibomanager;

import java.io.File;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.internal.bootstrap.geometrypipeline.ibo.IBOHandle;
import com.internal.bootstrap.geometrypipeline.meshmanager.InternalLoadManager;
import com.internal.bootstrap.geometrypipeline.vao.VAOInstance;
import com.internal.core.engine.SystemPackage;
import com.internal.core.util.FileUtility;
import com.internal.core.util.JsonUtility;

class InternalBuildSystem extends SystemPackage {

    // Internal
    private IBOManager iboManager;

    // Base \\

    @Override
    protected void get() {
        this.iboManager = get(IBOManager.class);
    }

    // Build \\

    IBOHandle addIBO(File file, InternalLoadManager loadManager, VAOInstance vaoInstance) {

        String resourceName = FileUtility.getFileName(file);

        IBOHandle existing = iboManager.getIBOHandleFromName(resourceName);
        if (existing != null)
            return existing;

        JsonObject json = JsonUtility.loadJsonObject(file);
        JsonElement iboElement = validateIBOElement(json, file);

        if (iboElement.isJsonPrimitive() && iboElement.getAsJsonPrimitive().isString()) {
            String iboName = iboElement.getAsString();
            return getOrCreateIBO(iboName, loadManager, file, vaoInstance);
        }

        if (iboElement.isJsonArray())
            return createIBOFromData(vaoInstance, iboElement.getAsJsonArray(), file);

        throwException("IBO must be a string reference or index data array in file: " + file.getName());
        return null;
    }

    // Validation \\

    private JsonElement validateIBOElement(JsonObject json, File file) {

        if (!json.has("ibo") || json.get("ibo").isJsonNull())
            throwException("Missing or null 'ibo' field in file: " + file.getName());

        return json.get("ibo");
    }

    // Resolution \\

    private IBOHandle getOrCreateIBO(
            String iboName,
            InternalLoadManager loadManager,
            File currentFile,
            VAOInstance vaoInstance) {

        IBOHandle existing = iboManager.getIBOHandleFromName(iboName);
        if (existing != null)
            return existing;

        File referencedFile = loadManager.getFileByResourceName(iboName);
        if (referencedFile == null)
            throwException(
                    "IBO reference '" + iboName + "' not found in file registry for file: " + currentFile.getName());

        iboManager.addIBO(iboName, referencedFile, loadManager, vaoInstance);

        existing = iboManager.getIBOHandleFromName(iboName);
        if (existing == null)
            throwException("Failed to create IBO '" + iboName + "' from file: " + referencedFile.getName());

        return existing;
    }

    // Creation \\

    private IBOHandle createIBOFromData(VAOInstance vaoInstance, JsonArray indicesArray, File file) {

        if (indicesArray.size() == 0)
            throwException("Index data array cannot be empty in file: " + file.getName());

        short[] indices = parseIndexData(indicesArray, file);
        return GLSLUtility.uploadIndexData(vaoInstance, create(IBOHandle.class), indices);
    }

    private short[] parseIndexData(JsonArray indicesArray, File file) {

        short[] indices = new short[indicesArray.size()];
        int index = 0;

        for (JsonElement indexElement : indicesArray) {

            int value = indexElement.getAsInt();

            if (value < 0 || value > 65535)
                throwException("Index value out of range for 16-bit: " + value + " in file: " + file.getName());

            indices[index++] = (short) value;
        }

        return indices;
    }
}