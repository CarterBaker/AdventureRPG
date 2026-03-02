package com.internal.bootstrap.geometrypipeline.meshmanager;

import java.io.File;

import com.google.gson.JsonObject;
import com.internal.bootstrap.geometrypipeline.ibomanager.IBOHandle;
import com.internal.bootstrap.geometrypipeline.ibomanager.IBOManager;
import com.internal.bootstrap.geometrypipeline.vao.VAOInstance;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOHandle;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOManager;
import com.internal.bootstrap.geometrypipeline.vbomanager.VBOHandle;
import com.internal.bootstrap.geometrypipeline.vbomanager.VBOManager;
import com.internal.core.engine.SystemPackage;
import com.internal.core.util.FileUtility;
import com.internal.core.util.JsonUtility;

class InternalBuildSystem extends SystemPackage {

    // Internal
    private VAOManager vaoManager;
    private VBOManager vboManager;
    private IBOManager iboManager;

    // Base \\

    @Override
    protected void get() {
        this.vaoManager = get(VAOManager.class);
        this.vboManager = get(VBOManager.class);
        this.iboManager = get(IBOManager.class);
    }

    // Build \\

    MeshHandle buildMeshHandle(File root, File file, int meshID, InternalLoadManager loadManager) {

        JsonObject jsonObject = JsonUtility.loadJsonObject(file);
        String resourceName = FileUtility.getPathWithFileNameWithoutExtension(root, file);

        VAOHandle vaoTemplate = buildVAOTemplate(resourceName, jsonObject, file, loadManager);
        if (vaoTemplate == null)
            return null;

        VAOInstance vaoInstance = vaoManager.createVAOInstance(vaoTemplate);

        VBOHandle vboHandle = buildVBOHandle(resourceName, jsonObject, file, loadManager, vaoInstance);
        IBOHandle iboHandle = buildIBOHandle(resourceName, jsonObject, file, loadManager, vaoInstance);

        if (vboHandle == null || iboHandle == null)
            return null;

        MeshHandle meshHandle = create(MeshHandle.class);
        meshHandle.constructor(vaoInstance, vboHandle, iboHandle);
        return meshHandle;
    }

    // VAO \\

    private VAOHandle buildVAOTemplate(
            String resourceName,
            JsonObject jsonObject,
            File file,
            InternalLoadManager loadManager) {

        if (!hasValidElement(jsonObject, "vao"))
            return null;

        VAOHandle existing = vaoManager.getVAOHandleFromName(resourceName);
        if (existing != null)
            return existing;

        var vaoElement = jsonObject.get("vao");

        if (vaoElement.isJsonPrimitive() && vaoElement.getAsJsonPrimitive().isString()) {

            String refName = vaoElement.getAsString();
            File refFile = loadManager.getFileByResourceName(refName);

            if (refFile == null)
                throwException("Referenced VAO file not found: " + refName);

            loadManager.processMeshFile(refFile);
            return vaoManager.getVAOHandleFromName(refName);
        }

        vaoManager.addVAO(resourceName, file, loadManager);
        return vaoManager.getVAOHandleFromName(resourceName);
    }

    // VBO \\

    private VBOHandle buildVBOHandle(
            String resourceName,
            JsonObject jsonObject,
            File file,
            InternalLoadManager loadManager,
            VAOInstance vaoInstance) {

        if (!hasValidElement(jsonObject, "vbo"))
            return null;

        VBOHandle existing = vboManager.getVBOHandleFromName(resourceName);
        if (existing != null)
            return existing;

        var vboElement = jsonObject.get("vbo");

        if (vboElement.isJsonPrimitive() && vboElement.getAsJsonPrimitive().isString()) {

            String refName = vboElement.getAsString();
            File refFile = loadManager.getFileByResourceName(refName);

            if (refFile == null)
                throwException("Referenced VBO file not found: " + refName);

            loadManager.processMeshFile(refFile);
            return vboManager.getVBOHandleFromName(refName);
        }

        vboManager.addVBO(resourceName, file, loadManager, vaoInstance);
        return vboManager.getVBOHandleFromName(resourceName);
    }

    // IBO \\

    private IBOHandle buildIBOHandle(
            String resourceName,
            JsonObject jsonObject,
            File file,
            InternalLoadManager loadManager,
            VAOInstance vaoInstance) {

        if (!hasValidElement(jsonObject, "ibo"))
            return null;

        IBOHandle existing = iboManager.getIBOHandleFromName(resourceName);
        if (existing != null)
            return existing;

        var iboElement = jsonObject.get("ibo");

        if (iboElement.isJsonPrimitive() && iboElement.getAsJsonPrimitive().isString()) {

            String refName = iboElement.getAsString();
            File refFile = loadManager.getFileByResourceName(refName);

            if (refFile == null)
                throwException("Referenced IBO file not found: " + refName);

            loadManager.processMeshFile(refFile);
            return iboManager.getIBOHandleFromName(refName);
        }

        iboManager.addIBO(resourceName, file, loadManager, vaoInstance);
        return iboManager.getIBOHandleFromName(resourceName);
    }

    // Utility \\

    private boolean hasValidElement(JsonObject jsonObject, String key) {
        return jsonObject.has(key) && !jsonObject.get(key).isJsonNull();
    }
}