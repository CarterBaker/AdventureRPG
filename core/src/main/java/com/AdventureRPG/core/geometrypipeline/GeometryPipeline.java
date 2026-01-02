package com.AdventureRPG.core.geometrypipeline;

import com.AdventureRPG.core.engine.PipelineFrame;
import com.AdventureRPG.core.geometrypipeline.ibomanager.IBOManager;
import com.AdventureRPG.core.geometrypipeline.modelmanager.ModelManager;
import com.AdventureRPG.core.geometrypipeline.vaomanager.VAOManager;
import com.AdventureRPG.core.geometrypipeline.vbomanager.VBOManager;

public class GeometryPipeline extends PipelineFrame {

    // Geometry Pipeline
    private VBOManager vboManager;
    private IBOManager iboManager;
    private VAOManager vaoManager;
    private ModelManager modelManager;

    @Override
    protected void create() {

        // Geometry Pipeline
        this.vboManager = create(VBOManager.class);
        this.iboManager = create(IBOManager.class);
        this.vaoManager = create(VAOManager.class);
        this.modelManager = create(ModelManager.class);
    }
}
