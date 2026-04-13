package application.bootstrap.lightingpipeline;

import application.bootstrap.lightingpipeline.naturallightmanager.NaturalLightManager;
import application.core.engine.PipelinePackage;

public class LightingPipeline extends PipelinePackage {

    /*
     * Registers all lighting pipeline managers. NaturalLightManager drives
     * sun and moon blending and pushes directional light state to the GPU
     * each frame.
     */

    @Override
    protected void create() {
        create(NaturalLightManager.class);
    }
}