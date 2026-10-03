package application.bootstrap.mappipeline;

import application.bootstrap.mappipeline.mapmanager.MapManager;
import engine.root.PipelinePackage;

public class MapPipeline extends PipelinePackage {

    /*
     * Registers the map manager, which turns the world into a top-down map
     * any view can draw. Created after the world and ocean pipelines, so the
     * terrain it samples and the chunks it reads are in place for the frame.
     */

    @Override
    protected void create() {
        create(MapManager.class);
    }
}
