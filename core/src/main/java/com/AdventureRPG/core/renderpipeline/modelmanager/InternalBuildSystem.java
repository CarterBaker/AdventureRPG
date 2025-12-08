package com.AdventureRPG.core.renderpipeline.modelmanager;

import java.io.File;

import com.AdventureRPG.core.kernel.SystemFrame;
import com.AdventureRPG.core.renderpipeline.vaomanager.VAOManager;
import com.AdventureRPG.core.util.JsonUtility;
import com.AdventureRPG.core.util.Exceptions.FileException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

class InternalBuildSystem extends SystemFrame {

    // Internal
    private VAOManager vaoManager;

    // Base \\

    @Override
    protected void init() {
        this.vaoManager = gameEngine.get(VAOManager.class);
    }

    // Build \\

    MeshDataInstance buildMeshData(File file, int meshID) {

        JsonObject obj = JsonUtility.loadJsonObject(file);

        // Extract mesh name from file
        String meshName = file.getName();
        if (meshName.endsWith(".json"))
            meshName = meshName.substring(0, meshName.length() - 5);

        // Get VAO information
        String vaoName = JsonUtility.validateString(obj, "vao");
        int vaoID = vaoManager.getVAOIDFromName(vaoName);
        int vaoHandle = vaoManager.getVAOHandleFromID(vaoID);

        // Parse vertices to determine stride
        JsonArray verticesArray = JsonUtility.validateArray(obj, "vertices");

        // Calculate vertex stride from first vertex
        JsonArray firstVertex = verticesArray.get(0).getAsJsonArray();
        int vertStride = firstVertex.size();

        // Parse all data
        FloatArrayList vertices = parseVertices(verticesArray, vertStride, file.getName());
        ShortArrayList indices = parseIndices(obj, verticesArray.size(), file.getName());
        int vertexCount = verticesArray.size();

        // Construct and return mesh in one shot
        return new MeshDataInstance(
                meshName,
                meshID,
                vaoHandle,
                vertStride,
                vertices,
                indices,
                vertexCount);
    }

    // Vertices \\

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

    // Indices \\

    private ShortArrayList parseIndices(
            JsonObject obj,
            int vertexCount,
            String fileName) {

        ShortArrayList indices = new ShortArrayList();

        JsonArray indicesArray = JsonUtility.validateArray(obj, "indices");

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