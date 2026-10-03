package editor.bootstrap.imagepipeline.imageregion;

import engine.root.StructPackage;

public class ImageRegionStruct extends StructPackage {

    /*
     * The bounding box of the pixels an image change touched, inclusive on
     * every side, grown pixel by pixel or region by region and cleared for
     * reuse, so tracking a change allocates nothing.
     */

    // Bounds
    private int minX;
    private int minY;
    private int maxX;
    private int maxY;
    private boolean empty;

    // Constructor \\

    public ImageRegionStruct() {
        clear();
    }

    // Management \\

    public void clear() {
        this.minX = Integer.MAX_VALUE;
        this.minY = Integer.MAX_VALUE;
        this.maxX = Integer.MIN_VALUE;
        this.maxY = Integer.MIN_VALUE;
        this.empty = true;
    }

    public void include(int x, int y) {
        this.minX = Math.min(minX, x);
        this.minY = Math.min(minY, y);
        this.maxX = Math.max(maxX, x);
        this.maxY = Math.max(maxY, y);
        this.empty = false;
    }

    public void include(ImageRegionStruct region) {

        if (region.isEmpty())
            return;

        include(region.getMinX(), region.getMinY());
        include(region.getMaxX(), region.getMaxY());
    }

    public void set(ImageRegionStruct region) {
        clear();
        include(region);
    }

    // Accessible \\

    public boolean isEmpty() {
        return empty;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getWidth() {
        return maxX - minX + 1;
    }

    public int getHeight() {
        return maxY - minY + 1;
    }
}
