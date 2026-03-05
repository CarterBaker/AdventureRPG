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
import com.internal.core.engine.BuilderPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.FileUtility;
import com.internal.core.util.JsonUtility;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

class InternalBuilder extends BuilderPackage {

    /*
     * Pure assembler. Buffer builders have already parsed, uploaded and
     * registered their data before this is called. This builder retrieves
     * handles from each manager using direct lookups only — never the
     * load-triggering accessors — to avoid recursing back into the mesh loader.
     */

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

    MeshHandle buildMeshHandle(File root, File file, int meshID, UVProvider uvProvider) {

        JsonObject json = JsonUtility.loadJsonObject(file);
        String resourceName = FileUtility.getPathWithFileNameWithoutExtension(root, file);

        // Direct lookup only — getVAOHandleFromName() triggers meshLoader.request()
        // which would recurse infinitely back into this builder.
        VAOHandle vaoTemplate = vaoManager.getVAOHandleDirect(resourceName);
        if (vaoTemplate == null)
            return null;

        VAOInstance vaoInstance = vaoManager.createVAOInstance(vaoTemplate);

        VBOHandle vboHandle;
        IBOHandle iboHandle;

        if (hasQuadEntries(json)) {
            // Quad data is generated inline — buffer builders correctly skipped this
            QuadExpansionResult expansion = expandVBO(json, vaoInstance, file, uvProvider);
            vboHandle = vboManager.addVBOFromData(resourceName, expansion.vertices, vaoInstance);
            iboHandle = iboManager.addIBOFromData(resourceName, expansion.indices, vaoInstance);
        } else {
            // Direct lookups only — buffer builders already registered these.
            // Using getVBOHandleFromName/getIBOHandleFromName here would trigger
            // meshLoader.request() and recurse infinitely.
            vboHandle = vboManager.getVBOHandleDirect(resourceName);
            iboHandle = iboManager.getIBOHandleDirect(resourceName);
        }

        if (vboHandle == null || iboHandle == null)
            return null;

        MeshHandle meshHandle = create(MeshHandle.class);
        meshHandle.constructor(vaoInstance, vboHandle, iboHandle);
        return meshHandle;
    }

    // Quad Detection \\

    private boolean hasQuadEntries(JsonObject json) {
        if (!hasValidElement(json, "vbo"))
            return false;
        JsonElement vboEl = json.get("vbo");
        if (!vboEl.isJsonArray())
            return false;
        for (JsonElement el : vboEl.getAsJsonArray())
            if (el.isJsonObject())
                return true;
        return false;
    }

    // Quad Expansion \\

    private QuadExpansionResult expandVBO(JsonObject json, VAOInstance vaoInstance, File file, UVProvider uvProvider) {

        int vertStride = vaoInstance.getVAOStruct().vertStride;
        FloatArrayList vertices = new FloatArrayList();
        ShortArrayList quadIndices = new ShortArrayList();
        int currentVertex = 0;

        for (JsonElement element : json.getAsJsonArray("vbo")) {

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

        if (hasValidElement(json, "ibo")) {
            for (JsonElement idx : json.getAsJsonArray("ibo")) {
                int value = idx.getAsInt();
                if (value < 0 || value > 65535)
                    throwException("Index out of 16-bit range: " + value + " in file: " + file.getName());
                allIndices.add((short) value);
            }
        }

        allIndices.addAll(quadIndices);

        if (allIndices.isEmpty())
            throwException("No index data produced for file: " + file.getName()
                    + ". Provide an explicit 'ibo' for raw verts and/or include quad entries.");

        return new QuadExpansionResult(vertices.toFloatArray(), allIndices.toShortArray());
    }

    private static final float[][] DEFAULT_CORNER_LOCAL_UVS = {
            { 0, 0 }, { 1, 0 }, { 1, 1 }, { 0, 1 }
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
            throwException("Quad object missing 'quad' array in file: " + file.getName());

        JsonArray positions = quadObj.getAsJsonArray("quad");
        if (positions.size() != 4)
            throwException("Quad 'quad' array must have exactly 4 corners in file: " + file.getName());

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
                            + " floats, expected " + posStride + " in file: " + file.getName());
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
                            + " floats, expected " + vertStride + " in file: " + file.getName());
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

    // VAO Compatibility \\

    private void validateVAOUVCompatibility(VAOInstance vaoInstance, File file) {
        int[] attrSizes = vaoInstance.getVAOStruct().attrSizes;
        if (attrSizes == null || attrSizes.length == 0)
            throwException("VAO has no attribute layout — cannot inject UVs in file: " + file.getName());
        int lastAttr = attrSizes[attrSizes.length - 1];
        if (lastAttr != 2)
            throwException("Textured quad requires last VAO attribute size 2, found "
                    + lastAttr + " in file: " + file.getName()
                    + ". Remove 'texture' from the quad or fix the VAO layout.");
    }

    // UV Snapping \\

    private float[][] resolveLocalUVs(JsonObject quadObj, File file) {
        if (!quadObj.has("uvs") || quadObj.get("uvs").isJsonNull())
            return DEFAULT_CORNER_LOCAL_UVS;

        JsonArray uvsArray = quadObj.getAsJsonArray("uvs");
        if (uvsArray.size() != 4)
            throwException("Quad 'uvs' must have exactly 4 entries in file: " + file.getName());

        float[][] localUVs = new float[4][2];
        for (int i = 0; i < 4; i++) {
            JsonArray pair = uvsArray.get(i).getAsJsonArray();
            if (pair.size() != 2)
                throwException("Quad 'uvs' entry " + i + " must have 2 values in file: " + file.getName());
            localUVs[i][0] = pair.get(0).getAsFloat();
            localUVs[i][1] = pair.get(1).getAsFloat();
        }
        return localUVs;
    }

    private float snapUV(float local, float tileMin, float tileMax) {
        int pixel = Math.round(local * EngineSetting.BLOCK_TEXTURE_SIZE);
        float snapped = pixel / (float) EngineSetting.BLOCK_TEXTURE_SIZE;
        return tileMin + snapped * (tileMax - tileMin);
    }

    // Utility \\

    private boolean hasValidElement(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull();
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