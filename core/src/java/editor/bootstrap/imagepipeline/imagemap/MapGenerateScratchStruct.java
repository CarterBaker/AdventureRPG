package editor.bootstrap.imagepipeline.imagemap;

import editor.bootstrap.imagepipeline.util.MapGenerateUtility;
import engine.assets.image.Pixmap;
import engine.root.EngineSetting;
import engine.root.StructPackage;

public class MapGenerateScratchStruct extends StructPackage {

    /*
     * The image a map is worked out of and the float images working it out
     * takes, rows top first: its RGBA8888 pixels and brightness, set once
     * per generation, and the height, a working image, a blur and its
     * scratch, written by whichever map is generated. Held by ImageManager
     * and grown only when an image larger than any before is generated, so
     * generating again allocates nothing.
     */

    // Size
    private int width;
    private int rows;
    private int count;

    // Images
    private int[] pixels;
    private float[] luminance;
    private float[] height;
    private float[] work;
    private float[] blurred;
    private float[] scratch;
    private final float[] normal;

    // Constructor \\

    public MapGenerateScratchStruct() {

        // Images
        this.pixels = new int[0];
        this.luminance = new float[0];
        this.height = new float[0];
        this.work = new float[0];
        this.blurred = new float[0];
        this.scratch = new float[0];
        this.normal = new float[EngineSetting.COLOR_CHANNEL_COUNT - 1];
    }

    // Management \\

    // The image's pixels and brightness taken, the images grown to hold it
    public void prepare(Pixmap source) {

        this.width = source.getWidth();
        this.rows = source.getHeight();
        this.count = width * rows;

        if (pixels.length < count) {
            this.pixels = new int[count];
            this.luminance = new float[count];
            this.height = new float[count];
            this.work = new float[count];
            this.blurred = new float[count];
            this.scratch = new float[count];
        }

        for (int y = 0; y < rows; y++)
            for (int x = 0; x < width; x++)
                pixels[y * width + x] = source.getPixel(x, y);

        MapGenerateUtility.toLuminance(pixels, luminance, count);
    }

    // Accessible \\

    public int getWidth() {
        return width;
    }

    public int getRows() {
        return rows;
    }

    public int getCount() {
        return count;
    }

    public int[] getPixels() {
        return pixels;
    }

    public float[] getLuminance() {
        return luminance;
    }

    public float[] getHeight() {
        return height;
    }

    public float[] getWork() {
        return work;
    }

    public float[] getBlurred() {
        return blurred;
    }

    public float[] getScratch() {
        return scratch;
    }

    public float[] getNormal() {
        return normal;
    }
}
