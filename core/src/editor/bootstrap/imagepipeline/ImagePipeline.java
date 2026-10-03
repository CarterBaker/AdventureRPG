package editor.bootstrap.imagepipeline;

import editor.bootstrap.imagepipeline.imagemanager.ImageManager;
import engine.root.PipelinePackage;

public class ImagePipeline extends PipelinePackage {

    /*
     * Registers the editor's image editing framework, so every tool that
     * paints into an image shares one set of open images, their history and
     * their GPU textures.
     */

    // Internal \\

    @Override
    protected void create() {
        create(ImageManager.class);
    }
}
