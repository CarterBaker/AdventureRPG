package com.internal.bootstrap.geometrypipeline.meshmanager;

import java.io.File;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.internal.bootstrap.geometrypipeline.ibo.IBOHandle;
import com.internal.bootstrap.geometrypipeline.ibomanager.IBOManager;
import com.internal.bootstrap.geometrypipeline.mesh.MeshHandle;
import com.internal.bootstrap.geometrypipeline.vao.VAOHandle;
import com.internal.bootstrap.geometrypipeline.vao.VAOInstance;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOManager;
import com.internal.bootstrap.geometrypipeline.vbo.VBOHandle;
import com.internal.bootstrap.geometrypipeline.vbomanager.VBOManager;
import com.internal.core.engine.SystemPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.FileUtility;
import com.internal.core.util.JsonUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

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

    MeshHandle buildMeshHandle(File root, File file, int meshID, InternalLoadManager loadManager,
            UVProvider uvProvider) {

        JsonObject jsonObject = JsonUtility.loadJsonObject(file);
        String resourceName = FileUtility.getPathWithFileNameWithoutExtension(root, file);

        VAOHandle vaoTemplate = buildVAOTemplate(resourceName, jsonObject, file, loadManager);
        if (vaoTemplate == null)
            return null;

        VAOInstance vaoInstance = vaoManager.createVAOInstance(vaoTemplate);

        VBOHandle vboHandle;
        IBOHandle iboHandle;

        if (hasQuadEntries(jsonObject)) {
            QuadExpansionResult expansion = expandVBO(jsonObject, vaoInstance, file, uvProvider);
            vboHandle = buildVBOHandleFromData(resourceName, expansion.vertices, vaoInstance);
            iboHandle = buildIBOHandleFromData(resourceName, expansion.indices, vaoInstance);
        } else {
            vboHandle = buildVBOHandle(resourceName, jsonObject, file, loadManager, vaoInstance);
            iboHandle = buildIBOHandle(resourceName, jsonObject, file, loadManager, vaoInstance);
        }

        if (vboHandle == null || iboHandle == null)
            return null;

        MeshHandle meshHandle = create(MeshHandle.class);
        meshHandle.constructor(vaoInstance, vboHandle, iboHandle);
        return meshHandle;
    }

    // Quad Detection \\

    private boolean hasQuadEntries(JsonObject jsonObject) {
        if (!hasValidElement(jsonObject, "vbo"))
            return false;
        JsonElement vboElement = jsonObject.get("vbo");
        if (!vboElement.isJsonArray())
            return false;
        for (JsonElement element : vboElement.getAsJsonArray())
            if (element.isJsonObject())
                return true;
        return false;
    }

    // VAO Compatibility \\

    // Enforced only on textured quads. The last VAO attribute must be size 2
    // to hold the injected (u, v) pair. Crashes with a clear message if not.
    private void validateVAOUVCompatibility(VAOInstance vaoInstance, File file) {
        int[] attrSizes = vaoInstance.getVAOStruct().attrSizes;
        if (attrSizes == null || attrSizes.length == 0)
            throwException("VAO has no attribute layout — cannot inject UVs in file: " + file.getName());
        int lastAttr = attrSizes[attrSizes.length - 1];
        if (lastAttr != 2)
            throwException("Textured quad requires the last VAO attribute to be size 2, found size "
                    + lastAttr + " in file: " + file.getName()
                    + ". Remove 'texture' from the quad or fix the VAO layout.");
    }

    // Quad Expansion \\

    // Walks the vbo array producing flat vertex and index data.
    // Raw vertex arrays and quad objects may appear in any order.
    //
    // Quad object format:
    //
    // No texture: { "quad": [[...],[...],[...],[...]] }
    // Each corner supplies all vertStride floats verbatim.
    //
    // With texture: { "quad": [[...],[...],[...],[...]], "texture": "atlas/tile" }
    // Each corner supplies (vertStride - 2) position floats.
    // Atlas UVs are injected at the end of each vertex.
    // VAO last attribute must be size 2 — crashes if not.
    //
    // With texture { "quad": [[...],[...],[...],[...]], "texture": "atlas/tile",
    // + uvs: "uvs": [[lu,lv],[lu,lv],[lu,lv],[lu,lv]] }
    // Same as above but each corner uses its own local UV (0-1)
    // instead of the default BL/BR/TR/TL corner mapping.
    // Local UV values are pixel-snapped against BLOCK_TEXTURE_SIZE
    // then remapped into atlas UV space.
    // 0.0 always resolves to tile min, 1.0 to tile max.
    //
    // Explicit ibo entries (covering raw verts) are prepended to the
    // auto-generated quad indices so both can coexist in one buffer.
    private QuadExpansionResult expandVBO(JsonObject jsonObject, VAOInstance vaoInstance, File file,
            UVProvider uvProvider) {

        int vertStride = vaoInstance.getVAOStruct().vertStride;

        FloatArrayList vertices = new FloatArrayList();
        ShortArrayList quadIndices = new ShortArrayList();
        int currentVertex = 0;

        for (JsonElement element : jsonObject.getAsJsonArray("vbo")) {

            if (element.isJsonArray()) {
                JsonArray vertex = element.getAsJsonArray();
                if (vertex.size() != vertStride)
                    throwException("Vertex attribute count mismatch. Expected "
                            + vertStride + " floats but got " + vertex.size()
                            + " in file: " + file.getName());
                for (JsonElement val : vertex)
                    vertices.add(val.getAsFloat());
                currentVertex++;

            } else if (element.isJsonObject()) {
                expandQuad(element.getAsJsonObject(), vertices, quadIndices, currentVertex, vertStride, vaoInstance,
                        file, uvProvider);
                currentVertex += 4;

            } else {
                throwException("VBO element must be a vertex array or quad object in file: " + file.getName());
            }
        }

        ShortArrayList allIndices = new ShortArrayList();

        if (hasValidElement(jsonObject, "ibo")) {
            for (JsonElement idx : jsonObject.getAsJsonArray("ibo")) {
                int value = idx.getAsInt();
                if (value < 0 || value > 65535)
                    throwException("Index value out of range for 16-bit: " + value + " in file: " + file.getName());
                allIndices.add((short) value);
            }
        }

        allIndices.addAll(quadIndices);

        if (allIndices.isEmpty())
            throwException("No index data produced for file: " + file.getName()
                    + ". Provide an explicit 'ibo' for raw verts and/or include quad entries.");

        return new QuadExpansionResult(vertices.toFloatArray(), allIndices.toShortArray());
    }

    // Corner UV mapping when no "uvs" field is present (BL -> BR -> TR -> TL):
    // corner 0 (BL) -> (u0, v0)
    // corner 1 (BR) -> (u1, v0)
    // corner 2 (TR) -> (u1, v1)
    // corner 3 (TL) -> (u0, v1)
    private static final float[][] DEFAULT_CORNER_LOCAL_UVS = {
            { 0, 0 },
            { 1, 0 },
            { 1, 1 },
            { 0, 1 }
    };

    private void expandQuad(
            JsonObject quadObj,
            FloatArrayList vertices,
            ShortArrayList quadIndices,
            int baseVertex,
            int vertStride,
            VAOInstance vaoInstance,
            File file,
            UVProvider uvProvider) {

        if (!quadObj.has("quad") || quadObj.get("quad").isJsonNull())
            throwException("Quad object missing 'quad' positions array in file: " + file.getName());

        JsonArray positions = quadObj.getAsJsonArray("quad");
        if (positions.size() != 4)
            throwException("Quad 'quad' array must have exactly 4 corner entries in file: " + file.getName());

        boolean hasTexture = quadObj.has("texture") && !quadObj.get("texture").isJsonNull();

        if (hasTexture) {

            validateVAOUVCompatibility(vaoInstance, file);

            int posStride = vertStride - 2;
            float[] uvs = uvProvider.getUVs(quadObj.get("texture").getAsString());
            float u0 = uvs[0], v0 = uvs[1], u1 = uvs[2], v1 = uvs[3];

            float[][] localUVs = resolveLocalUVs(quadObj, file);

            for (int i = 0; i < 4; i++) {
                JsonArray pos = positions.get(i).getAsJsonArray();
                if (pos.size() != posStride)
                    throwException("Textured quad corner " + i + " has " + pos.size()
                            + " position floats but expected " + posStride
                            + " (vertStride " + vertStride + " - 2) in file: " + file.getName());
                for (JsonElement val : pos)
                    vertices.add(val.getAsFloat());
                vertices.add(snapUV(localUVs[i][0], u0, u1));
                vertices.add(snapUV(localUVs[i][1], v0, v1));
            }

        } else {

            for (int i = 0; i < 4; i++) {
                JsonArray corner = positions.get(i).getAsJsonArray();
                if (corner.size() != vertStride)
                    throwException("Untextured quad corner " + i + " has " + corner.size()
                            + " floats but VAO vertStride is " + vertStride
                            + " in file: " + file.getName());
                for (JsonElement val : corner)
                    vertices.add(val.getAsFloat());
            }
        }

        quadIndices.add((short) baseVertex);
        quadIndices.add((short) (baseVertex + 1));
        quadIndices.add((short) (baseVertex + 2));
        quadIndices.add((short) (baseVertex + 2));
        quadIndices.add((short) (baseVertex + 3));
        quadIndices.add((short) baseVertex);
    }

    // UV Snapping \\

    // Returns the per-corner local UV pairs to use for a textured quad.
    // If a "uvs" field is present, reads 4 [localU, localV] pairs from it.
    // Otherwise falls back to the default BL/BR/TR/TL corner layout.
    private float[][] resolveLocalUVs(JsonObject quadObj, File file) {
        if (!quadObj.has("uvs") || quadObj.get("uvs").isJsonNull())
            return DEFAULT_CORNER_LOCAL_UVS;

        JsonArray uvsArray = quadObj.getAsJsonArray("uvs");
        if (uvsArray.size() != 4)
            throwException("Quad 'uvs' array must have exactly 4 entries in file: " + file.getName());

        float[][] localUVs = new float[4][2];
        for (int i = 0; i < 4; i++) {
            JsonArray pair = uvsArray.get(i).getAsJsonArray();
            if (pair.size() != 2)
                throwException("Quad 'uvs' entry " + i + " must have exactly 2 values in file: " + file.getName());
            localUVs[i][0] = pair.get(0).getAsFloat();
            localUVs[i][1] = pair.get(1).getAsFloat();
        }
        return localUVs;
    }

    // Maps a local UV (0-1) into atlas UV space with pixel-level snapping.
    // 0.0 always resolves to tileMin, 1.0 always resolves to tileMax.
    // Values in between snap to the nearest pixel boundary within the tile.
    private float snapUV(float local, float tileMin, float tileMax) {
        int pixel = Math.round(local * EngineSetting.BLOCK_TEXTURE_SIZE);
        float snapped = pixel / (float) EngineSetting.BLOCK_TEXTURE_SIZE;
        return tileMin + snapped * (tileMax - tileMin);
    }

    // Data VBO / IBO \\

    private VBOHandle buildVBOHandleFromData(String resourceName, float[] vertices, VAOInstance vaoInstance) {
        VBOHandle existing = vboManager.getVBOHandleFromName(resourceName);
        if (existing != null)
            return existing;
        return vboManager.addVBOFromData(resourceName, vertices, vaoInstance);
    }

    private IBOHandle buildIBOHandleFromData(String resourceName, short[] indices, VAOInstance vaoInstance) {
        IBOHandle existing = iboManager.getIBOHandleFromName(resourceName);
        if (existing != null)
            return existing;
        return iboManager.addIBOFromData(resourceName, indices, vaoInstance);
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

    private static final class QuadExpansionResult {
        final float[] vertices;
        final short[] indices;

        QuadExpansionResult(float[] vertices, short[] indices) {
            this.vertices = vertices;
            this.indices = indices;
        }
    }
}