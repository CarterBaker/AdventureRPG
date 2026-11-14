package application.bootstrap.geometrypipeline.ibomanager;

import java.io.File;

import application.bootstrap.geometrypipeline.ibo.IBOHandle;
import application.bootstrap.geometrypipeline.vao.VAOInstance;
import engine.root.BuilderPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class IBOBuilder extends BuilderPackage {

    /*
     * Parses the 'ibo' field from mesh ARPG and uploads index data into an
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

    public void build(
            String resourceName,
            File file,
            Object2ObjectOpenHashMap<String, File> registry,
            VAOInstance vaoInstance) {

        if (iboManager.hasIBO(resourceName))
            return;

        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);

        if (hasQuadEntries(arpg))
            return;

        if (!arpg.has("ibo") || arpg.get("ibo").isNull())
            return;

        ArpgElementStruct iboEl = arpg.get("ibo");

        if (iboEl.isValue() && iboEl.getAsValue().isString()) {
            String refName = iboEl.getAsString();
            resolveRef(refName, resourceName, file, registry, vaoInstance);
            iboManager.registerIBO(resourceName, iboManager.getIBOHandleDirect(refName));
            return;
        }

        if (iboEl.isArray()) {
            iboManager.registerIBO(resourceName, buildFromData(iboEl.getAsArray(), vaoInstance, file));
            return;
        }

        throwException("IBO must be a string reference or index array in file: " + file.getName());
    }

    // Resolution \\

    private void resolveRef(
            String refName,
            String sourceResourceName,
            File sourceFile,
            Object2ObjectOpenHashMap<String, File> registry,
            VAOInstance vaoInstance) {

        if (iboManager.hasIBO(refName))
            return;

        File refFile = registry.get(refName);

        if (refFile == null)
            throwException("Referenced IBO '" + refName + "' not found. Source: " + sourceFile.getName());

        ArpgObjectStruct refArpg = ArpgUtility.loadObject(refFile);

        if (!refArpg.has("ibo") || refArpg.get("ibo").isNull())
            throwException("Referenced IBO file '" + refName + "' has no 'ibo' field.");

        ArpgElementStruct refEl = refArpg.get("ibo");

        if (!refEl.isArray())
            throwException("Referenced IBO '" + refName + "' must contain an index array.");

        iboManager.registerIBO(refName, buildFromData(refEl.getAsArray(), vaoInstance, refFile));
    }

    // Creation \\

    private IBOHandle buildFromData(
            ArpgArrayStruct indicesArray,
            VAOInstance vaoInstance,
            File file) {

        if (indicesArray.size() == 0)
            throwException("Index data array cannot be empty in file: " + file.getName());

        short[] indices = new short[indicesArray.size()];
        int index = 0;

        for (ArpgElementStruct indexEl : indicesArray) {
            int value = indexEl.getAsInt();
            if (value < 0 || value > 0xFFFF)
                throwException("Index out of 16-bit range: " + value + " in file: " + file.getName());
            indices[index++] = (short) value;
        }

        return IBOGLSLUtility.uploadIndexData(
                vaoInstance,
                create(IBOHandle.class),
                indices);
    }

    // Utility \\

    private boolean hasQuadEntries(ArpgObjectStruct arpg) {

        if (!arpg.has("vbo") || arpg.get("vbo").isNull())
            return false;

        ArpgElementStruct vboEl = arpg.get("vbo");

        if (!vboEl.isArray())
            return false;

        for (ArpgElementStruct el : vboEl.getAsArray())
            if (el.isObject())
                return true;

        return false;
    }
}