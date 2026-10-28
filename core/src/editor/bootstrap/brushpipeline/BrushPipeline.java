package editor.bootstrap.brushpipeline;

import editor.bootstrap.brushpipeline.brushmanager.BrushManager;
import engine.root.PipelinePackage;

public class BrushPipeline extends PipelinePackage {

    /*
     * Registers the editor's brushes, the tips every tool that strokes
     * stamps with, whichever window it strokes in.
     */

    // Internal \\

    @Override
    protected void create() {
        create(BrushManager.class);
    }
}
