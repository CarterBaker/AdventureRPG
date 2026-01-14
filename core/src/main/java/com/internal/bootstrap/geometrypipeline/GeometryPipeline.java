package com.internal.bootstrap.geometrypipeline;

import com.internal.bootstrap.geometrypipeline.ibomanager.IBOManager;
import com.internal.bootstrap.geometrypipeline.meshmanager.MeshManager;
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
        create(MeshManager.class);
    }
}
