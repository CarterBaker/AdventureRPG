package com.AdventureRPG.core.geometry;

import com.AdventureRPG.core.engine.PipelineFrame;
import com.AdventureRPG.core.geometry.ibomanager.IBOManager;
import com.AdventureRPG.core.geometry.modelmanager.ModelManager;
import com.AdventureRPG.core.geometry.vaomanager.VAOManager;
import com.AdventureRPG.core.geometry.vbomanager.VBOManager;

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
