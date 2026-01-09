package com.AdventureRPG.bootstrap.geometrypipeline.modelmanager;

import java.io.File;

import com.AdventureRPG.bootstrap.geometrypipeline.ibomanager.IBOHandle;
import com.AdventureRPG.bootstrap.geometrypipeline.ibomanager.IBOManager;
import com.AdventureRPG.bootstrap.geometrypipeline.vaomanager.VAOHandle;
import com.AdventureRPG.bootstrap.geometrypipeline.vaomanager.VAOManager;
import com.AdventureRPG.bootstrap.geometrypipeline.vbomanager.VBOHandle;
import com.AdventureRPG.bootstrap.geometrypipeline.vbomanager.VBOManager;
import com.AdventureRPG.core.engine.SystemPackage;
import com.AdventureRPG.core.util.FileUtility;
import com.AdventureRPG.core.util.JsonUtility;
import com.google.gson.JsonObject;

class InternalBuildSystem extends SystemPackage {

    // Internal
    private VBOManager vboManager;
    private IBOManager iboManager;
    private VAOManager vaoManager;

    // Base \\

    @Override
    protected void get() {
        this.vboManager = get(VBOManager.class);
        this.iboManager = get(IBOManager.class);
        this.vaoManager = get(VAOManager.class);
    }

    // Build \\

    MeshHandle buildMeshHandle(
            File root,
            File file,
            int meshID,
            InternalLoadManager loadManager) {

        JsonObject jsonObject = JsonUtility.loadJsonObject(file);
        String resourceName = FileUtility.getPathWithFileNameWithoutExtension(root, file);

        VAOHandle vaoHandle = buildVAOHandle(resourceName, jsonObject, file, loadManager);
        VBOHandle vboHandle = buildVBOHandle(resourceName, jsonObject, file, loadManager);
        IBOHandle iboHandle = buildIBOHandle(resourceName, jsonObject, file, loadManager);

        return createMeshHandle(
                vaoHandle,
                vboHandle,
                iboHandle);
    }

    private MeshHandle createMeshHandle(
            VAOHandle vaoHandle,
            VBOHandle vboHandle,
            IBOHandle iboHandle) {

        if (vaoHandle == null || vboHandle == null || iboHandle == null) {
            return null;
        }

        MeshHandle meshHandle = create(MeshHandle.class);
        meshHandle.constructor(
                vaoHandle.getAttributeHandle(),
                vaoHandle.getVertStride(),
                vboHandle.getVertexHandle(),
                vboHandle.getVertexCount(),
                iboHandle.getIndexHandle(),
                iboHandle.getIndexCount());

        return meshHandle;
    }

    // VAO Building \\

    private VAOHandle buildVAOHandle(
            String resourceName,
            JsonObject jsonObject,
            File file,
            InternalLoadManager loadManager) {

        if (!hasValidElement(jsonObject, "vao"))
            return null;

        // Check if the vao already exists
        VAOHandle existing = vaoManager.getVAOHandleFromName(resourceName);
        if (existing != null)
            return existing;

        var vaoElement = jsonObject.get("vao");

        // Case 1: string reference
        if (vaoElement.isJsonPrimitive() && vaoElement.getAsJsonPrimitive().isString()) {

            String refName = vaoElement.getAsString();
            File refFile = loadManager.getFileByResourceName(refName);

            if (refFile == null)
                throwException("Referenced VAO file not found: " + refName);

            // Recursively build dependency
            loadManager.processMeshFile(refFile);

            // Return handle now created in VAOManager
            return vaoManager.getVAOHandleFromName(refName);
        }

        // Case 2: int (vert stride) or raw object — let VAOManager handle it
        vaoManager.addVAO(
                resourceName,
                file,
                loadManager);
        return vaoManager.getVAOHandleFromName(resourceName);
    }

    // VBO Building \\

    private VBOHandle buildVBOHandle(
            String resourceName,
            JsonObject jsonObject,
            File file,
            InternalLoadManager loadManager) {

        if (!hasValidElement(jsonObject, "vbo"))
            return null;

        // Check if the vbo already exists
        VBOHandle existing = vboManager.getVBOHandleFromName(resourceName);
        if (existing != null)
            return existing;

        var vboElement = jsonObject.get("vbo");

        // String reference
        if (vboElement.isJsonPrimitive() && vboElement.getAsJsonPrimitive().isString()) {

            String refName = vboElement.getAsString();
            File refFile = loadManager.getFileByResourceName(refName);

            if (refFile == null)
                throwException("Referenced VBO file not found: " + refName);

            loadManager.processMeshFile(refFile);
            return vboManager.getVBOHandleFromName(refName);
        }

        // Raw data
        vboManager.addVBO(
                resourceName,
                file,
                loadManager);
        return vboManager.getVBOHandleFromName(resourceName);
    }

    // IBO Building \\

    private IBOHandle buildIBOHandle(
            String resourceName,
            JsonObject jsonObject,
            File file,
            InternalLoadManager loadManager) {

        if (!hasValidElement(jsonObject, "ibo"))
            return null;

        // Check if the ibSo already exists
        IBOHandle existing = iboManager.getIBOHandleFromName(resourceName);
        if (existing != null)
            return existing;

        var iboElement = jsonObject.get("ibo");

        // String reference
        if (iboElement.isJsonPrimitive() && iboElement.getAsJsonPrimitive().isString()) {

            String refName = iboElement.getAsString();
            File refFile = loadManager.getFileByResourceName(refName);

            if (refFile == null)
                throwException("Referenced IBO file not found: " + refName);

            loadManager.processMeshFile(refFile);
            return iboManager.getIBOHandleFromName(refName);
        }

        // Raw data
        iboManager.addIBO(
                resourceName,
                file,
                loadManager);
        return iboManager.getIBOHandleFromName(resourceName);
    }

    // Utility \\

    private boolean hasValidElement(JsonObject jsonObject, String elementName) {
        return jsonObject.has(elementName) && !jsonObject.get(elementName).isJsonNull();
    }
}