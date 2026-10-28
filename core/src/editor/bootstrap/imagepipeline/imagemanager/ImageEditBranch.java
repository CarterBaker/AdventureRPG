package editor.bootstrap.imagepipeline.imagemanager;

import editor.bootstrap.brushpipeline.brush.BrushHandle;
import editor.bootstrap.brushpipeline.util.BrushField;
import editor.bootstrap.brushpipeline.util.BrushUtility;
import editor.bootstrap.imagepipeline.imagebrush.ImageBrushStruct;
import editor.bootstrap.imagepipeline.imagedocument.ImageDocumentInstance;
import editor.bootstrap.imagepipeline.imageregion.ImageRegionStruct;
import editor.runtime.EditorSetting;
import engine.assets.image.Pixmap;
import engine.graphics.color.PackedColorUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.ints.IntArrayList;

class ImageEditBranch extends BranchPackage {

    /*
     * Performs every change to an image's pixels. A stroke stamps its brush's
     * tip around a pixel, each covered pixel taken by the stroke's strength
     * scaled by how much of the tip covers it, and only the tip's density of
     * them touched, stamping along its path once every spacing and keeping the
     * gap from one drag to the next. Paint lays the stroke's color down, Smooth
     * softens toward the pixel's square, Swirl turns the tip's pixels about its
     * centre by the stroke's twist and Scatter shuffles them. Where the image
     * blends, strength fades a pixel toward what it takes; where it does not,
     * strength is the chance the pixel takes it whole, Smooth takes the
     * commonest color around and Scatter only moves colors, so no color the
     * image could not hold is ever written. A fill floods the four-connected
     * run of one color. Coordinates wrap on an image whose edges wrap and are
     * clipped on any other. Every pixel that actually changes is recorded for
     * undo and grows the caller's region.
     */

    // Internal
    private ImageHistoryBranch imageHistoryBranch;

    // Scratch
    private IntArrayList fillStack;
    private IntArrayList tipPixels;
    private int[] neighbourColors;
    private int[] neighbourCounts;
    private int randomState;

    // Base \\

    @Override
    protected void create() {

        int side = EditorSetting.IMAGE_SMOOTH_REACH_PIXELS * 2 + 1;

        this.fillStack = new IntArrayList();
        this.tipPixels = new IntArrayList();
        this.neighbourColors = new int[side * side];
        this.neighbourCounts = new int[side * side];
        this.randomState = EditorSetting.IMAGE_BRUSH_RANDOM_SEED;
    }

    @Override
    protected void get() {
        this.imageHistoryBranch = get(ImageHistoryBranch.class);
    }

    // Brush \\

    // A stamp every spacing from where the stroke stands toward a pixel, the stroke left on its last stamp
    void stroke(
            ImageDocumentInstance document,
            int toX,
            int toY,
            ImageBrushStruct brush,
            ImageRegionStruct region) {

        int fromX = document.getStrokeX();
        int fromY = document.getStrokeY();
        float length = (float) Math.hypot(toX - fromX, toY - fromY);
        float spacing = BrushUtility.resolveSpacing(brush.getBrush(), brush.getRadius());
        int stamps = (int) (length / spacing);

        for (int i = 1; i <= stamps; i++) {

            float t = i * spacing / length;
            int x = Math.round(fromX + (toX - fromX) * t);
            int y = Math.round(fromY + (toY - fromY) * t);

            stamp(document, x, y, brush, region);
            document.moveStroke(x, y);
        }
    }

