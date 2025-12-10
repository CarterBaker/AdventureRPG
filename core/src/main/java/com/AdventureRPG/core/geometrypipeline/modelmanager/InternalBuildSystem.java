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
import com.AdventureRPG.core.util.Exceptions.FileException;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

class InternalBuildSystem extends SystemFrame {

    // Internal
    private VBOManager vboManager;
    private IBOManager iboManager;
    private VAOManager vaoManager;

    // Base \\

    @Override
    protected void init() {

        // Internal
        this.vboManager = gameEngine.get(VBOManager.class);
        this.iboManager = gameEngine.get(IBOManager.class);
        this.vaoManager = gameEngine.get(VAOManager.class);
    }

    // Build \\

    MeshHandle buildMeshHandle(File file, int meshID) {

        JsonObject obj = JsonUtility.loadJsonObject(file);

        // Extract resource name from file
        String resourceName = FileUtility.getFileName(file);

        // Process all three sections - any can be null
        VAOHandle vaoHandle = processVAO(
                resourceName,
                obj,
                file);

        VBOHandle vboHandle = processVBO(
                resourceName,
                obj,
                file);

        IBOHandle iboHandle = processIBO(
                resourceName,
                obj,
                file);

        // If any component is null, don't create a mesh - just register the resources
        if (vaoHandle == null || vboHandle == null || iboHandle == null)
            return null; // Resources created but no complete mesh

        // All components present - construct and return complete mesh data instance
        return new MeshHandle(
                vaoHandle.attributeHandle,
                vaoHandle.vertStride,
                vboHandle.vertexHandle,
                vboHandle.vertexCount,
                iboHandle.indexHandle,
                iboHandle.indexCount);
    }

    // VAO Processing \\

    private VAOHandle processVAO(
            String resourceName,
            JsonObject obj,
            File file) {

        if (!obj.has("vao") || obj.get("vao").isJsonNull())
            return null; // No VAO to process

        JsonElement vaoElement = obj.get("vao");

        // Case 1: String reference to existing VAO
        if (vaoElement.isJsonPrimitive() && vaoElement.getAsJsonPrimitive().isString()) {
            String vaoName = vaoElement.getAsString();
            return vaoManager.getVAOHandleFromName(vaoName);
        }

        // Case 2: Inline VAO definition
        if (vaoElement.isJsonObject()) {

            // Create VAO from inline definition
            vaoManager.addVAO(resourceName, file);
            return vaoManager.getVAOHandleFromName(resourceName);
        }

        throw new FileException.FileReadException( // TODO: Not the best error
                "Mesh data error: " + file.getName() + ", VAO must be a string reference or object definition");
    }

    // VBO Processing \\

    private VBOHandle processVBO(
            String resourceName,
            JsonObject obj,
            File file) {

        if (!obj.has("vbo") || obj.get("vbo").isJsonNull())
            return null; // No VBO to process

        JsonElement vboElement = obj.get("vbo");

        // Case 1: String reference to existing VBO
        if (vboElement.isJsonPrimitive() && vboElement.getAsJsonPrimitive().isString()) {
            String vboName = vboElement.getAsString();
            return vboManager.getVBOHandleFromName(vboName);
        }

        // Case 2: Inline vertex data array
        if (vboElement.isJsonArray()) {

            // Store this VBO for potential reuse
            vboManager.addVBO(resourceName, file);
            return vboManager.getVBOHandleFromName(resourceName);
        }

        throw new FileException.FileReadException( // TODO: Not the best error
                "Mesh data error: " + file.getName() + ", VBO must be a string reference or array of vertices");
    }

    // IBO Processing \\

    private IBOHandle processIBO(
            String resourceName,
            JsonObject obj,
            File file) {

        if (!obj.has("ibo") || obj.get("ibo").isJsonNull())
            return null; // No IBO to process

        JsonElement iboElement = obj.get("ibo");

        // Case 1: String reference to existing IBO
        if (iboElement.isJsonPrimitive() && iboElement.getAsJsonPrimitive().isString()) {
            String iboName = iboElement.getAsString();
            return iboManager.getIBOHandleFromName(iboName);
        }

        // Case 2: Inline index data array
        if (iboElement.isJsonArray()) {

            // Store this IBO for potential reuse
            iboManager.addIBO(resourceName, file);
            return iboManager.getIBOHandleFromName(resourceName);
        }

        throw new FileException.FileReadException( // TODO: Add my own error
                "Mesh data error: " + file.getName() + " - IBO must be a string reference or array of indices");
    }
}