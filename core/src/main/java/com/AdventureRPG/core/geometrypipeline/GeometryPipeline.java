package com.AdventureRPG.core.geometrypipeline;

import com.AdventureRPG.core.kernel.PipelineFrame;

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
        this.vboManager = (VBOManager) register(new VBOManager());
        this.iboManager = (IBOManager) register(new IBOManager());
        this.vaoManager = (VAOManager) register(new VAOManager());
        this.modelManager = (ModelManager) register(new ModelManager());
    }
}
