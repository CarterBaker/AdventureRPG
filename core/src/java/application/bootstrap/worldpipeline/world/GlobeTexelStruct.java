package application.bootstrap.worldpipeline.world;

import engine.root.StructPackage;

public class GlobeTexelStruct extends StructPackage {

    /*
     * Where a direction falls on a globe world's images: the image face, in
     * cube map order, and the continuous texel position on it, texel centres
     * at half steps, x across the face's s axis and y down its t axis.
     */

    // Texel
    private int face;
    private double x;
    private double y;

    // Accessible \\

    public void set(int face, double x, double y) {
        this.face = face;
        this.x = x;
        this.y = y;
    }

    public int getFace() {
        return face;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
}
