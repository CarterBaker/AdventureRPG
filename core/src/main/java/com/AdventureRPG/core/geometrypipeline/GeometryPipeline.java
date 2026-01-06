package com.AdventureRPG.core.geometrypipeline;

import com.AdventureRPG.core.engine.PipelineFrame;
import com.AdventureRPG.core.geometrypipeline.buildManager.BuildManager;
import com.AdventureRPG.core.geometrypipeline.ibomanager.IBOManager;
import com.AdventureRPG.core.geometrypipeline.modelmanager.ModelManager;
import com.AdventureRPG.core.geometrypipeline.vaomanager.VAOManager;
import com.AdventureRPG.core.geometrypipeline.vbomanager.VBOManager;

public class GeometryPipeline extends PipelineFrame {

    /* !!! Released after use !!! */

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
