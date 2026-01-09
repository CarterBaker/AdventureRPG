package com.AdventureRPG.bootstrap.geometrypipeline;

import com.AdventureRPG.bootstrap.geometrypipeline.buildManager.BuildManager;
import com.AdventureRPG.bootstrap.geometrypipeline.ibomanager.IBOManager;
import com.AdventureRPG.bootstrap.geometrypipeline.modelmanager.ModelManager;
import com.AdventureRPG.bootstrap.geometrypipeline.vaomanager.VAOManager;
import com.AdventureRPG.bootstrap.geometrypipeline.vbomanager.VBOManager;
import com.AdventureRPG.core.engine.PipelinePackage;

public class GeometryPipeline extends PipelinePackage {

    @Override
    protected void create() {

        // Geometry Pipeline
        create(VBOManager.class);
        create(IBOManager.class);
        create(VAOManager.class);
        create(ModelManager.class);
        create(BuildManager.class);
    }
}
