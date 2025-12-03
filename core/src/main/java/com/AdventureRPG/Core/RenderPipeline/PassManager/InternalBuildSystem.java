package com.AdventureRPG.Core.RenderPipeline.PassManager;

import java.io.File;

import com.AdventureRPG.Core.Bootstrap.SystemFrame;
import com.AdventureRPG.Core.RenderPipeline.Material.Material;
import com.AdventureRPG.Core.RenderPipeline.MaterialManager.MaterialManager;
import com.AdventureRPG.Core.RenderPipeline.ProcessingPass.ProcessingPass;
import com.AdventureRPG.Core.Util.FileUtility;
import com.AdventureRPG.Core.Util.JsonUtility;
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
