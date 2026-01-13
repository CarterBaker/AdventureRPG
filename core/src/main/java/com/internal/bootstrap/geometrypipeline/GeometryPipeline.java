package com.internal.bootstrap.geometrypipeline;

import com.internal.bootstrap.geometrypipeline.buildManager.BuildManager;
import com.internal.bootstrap.geometrypipeline.ibomanager.IBOManager;
import com.internal.bootstrap.geometrypipeline.modelmanager.ModelManager;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOManager;
import com.internal.bootstrap.geometrypipeline.vbomanager.VBOManager;
import com.internal.core.engine.PipelinePackage;

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
