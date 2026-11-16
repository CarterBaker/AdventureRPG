package editor.bootstrap.imageeditorpipeline;

import editor.bootstrap.imageeditorpipeline.imageeditormanager.ImageEditorManager;
import engine.root.PipelinePackage;

public class ImageEditorPipeline extends PipelinePackage {

    /*
     * Registers the Image Editor: the layered images the owner makes and
     * paints under Textures, as a painting tool's documents, with the maps
     * each carries, saved as .arpg files beside the player's saves.
     */

    // Internal \\

    @Override
    protected void create() {
        create(ImageEditorManager.class);
    }
}
