package application.bootstrap.geometrypipeline.vaomanager;

import java.io.File;

import application.bootstrap.geometrypipeline.vao.VAOHandle;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class VAOBuilder extends BuilderPackage {

    /*
     * Parses the 'vao' field from mesh ARPG and constructs a VAOHandle layout
     * template. Supports direct attribute size arrays and string references to
     * other registered VAOs. When the mesh ARPG declares a "rig", two extra
     * trailing attributes — bone indices and bone weights, each
     * EngineSetting.MAX_BONE_INFLUENCES floats wide — are appended after the
     * declared attributes. Bootstrap-only.
     */

    // Internal
    private VAOManager vaoManager;

    // Base \\

    @Override
    protected void get() {

        // Internal
        this.vaoManager = get(VAOManager.class);
    }

    // Build \\

    public void build(
            String resourceName,
            File file,
            Object2ObjectOpenHashMap<String, File> registry) {

        if (vaoManager.hasVAO(resourceName))
            return;

        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);

        if (!arpg.has("vao") || arpg.get("vao").isNull())
            return;

        ArpgElementStruct vaoEl = arpg.get("vao");
        boolean hasBones = hasRig(arpg);

        if (vaoEl.isValue() && vaoEl.getAsValue().isString()) {

            if (hasBones)
                throwException("Bone-weighted mesh must declare its own inline \"vao\" array — "
                        + "cannot reference a shared VAO template, since that would change the "
                        + "stride for every other mesh sharing it. File: " + file.getName());

            String refName = vaoEl.getAsString();
            resolveRef(refName, file, registry);
            vaoManager.registerVAO(resourceName, vaoManager.getVAOHandleDirect(refName));
            return;
        }

        if (vaoEl.isArray()) {
            vaoManager.registerVAO(resourceName, buildLayout(vaoEl.getAsArray(), file, hasBones));
            return;
        }

        throwException("VAO must be a string reference or int array in file: " + file.getName());
    }

    // Resolution \\

    private void resolveRef(
            String refName,
            File sourceFile,
            Object2ObjectOpenHashMap<String, File> registry) {

        if (vaoManager.hasVAO(refName))
            return;

        File refFile = registry.get(refName);

        if (refFile == null)
            throwException("Referenced VAO '" + refName + "' not found. Source: " + sourceFile.getName());

        ArpgObjectStruct refArpg = ArpgUtility.loadObject(refFile);

        if (!refArpg.has("vao") || refArpg.get("vao").isNull())
            throwException("Referenced VAO file '" + refName + "' has no 'vao' field.");

        ArpgElementStruct refEl = refArpg.get("vao");

        if (!refEl.isArray())
            throwException("Referenced VAO '" + refName + "' must contain an int array.");

        vaoManager.registerVAO(refName, buildLayout(refEl.getAsArray(), refFile, hasRig(refArpg)));
    }

    // Creation \\

    private VAOHandle buildLayout(ArpgArrayStruct arpgArray, File file, boolean hasBones) {

        if (arpgArray.size() == 0)
            throwException("VAO attribute size array must not be empty in file: " + file.getName());

        int declaredCount = arpgArray.size();
        int totalCount = hasBones ? declaredCount + 2 : declaredCount;
        int[] attrSizes = new int[totalCount];

        for (int i = 0; i < declaredCount; i++) {
            attrSizes[i] = arpgArray.get(i).getAsInt();
            if (attrSizes[i] <= 0)
                throwException("VAO attribute size must be positive in file: " + file.getName());
        }

        if (hasBones) {
            attrSizes[declaredCount] = EngineSetting.MAX_BONE_INFLUENCES;
            attrSizes[declaredCount + 1] = EngineSetting.MAX_BONE_INFLUENCES;
        }

        VAOHandle handle = create(VAOHandle.class);
        handle.constructor(attrSizes);

        return handle;
    }

    // Utility \\

    private boolean hasRig(ArpgObjectStruct arpg) {
        return arpg.has("rig") && !arpg.get("rig").isNull();
    }
}