package editor.bootstrap.brushpipeline;

import editor.bootstrap.brushpipeline.brushmanager.BrushManager;
import engine.root.PipelinePackage;

public class BrushPipeline extends PipelinePackage {

    /*
     * Registers the editor's brush presets, the one set every tool that
     * strokes reads its shape, falloff, density and mode from, whichever
     * window it strokes in.
     */

    // Internal \\

    @Override
    protected void create() {
        create(BrushManager.class);
    }
}
