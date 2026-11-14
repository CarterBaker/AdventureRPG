package application.bootstrap.animationpipeline;

import application.bootstrap.animationpipeline.animationmanager.AnimationManager;
import engine.root.PipelinePackage;

public class AnimationPipeline extends PipelinePackage {

    /*
     * Registers the animation clip manager. Bone-track validation happens
     * against RigManager via the on-demand load path, so registration order
     * relative to GeometryPipeline is not load-bearing — kept after it here
     * purely for readability.
     */

    @Override
    protected void create() {
        create(AnimationManager.class);
    }
}