    // A moving stroke reads the tip's pixels before any is written, so Swirl and Scatter move what was there
    void stamp(
            ImageDocumentInstance document,
            int centerX,
            int centerY,
            ImageBrushStruct brush,
            ImageRegionStruct region) {

        BrushHandle tip = brush.getBrush();
        int radius = brush.getRadius();
        float reach = radius + EditorSetting.IMAGE_BRUSH_EDGE_PIXELS;
        float density = tip.getValue(BrushField.DENSITY);

        if (brush.getStroke().isMoving())
            captureTip(document, centerX, centerY, radius);

        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {

                float strength = BrushUtility.resolveCoverage(tip, dx, dy, reach) * brush.getStrength();

                if (strength <= 0f || nextRandom() >= density)
                    continue;

                int x = centerX + dx;
                int y = centerY + dy;

                writePixel(document, x, y, resolveColor(document, brush, x, y, dx, dy, strength), region);
            }
        }
    }

    private void captureTip(ImageDocumentInstance document, int centerX, int centerY, int radius) {

        tipPixels.clear();

        for (int dy = -radius; dy <= radius; dy++)
            for (int dx = -radius; dx <= radius; dx++)
                tipPixels.add(readPixel(document, centerX + dx, centerY + dy));
    }

    private int resolveColor(
            ImageDocumentInstance document,
            ImageBrushStruct brush,
            int x,
            int y,
            int dx,
            int dy,
            float strength) {

        int before = readPixel(document, x, y);
        boolean blending = brush.isBlending();

        return switch (brush.getStroke()) {
            case PAINT -> blend(before, brush.getColor(), strength, blending);
            case SMOOTH -> blend(
                    before,
                    blending ? averagePixel(document, x, y) : commonestPixel(document, x, y),
                    strength,
                    blending);
            case SWIRL -> swirlPixel(document, brush, x - dx, y - dy, dx, dy, strength);
            case SCATTER -> blend(before, tipPixels.getInt(randomIndex(tipPixels.size())), strength, false);
        };
    }

    // Faded by strength where the image blends, otherwise taken whole with strength as its chance
    private int blend(int before, int target, float strength, boolean blending) {

        if (!blending)
            return nextRandom() < strength ? target : before;

        return (PackedColorUtility.mix(before >>> Byte.SIZE, target >>> Byte.SIZE, strength) << Byte.SIZE)
                | (before & EngineSetting.PACKED_COLOR_CHANNEL_MASK);
    }

    // The pixel the tip's twist carries here, turned further the stronger the tip is at this pixel
    private int swirlPixel(
            ImageDocumentInstance document,
            ImageBrushStruct brush,
            int centerX,
            int centerY,
            int dx,
            int dy,
            float strength) {

        double angle = -Math.toRadians(brush.getTwist()) * strength;
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        int sourceX = (int) Math.round(dx * cos - dy * sin);
        int sourceY = (int) Math.round(dx * sin + dy * cos);
        int radius = brush.getRadius();

        if (Math.abs(sourceX) <= radius && Math.abs(sourceY) <= radius)
            return readTip(radius, sourceX, sourceY);

        return readPixel(document, centerX + sourceX, centerY + sourceY);
    }

    private int readTip(int radius, int dx, int dy) {
        return tipPixels.getInt((dy + radius) * (radius * 2 + 1) + dx + radius);
    }

    // Each color channel averaged over the pixel's square, keeping the pixel's own alpha; pixels are RGBA8888
    private int averagePixel(ImageDocumentInstance document, int x, int y) {

        int reach = EditorSetting.IMAGE_SMOOTH_REACH_PIXELS;
        float red = 0f;
        float green = 0f;
        float blue = 0f;
        int count = 0;

        for (int dy = -reach; dy <= reach; dy++) {
            for (int dx = -reach; dx <= reach; dx++) {

                int color = readPixel(document, x + dx, y + dy) >>> Byte.SIZE;

                red += PackedColorUtility.red(color);
                green += PackedColorUtility.green(color);
                blue += PackedColorUtility.blue(color);
                count++;
            }
        }

        return (PackedColorUtility.pack(red / count, green / count, blue / count) << Byte.SIZE)
                | (readPixel(document, x, y) & EngineSetting.PACKED_COLOR_CHANNEL_MASK);
    }

    // The color found most often in the pixel's square, the pixel's own winning a tie
    private int commonestPixel(ImageDocumentInstance document, int x, int y) {

        int reach = EditorSetting.IMAGE_SMOOTH_REACH_PIXELS;
        int own = readPixel(document, x, y);
        int distinct = 0;
        int commonest = own;
        int commonestCount = 0;

        for (int dy = -reach; dy <= reach; dy++) {
            for (int dx = -reach; dx <= reach; dx++) {

                int color = readPixel(document, x + dx, y + dy);
                int slot = 0;

                while (slot < distinct && neighbourColors[slot] != color)
                    slot++;

                if (slot == distinct) {
                    neighbourColors[distinct] = color;
                    neighbourCounts[distinct++] = 0;
                }

                int count = ++neighbourCounts[slot];

                if (count > commonestCount || (count == commonestCount && color == own)) {
                    commonest = color;
                    commonestCount = count;
                }
            }
        }

        return commonest;
    }

    // Random \\

    // A xorshift step, uniform in [0, 1); brushes need no stronger randomness and allocate nothing
    private float nextRandom() {

        randomState ^= randomState << 13;
        randomState ^= randomState >>> 17;
        randomState ^= randomState << 5;

        return (randomState >>> Byte.SIZE) * EditorSetting.IMAGE_BRUSH_RANDOM_SCALE;
    }

    private int randomIndex(int size) {
        return Math.min(size - 1, (int) (nextRandom() * size));
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
