package editor.bootstrap.imagepipeline.imagemanager;

import editor.bootstrap.imagepipeline.imagemap.MapGenerateScratchStruct;
import editor.bootstrap.imagepipeline.util.ImageMap;
import engine.assets.image.Pixmap;
import engine.root.BranchPackage;

class ImageMapBranch extends BranchPackage {

    /*
     * Works a map out of an image, the one path every editor generates its
     * maps through: the image's pixels taken into the scratch once, then the
     * map's own generator run by the map's own settings.
     */

    // Scratch
    private MapGenerateScratchStruct scratch;

    // Base \\

    @Override
    protected void create() {
        this.scratch = new MapGenerateScratchStruct();
    }

    // Generate \\

    void generate(ImageMap map, Pixmap source, float[] settings, int[] out) {
        scratch.prepare(source);
        map.getGenerator().generate(scratch, settings, out);
    }
}
