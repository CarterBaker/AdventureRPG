package application.bootstrap.contentpipeline;

import application.bootstrap.contentpipeline.contentmanager.ContentManager;
import engine.root.PipelinePackage;

public class ContentPipeline extends PipelinePackage {

    /*
     * Registers ContentManager, which owns the ores, rocks and roots whole
     * blocks hold. It is created after the world pipeline, whose blocks its
     * contents name as hosts, so every block is loaded before contents load
     * in awake(); world systems resolve it in their get() phase as any other
     * manager.
     */

    @Override
    protected void create() {
        create(ContentManager.class);
    }
}
