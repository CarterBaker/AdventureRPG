package editor.bootstrap.imagepipeline.imagemanager;

import editor.bootstrap.imagepipeline.imagebrush.ImageBrushStruct;
import editor.bootstrap.imagepipeline.imagedocument.ImageDocumentInstance;
import editor.bootstrap.imagepipeline.imageregion.ImageRegionStruct;
import engine.assets.image.Pixmap;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.ints.IntArrayList;

class ImageEditBranch extends BranchPackage {

    /*
     * Performs every change to an image's pixels. A brush stamps a disc of
     * its radius, a stroke stamps every pixel along its path, and a fill
     * floods the four-connected run of one color. Coordinates wrap on an
     * image whose edges wrap and are clipped on any other. Every pixel that
     * actually changes is recorded for undo and grows the caller's region.
     */

    // Internal
    private ImageHistoryBranch imageHistoryBranch;

    // Scratch
    private IntArrayList fillStack;

    // Base \\

    @Override
    protected void create() {
        this.fillStack = new IntArrayList();
    }

    @Override
    protected void get() {
        this.imageHistoryBranch = get(ImageHistoryBranch.class);
    }

    // Brush \\

    void stroke(
            ImageDocumentInstance document,
            int fromX,
            int fromY,
            int toX,
            int toY,
            ImageBrushStruct brush,
            ImageRegionStruct region) {

        int steps = Math.max(Math.abs(toX - fromX), Math.abs(toY - fromY));

        for (int step = 1; step <= steps; step++) {

            float t = step / (float) steps;

            stamp(
                    document,
                    Math.round(fromX + (toX - fromX) * t),
                    Math.round(fromY + (toY - fromY) * t),
                    brush,
                    region);
        }
    }

    // Covers every pixel whose centre lies within half a pixel past the radius, so small discs stay round
    void stamp(
            ImageDocumentInstance document,
            int centerX,
            int centerY,
            ImageBrushStruct brush,
            ImageRegionStruct region) {

        int radius = brush.getRadius();
        int reachSq = radius * radius + radius;

        for (int dy = -radius; dy <= radius; dy++)
            for (int dx = -radius; dx <= radius; dx++)
                if (dx * dx + dy * dy <= reachSq)
                    writePixel(document, centerX + dx, centerY + dy, brush.getColor(), region);
    }

    // Fill \\

    void fill(ImageDocumentInstance document, int x, int y, int color, ImageRegionStruct region) {

        int start = toPixelIndex(document, x, y);

        if (start == EngineSetting.INDEX_NOT_FOUND)
            return;

        int width = document.getWidth();
        Pixmap pixmap = document.getPixmap();
        int target = pixmap.getPixel(start % width, start / width);

        if (target == color)
            return;

        fillStack.clear();
        fillStack.add(start);

        while (!fillStack.isEmpty()) {

            int index = fillStack.popInt();
            int pixelX = index % width;
            int pixelY = index / width;

            if (pixmap.getPixel(pixelX, pixelY) != target)
                continue;

            writePixel(document, pixelX, pixelY, color, region);
            pushFillNeighbor(document, pixelX + 1, pixelY, target);
            pushFillNeighbor(document, pixelX - 1, pixelY, target);
            pushFillNeighbor(document, pixelX, pixelY + 1, target);
            pushFillNeighbor(document, pixelX, pixelY - 1, target);
        }
    }

    private void pushFillNeighbor(ImageDocumentInstance document, int x, int y, int target) {

        int index = toPixelIndex(document, x, y);

        if (index != EngineSetting.INDEX_NOT_FOUND
                && document.getPixmap().getPixel(index % document.getWidth(), index / document.getWidth()) == target)
            fillStack.add(index);
    }

    // Pixels \\

    private void writePixel(ImageDocumentInstance document, int x, int y, int color, ImageRegionStruct region) {

        int index = toPixelIndex(document, x, y);

        if (index == EngineSetting.INDEX_NOT_FOUND)
            return;

        int width = document.getWidth();
        int pixelX = index % width;
        int pixelY = index / width;
        Pixmap pixmap = document.getPixmap();
        int before = pixmap.getPixel(pixelX, pixelY);

        if (before == color)
            return;

        imageHistoryBranch.record(document, index, before);
        pixmap.drawPixel(pixelX, pixelY, color);
        region.include(pixelX, pixelY);
    }

    // An edge pixel stands in for any point past it on an image that does not wrap
    int readPixel(ImageDocumentInstance document, int x, int y) {

        int width = document.getWidth();
        int height = document.getHeight();
        int pixelX = document.isWrapping() ? Math.floorMod(x, width) : Math.max(0, Math.min(width - 1, x));
        int pixelY = document.isWrapping() ? Math.floorMod(y, height) : Math.max(0, Math.min(height - 1, y));

        return document.getPixmap().getPixel(pixelX, pixelY);
    }

    private int toPixelIndex(ImageDocumentInstance document, int x, int y) {

        int width = document.getWidth();
        int height = document.getHeight();

        if (document.isWrapping())
            return Math.floorMod(y, height) * width + Math.floorMod(x, width);

        if (x < 0 || y < 0 || x >= width || y >= height)
            return EngineSetting.INDEX_NOT_FOUND;

        return y * width + x;
    }
}
