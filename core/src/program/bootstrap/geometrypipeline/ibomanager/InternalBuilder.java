package program.bootstrap.geometrypipeline.ibomanager;

import java.io.File;
import java.util.Map;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import program.bootstrap.geometrypipeline.ibo.IBOHandle;
import program.bootstrap.geometrypipeline.vao.VAOInstance;
import program.core.engine.BuilderPackage;
import program.core.util.JsonUtility;

public class InternalBuilder extends BuilderPackage {

    /*
     * Parses the 'ibo' field from mesh JSON and uploads index data into an
     * IBOHandle. Supports direct index arrays and string references to other
     * registered meshes. Skips files that contain quad objects — those are
     * handled by quad expansion in the mesh builder. Bootstrap-only.
     */

    // Internal
    private IBOManager iboManager;

    // Base \\

    @Override
    protected void get() {

        // Internal
        this.iboManager = get(IBOManager.class);
    }

    // Build \\

    /*
     * VAOInstance is provided by InternalLoader — the same instance that will
     * be stored in the MeshHandle. IBO binding is recorded in the VAO state,
     * so this must be the exact same VAOInstance the mesh assembler uses.
     */
    public void build(
            String resourceName,
            File file,
            Map<String, File> registry,
            VAOInstance vaoInstance) {

        if (iboManager.hasIBO(resourceName))
            return;

        JsonObject json = JsonUtility.loadJsonObject(file);

        if (hasQuadEntries(json))
            return;

        if (!json.has("ibo") || json.get("ibo").isJsonNull())
            return;

        JsonElement iboEl = json.get("ibo");

        if (iboEl.isJsonPrimitive() && iboEl.getAsJsonPrimitive().isString()) {
            String refName = iboEl.getAsString();
            resolveRef(refName, resourceName, file, registry, vaoInstance);
            iboManager.registerIBO(resourceName, iboManager.getIBOHandleDirect(refName));
            return;
        }

        if (iboEl.isJsonArray()) {
            iboManager.registerIBO(resourceName, buildFromData(iboEl.getAsJsonArray(), vaoInstance, file));
            return;
        }

        throwException("IBO must be a string reference or index array in file: " + file.getName());
    }

    // Resolution \\

    private void resolveRef(
            String refName,
            String sourceResourceName,
            File sourceFile,
            Map<String, File> registry,
            VAOInstance vaoInstance) {

        if (iboManager.hasIBO(refName))
            return;

        File refFile = registry.get(refName);

        if (refFile == null)
            throwException("Referenced IBO '" + refName + "' not found. Source: " + sourceFile.getName());

        JsonObject refJson = JsonUtility.loadJsonObject(refFile);

        if (!refJson.has("ibo") || refJson.get("ibo").isJsonNull())
            throwException("Referenced IBO file '" + refName + "' has no 'ibo' field.");

        JsonElement refEl = refJson.get("ibo");

        if (!refEl.isJsonArray())
            throwException("Referenced IBO '" + refName + "' must contain an index array.");

        iboManager.registerIBO(refName, buildFromData(refEl.getAsJsonArray(), vaoInstance, refFile));
    }

    // Creation \\

    private IBOHandle buildFromData(
            JsonArray indicesArray,
            VAOInstance vaoInstance,
            File file) {

        if (indicesArray.size() == 0)
            throwException("Index data array cannot be empty in file: " + file.getName());

        short[] indices = new short[indicesArray.size()];
        int index = 0;

        for (JsonElement indexEl : indicesArray) {
            int value = indexEl.getAsInt();
            if (value < 0 || value > 0xFFFF)
                throwException("Index out of 16-bit range: " + value + " in file: " + file.getName());
            indices[index++] = (short) value;
        }

        return GLSLUtility.uploadIndexData(
                vaoInstance,
                create(IBOHandle.class),
                indices);
    }

    // Utility \\

    private boolean hasQuadEntries(JsonObject json) {

        if (!json.has("vbo") || json.get("vbo").isJsonNull())
            return false;

        JsonElement vboEl = json.get("vbo");

        if (!vboEl.isJsonArray())
            return false;

        for (JsonElement el : vboEl.getAsJsonArray())
            if (el.isJsonObject())
                return true;

        return false;
    }
}