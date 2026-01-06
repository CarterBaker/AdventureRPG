package com.AdventureRPG.core.shaderpipeline.passmanager;

import java.io.File;

import com.AdventureRPG.core.engine.SystemPackage;
import com.AdventureRPG.core.geometrypipeline.modelmanager.MeshHandle;
import com.AdventureRPG.core.geometrypipeline.modelmanager.ModelManager;
import com.AdventureRPG.core.shaderpipeline.materialmanager.MaterialHandle;
import com.AdventureRPG.core.shaderpipeline.materialmanager.MaterialManager;
import com.AdventureRPG.core.util.FileUtility;
import com.AdventureRPG.core.util.JsonUtility;
import com.google.gson.JsonObject;

class InternalBuildSystem extends SystemPackage {

    // Internal
    private ModelManager modelManager;
    private MaterialManager materialManager;
    private MeshHandle processingTriangle;

    // Base \\

    @Override
    protected void get() {

        // Internal
        this.modelManager = get(ModelManager.class);
        this.materialManager = get(MaterialManager.class);
    }

    // Pass Management \\

    void assignMeshData() {

        // Internal
        int meshID = modelManager.getMeshHandleIDFromMeshName("util/ProcessingTriangle");
        this.processingTriangle = modelManager.getMeshHandleFromMeshHandleID(meshID);
    }

    PassHandle buildPass(File root, File file, int passID) {

        String passName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        JsonObject json = JsonUtility.loadJsonObject(file);
        MaterialHandle material = materialManager.getMaterialFromMaterialID(getPassID(json));

        PassHandle processingPassHandle = create(PassHandle.class);
        processingPassHandle.constructor(
                passName,
                passID,
                material,
                processingTriangle);

        return processingPassHandle;
    }

    private int getPassID(JsonObject json) {
        String passName = JsonUtility.validateString(json, "material");
        return materialManager.getMaterialIDFromMaterialName(passName);
    }
}
