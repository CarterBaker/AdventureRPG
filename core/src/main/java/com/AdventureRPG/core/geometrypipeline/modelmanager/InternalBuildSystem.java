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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

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

    MeshDataInstance buildMeshData(File file, int meshID) {

        JsonObject obj = JsonUtility.loadJsonObject(file);

        // Extract resource name from file
        String resourceName = FileUtility.getFileName(file);

        // Process all three sections - any can be null
        VAOHandle vaoHandle = processVAO(obj, file);
        VBOHandle vboData = processVBO(obj, file, vaoHandle);
        IBOHandle iboData = processIBO(obj, file, vboData != null ? vboData.vertexCount : 0);

        // If any component is null, don't create a mesh - just register the resources
        if (vaoHandle == null || vboData == null || iboData == null)
            return null; // Resources created but no complete mesh

        // All components present - construct and return complete mesh data instance
        return new MeshDataInstance(
                resourceName,
                meshID,
                vaoHandle,
                vboData.vertices,
                iboData.indices,
                vboData.vertexCount);
    }

    // VAO Processing \\

    private VAOHandle processVAO(JsonObject obj, File file) {

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
            vaoManager.addVAO(file);
            String vaoName = FileUtility.getFileName(file);
            return vaoManager.getVAOHandleFromName(vaoName);
        }

        throw new FileException.FileReadException( // TODO: Not the best error
                "Mesh data error: " + file.getName() + " - VAO must be a string reference or object definition");
    }

    // VBO Processing \\

    private VBOHandle processVBO(JsonObject obj, File file, VAOHandle vaoHandle) {

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

            JsonArray verticesArray = vboElement.getAsJsonArray();

            // Need VAO to know stride for parsing
            if (vaoHandle == null) // TODO: Not the best error
                throw new FileException.FileReadException(
                        "Mesh data error: " + file.getName() + " - Cannot parse inline VBO without VAO definition");

            FloatArrayList vertices = parseVertices(verticesArray, vaoHandle.vertStride, file.getName());
            int vertCount = verticesArray.size() / vaoHandle.vertStride;

            // Store this VBO for potential reuse
            vboManager.addVBO(vertices, vertCount);

            return new VBOHandle(vertices, vertCount);
        }

        throw new FileException.FileReadException(
                "Mesh data error: " + file.getName() + " - VBO must be a string reference or array of vertices");
    }

    // IBO Processing \\

    private IBOHandle processIBO(JsonObject obj, File file, int vertexCount) {

        if (!obj.has("ibo") || obj.get("ibo").isJsonNull())
            return null; // No IBO to process

        JsonElement iboElement = obj.get("ibo");

        // Case 1: String reference to existing IBO
        if (iboElement.isJsonPrimitive() && iboElement.getAsJsonPrimitive().isString()) {
            String iboName = iboElement.getAsString();
            int iboID = iboManager.getIBOIDFromIBOName(iboName);
            int indexCount = iboManager.getIndexCountFromIBOID(iboID);
            // TODO: Retrieve actual indices from IBO manager if needed
            return new IBOHandle(new ShortArrayList());
        }

        // Case 2: Inline index data array
        if (iboElement.isJsonArray()) {
            JsonArray indicesArray = iboElement.getAsJsonArray();
            ShortArrayList indices = parseIndices(indicesArray, vertexCount, file.getName());

            // Store this IBO for potential reuse
            // TODO: Call iboManager.addIBO with the parsed data

            return new IBOHandle(indices);
        }

        throw new FileException.FileReadException(
                "Mesh data error: " + file.getName() + " - IBO must be a string reference or array of indices");
    }

    // Parsing \\

    private FloatArrayList parseVertices(
            JsonArray verticesArray,
            int expectedStride,
            String fileName) {

        FloatArrayList vertices = new FloatArrayList();

        for (int i = 0; i < verticesArray.size(); i++) {

            JsonElement vertexElement = verticesArray.get(i);

            if (!vertexElement.isJsonArray())
                throw new FileException.FileReadException(
                        "Mesh data error: " + fileName + ", vertex at index " + i + " is not an array");

            JsonArray vertexArray = vertexElement.getAsJsonArray();

            if (vertexArray.size() != expectedStride)
                throw new FileException.FileReadException(
                        "Mesh data error: " + fileName + ", vertex at index " + i
                                + " has incorrect stride. Expected: " + expectedStride
                                + ", Found: " + vertexArray.size());

            // Add all floats from this vertex
            for (JsonElement floatElement : vertexArray)
                vertices.add(floatElement.getAsFloat());
        }

        return vertices;
    }

    private ShortArrayList parseIndices(
            JsonArray indicesArray,
            int vertexCount,
            String fileName) {

        ShortArrayList indices = new ShortArrayList();

        for (JsonElement indexElement : indicesArray) {

            int index = indexElement.getAsInt();

            // Validate index is within bounds
            if (index < 0 || index >= vertexCount)
                throw new FileException.FileReadException(
                        "Mesh data error: " + fileName + ", index " + index
                                + " is out of bounds. Vertex count: " + vertexCount);

            indices.add((short) index);
        }

        // Validate triangle topology
        if (indices.size() % 3 != 0)
            throw new FileException.FileReadException(
                    "Mesh data error: " + fileName + ", indices count (" + indices.size()
                            + ") is not divisible by 3 (not valid triangles)");

        return indices;
    }
}