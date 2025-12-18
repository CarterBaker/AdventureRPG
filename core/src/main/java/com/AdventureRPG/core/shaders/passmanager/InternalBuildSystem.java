package com.AdventureRPG.core.shaders.passmanager;

import java.io.File;

import com.AdventureRPG.core.engine.SystemFrame;
import com.AdventureRPG.core.geometry.mesh.MeshHandle;
import com.AdventureRPG.core.geometry.modelmanager.ModelManager;
import com.AdventureRPG.core.shaders.materialmanager.MaterialManager;
import com.AdventureRPG.core.shaders.materials.Material;
import com.AdventureRPG.core.shaders.processingpass.ProcessingPass;
import com.AdventureRPG.core.util.FileUtility;
import com.AdventureRPG.core.util.JsonUtility;
import com.google.gson.JsonObject;

class InternalBuildSystem extends SystemFrame {

    // Internal
    private MaterialManager materialManager;
    private MeshHandle processingTriangle;

    // Base \\

    @Override
    protected void init() {

        // Internal
        this.materialManager = gameEngine.get(MaterialManager.class);

        ModelManager modelManager = gameEngine.get(ModelManager.class);
        int meshID = modelManager.getMeshHandleIDFromMeshName("util/ProcessingTriangle");
        this.processingTriangle = modelManager.getMeshHandleFromMeshHandleID(meshID);
    }

    // Pass Management \\

    ProcessingPass buildPass(File root, File file, int passID) {

        String passName = FileUtility.getPathWithFileNameWithoutExtension(root, file);

        JsonObject json = JsonUtility.loadJsonObject(file);
        int materialID = getPassID(json);

        Material material = materialManager.getMaterialFromMaterialID(materialID);

        return new ProcessingPass(
                passName,
                passID,
                material,
                processingTriangle);
    }

    private int getPassID(JsonObject json) {
        String passName = JsonUtility.validateString(json, "material");
        return materialManager.getMaterialIDFromMaterialName(passName);
    }
}
