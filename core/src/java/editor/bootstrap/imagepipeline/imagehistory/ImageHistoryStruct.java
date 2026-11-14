package editor.bootstrap.imagepipeline.imagehistory;

import editor.bootstrap.imagepipeline.imageregion.ImageRegionStruct;
import engine.root.StructPackage;

public class ImageHistoryStruct extends StructPackage {

    /*
     * One undo step of an image: every pixel a stroke or fill changed, by
     * index into the image, with its RGBA8888 color before and after, and the
     * region they span, so undo and redo each write one side back.
     */

    // Pixels
    private final int[] pixelIndices;
    private final int[] beforeColors;
    private final int[] afterColors;

    // Region
    private final ImageRegionStruct region;

    // Constructor \\

    public ImageHistoryStruct(int[] pixelIndices, int[] beforeColors, int[] afterColors, ImageRegionStruct region) {

        // Pixels
        this.pixelIndices = pixelIndices;
        this.beforeColors = beforeColors;
        this.afterColors = afterColors;

        // Region
        this.region = new ImageRegionStruct();
        this.region.set(region);
    }

    // Accessible \\

    public int getPixelCount() {
        return pixelIndices.length;
    }

    public int getPixelIndex(int index) {
        return pixelIndices[index];
    }

    public int getBeforeColor(int index) {
        return beforeColors[index];
    }

    public int getAfterColor(int index) {
        return afterColors[index];
    }

    public ImageRegionStruct getRegion() {
        return region;
    }
}
