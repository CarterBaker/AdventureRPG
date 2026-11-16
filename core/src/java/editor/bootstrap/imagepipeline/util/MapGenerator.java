package editor.bootstrap.imagepipeline.util;

import editor.bootstrap.imagepipeline.imagemap.MapGenerateScratchStruct;

public interface MapGenerator {

    /*
     * Works one map out of an image prepared in the scratch, by the map's own
     * settings, writing RGBA8888 pixels rows top first into the output.
     */

    void generate(MapGenerateScratchStruct scratch, float[] settings, int[] out);
}
