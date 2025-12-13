package com.AdventureRPG.core.geometrypipeline.modelmanager;

import java.io.File;

import com.AdventureRPG.core.kernel.SystemFrame;
import com.AdventureRPG.core.geometrypipeline.ibomanager.IBOHandle;
import com.AdventureRPG.core.geometrypipeline.ibomanager.IBOManager;
import com.AdventureRPG.core.geometrypipeline.vaomanager.VAOHandle;
import com.AdventureRPG.core.geometrypipeline.vaomanager.VAOManager;
import com.AdventureRPG.core.geometrypipeline.vbomanager.VBOHandle;
import com.AdventureRPG.core.geometrypipeline.vbomanager.VBOManager;
import com.AdventureRPG.core.util.FileUtility;
import com.AdventureRPG.core.util.JsonUtility;
import com.google.gson.JsonObject;

class InternalBuildSystem extends SystemFrame {

    // Internal
    private VBOManager vboManager;
    private IBOManager iboManager;
    private VAOManager vaoManager;

    // Base \\

    @Override
    protected void init() {
        this.vboManager = gameEngine.get(VBOManager.class);
        this.iboManager = gameEngine.get(IBOManager.class);
        this.vaoManager = gameEngine.get(VAOManager.class);
    }

    // Build \\

    MeshHandle buildMeshHandle(File file, int meshID, InternalLoadManager loadManager) {
        JsonObject jsonObject = loadJsonFromFile(file);
        String resourceName = FileUtility.getFileName(file);

        VAOHandle vaoHandle = buildVAOHandle(resourceName, jsonObject, file, loadManager);
        VBOHandle vboHandle = buildVBOHandle(resourceName, jsonObject, file, loadManager);
        IBOHandle iboHandle = buildIBOHandle(resourceName, jsonObject, file, loadManager);

        return createMeshHandle(vaoHandle, vboHandle, iboHandle);
    }

    private JsonObject loadJsonFromFile(File file) {
        return JsonUtility.loadJsonObject(file);
    }

    private MeshHandle createMeshHandle(VAOHandle vaoHandle, VBOHandle vboHandle, IBOHandle iboHandle) {
        if (vaoHandle == null || vboHandle == null || iboHandle == null) {
            return null;
        }

        return new MeshHandle(
                vaoHandle.attributeHandle,
                vaoHandle.vertStride,
                vboHandle.vertexHandle,
                vboHandle.vertexCount,
                iboHandle.indexHandle,
                iboHandle.indexCount);
    }

    // VAO Building \\

    private VAOHandle buildVAOHandle(String resourceName, JsonObject jsonObject, File file,
            InternalLoadManager loadManager) {
        if (!hasValidElement(jsonObject, "vao")) {
            return null;
        }

        // Check if already exists
        VAOHandle existing = vaoManager.getVAOHandleFromName(resourceName);
        if (existing != null) {
            return existing;
        }

        vaoManager.addVAO(resourceName, file, loadManager);
        return vaoManager.getVAOHandleFromName(resourceName);
    }

    // VBO Building \\

    private VBOHandle buildVBOHandle(String resourceName, JsonObject jsonObject, File file,
            InternalLoadManager loadManager) {
        if (!hasValidElement(jsonObject, "vbo")) {
            return null;
        }

        // Check if already exists
        VBOHandle existing = vboManager.getVBOHandleFromName(resourceName);
        if (existing != null) {
            return existing;
        }

        vboManager.addVBO(resourceName, file, loadManager);
        return vboManager.getVBOHandleFromName(resourceName);
    }

    // IBO Building \\

    private IBOHandle buildIBOHandle(String resourceName, JsonObject jsonObject, File file,
            InternalLoadManager loadManager) {
        if (!hasValidElement(jsonObject, "ibo")) {
            return null;
        }

        // Check if already exists
        IBOHandle existing = iboManager.getIBOHandleFromName(resourceName);
        if (existing != null) {
            return existing;
        }

        iboManager.addIBO(resourceName, file, loadManager);
        return iboManager.getIBOHandleFromName(resourceName);
    }

    // Utility \\

    private boolean hasValidElement(JsonObject jsonObject, String elementName) {
        return jsonObject.has(elementName) && !jsonObject.get(elementName).isJsonNull();
    }
}