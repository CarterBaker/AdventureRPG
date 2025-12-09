package com.AdventureRPG.core.shaderpipeline.passmanager;

import java.io.File;

import com.AdventureRPG.core.kernel.SystemFrame;
import com.AdventureRPG.core.shaderpipeline.material.Material;
import com.AdventureRPG.core.shaderpipeline.materialmanager.MaterialManager;
import com.AdventureRPG.core.shaderpipeline.processingpass.ProcessingPass;
import com.AdventureRPG.core.util.FileUtility;
import com.AdventureRPG.core.util.JsonUtility;
import com.google.gson.JsonObject;

class InternalBuildSystem extends SystemFrame {

    // Internal
    private MaterialManager materialManager;

    // Base \\

    @Override
    protected void init() {

        // Internal
        this.materialManager = gameEngine.get(MaterialManager.class);
    }

    // Pass Management \\

    ProcessingPass buildPass(File file, int passID) {

        String passName = FileUtility.getFileName(file);

        JsonObject json = JsonUtility.loadJsonObject(file);
        int materialID = getPassID(json);

        Material material = materialManager.getMaterialFromMaterialID(materialID);

        return new ProcessingPass(
                passName,
                passID,
                material);
    }

    private int getPassID(JsonObject json) {
        String passName = JsonUtility.validateString(json, "material");
        return materialManager.getMaterialIDFromMaterialName(passName);
    }
}
