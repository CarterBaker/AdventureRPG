package application.bootstrap.geometrypipeline.subvoxel;

import engine.root.StructPackage;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class SubVoxelQuadListStruct extends StructPackage {

    /*
     * The merged faces one sub-voxel mesh pass produced, in sub-voxels and
     * free of any vertex format: each quad's face, numbered as the item
     * shader numbers faces, the plane it lies on along that face's axis, its
     * corner on the plane's two other axes in the mesher's u, v order, its
     * extent along each, and its zero-based part. Whoever draws the quads
     * writes them in its own format. Reused in place by its owner.
     */

    // Layout
    private static final int FACE = 0;
    private static final int PLANE = 1;
    private static final int U = 2;
    private static final int V = 3;
    private static final int WIDTH = 4;
    private static final int HEIGHT = 5;
    private static final int PART = 6;
    private static final int STRIDE = 7;

    // Quads
    private final IntArrayList quads;

    // Constructor \\

    public SubVoxelQuadListStruct() {
        this.quads = new IntArrayList();
    }

    // Management \\

    public void add(int face, int plane, int u, int v, int width, int height, int partIndex) {
        quads.add(face);
        quads.add(plane);
        quads.add(u);
        quads.add(v);
        quads.add(width);
        quads.add(height);
        quads.add(partIndex);
    }

    public void clear() {
        quads.clear();
    }

    // Accessible \\

    public int size() {
        return quads.size() / STRIDE;
    }

    public boolean isEmpty() {
        return quads.isEmpty();
    }

    public int getFace(int quad) {
        return quads.getInt(quad * STRIDE + FACE);
    }

    public int getPlane(int quad) {
        return quads.getInt(quad * STRIDE + PLANE);
    }

    public int getU(int quad) {
        return quads.getInt(quad * STRIDE + U);
    }

    public int getV(int quad) {
        return quads.getInt(quad * STRIDE + V);
    }

    public int getWidth(int quad) {
        return quads.getInt(quad * STRIDE + WIDTH);
    }

    public int getHeight(int quad) {
        return quads.getInt(quad * STRIDE + HEIGHT);
    }

    public int getPart(int quad) {
        return quads.getInt(quad * STRIDE + PART);
    }
}
