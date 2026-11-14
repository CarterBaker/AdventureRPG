package application.bootstrap.mappipeline.map;

import engine.root.StructPackage;

public class MapDrawStruct extends StructPackage {

    /*
     * One quad of a map view to draw this frame: the tile texture it reads,
     * the window rectangle it covers in pixels with y up, and the part of the
     * texture shown across it, in texture space with v running north to south.
     * A tile still generating is stood in for by the matching part of a
     * coarser tile, so the rectangle always belongs to the tile asked for.
     * The world region is the same rectangle as a share of the whole world,
     * unwrapped and with v running north to south, which the day and night
     * and weather overlays are read across.
     */

    // Texture
    private int texture;

    // Rectangle
    private float left;
    private float bottom;
    private float right;
    private float top;

    // Texture Region
    private float u0;
    private float v0;
    private float u1;
    private float v1;

    // World Region
    private float worldU0;
    private float worldV0;
    private float worldU1;
    private float worldV1;

    // Management \\

    public void set(int texture, float left, float bottom, float right, float top) {
        this.texture = texture;
        this.left = left;
        this.bottom = bottom;
        this.right = right;
        this.top = top;
        setRegion(0f, 0f, 1f, 1f);
    }

    public void setRegion(float u0, float v0, float u1, float v1) {
        this.u0 = u0;
        this.v0 = v0;
        this.u1 = u1;
        this.v1 = v1;
    }

    public void setWorldRegion(float worldU0, float worldV0, float worldU1, float worldV1) {
        this.worldU0 = worldU0;
        this.worldV0 = worldV0;
        this.worldU1 = worldU1;
        this.worldV1 = worldV1;
    }

    // Accessible \\

    public int getTexture() {
        return texture;
    }

    public float getLeft() {
        return left;
    }

    public float getBottom() {
        return bottom;
    }

    public float getRight() {
        return right;
    }

    public float getTop() {
        return top;
    }

    public float getU0() {
        return u0;
    }

    public float getV0() {
        return v0;
    }

    public float getU1() {
        return u1;
    }

    public float getV1() {
        return v1;
    }

    public float getWorldU0() {
        return worldU0;
    }

    public float getWorldV0() {
        return worldV0;
    }

    public float getWorldU1() {
        return worldU1;
    }

    public float getWorldV1() {
        return worldV1;
    }
}
