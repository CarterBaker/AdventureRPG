package application.bootstrap.geometrypipeline.vbo;

import engine.root.DataPackage;

public class VBOData extends DataPackage {

    /*
     * Immutable GPU vertex buffer payload. Holds the raw OpenGL buffer handle,
     * vertex count, and the model-space bounding box of the uploaded vertex
     * data. Bounds are computed against up to the first 3 floats of each
     * vertex — whichever position axes the layout actually declares — so a
     * flat screen-space layout (fonts, UI, sprites) with only 1 or 2 floats
     * of position simply leaves the missing axis at zero instead of failing.
     * width/height/length are only ever consumed by callers stretching a
     * full 3D mesh onto an entity's actual size (see
     * EntityInstance.updateModelScale()) — every mesh that path touches
     * always carries a full 3-float position. Owned by VBOHandle or
     * VBOInstance for its lifetime.
     */

    // Internal
    private final int vertexHandle;
    private final int vertexCount;

    // Model Bounds
    private final float minX;
    private final float minY;
    private final float minZ;
    private final float maxX;
    private final float maxY;
    private final float maxZ;
    private final float width;
    private final float height;
    private final float length;

    // Constructor \\

    public VBOData(int vertexHandle, float[] vertices, int vertStride) {

        if (vertices.length == 0)
            throwException("Cannot compute model bounds from empty vertex data.");

        if (vertStride < 1)
            throwException("Vertex stride must be at least 1 — got " + vertStride);

        // Internal
        this.vertexHandle = vertexHandle;
        this.vertexCount = vertices.length / vertStride;

        int positionAxes = Math.min(vertStride, 3);

        float boundsMinX = Float.MAX_VALUE;
        float boundsMinY = positionAxes >= 2 ? Float.MAX_VALUE : 0f;
        float boundsMinZ = positionAxes >= 3 ? Float.MAX_VALUE : 0f;
        float boundsMaxX = -Float.MAX_VALUE;
        float boundsMaxY = positionAxes >= 2 ? -Float.MAX_VALUE : 0f;
        float boundsMaxZ = positionAxes >= 3 ? -Float.MAX_VALUE : 0f;

        for (int i = 0; i < vertices.length; i += vertStride) {

            float x = vertices[i];

            if (x < boundsMinX)
                boundsMinX = x;
            if (x > boundsMaxX)
                boundsMaxX = x;

            if (positionAxes >= 2) {

                float y = vertices[i + 1];

                if (y < boundsMinY)
                    boundsMinY = y;
                if (y > boundsMaxY)
                    boundsMaxY = y;
            }

            if (positionAxes >= 3) {

                float z = vertices[i + 2];

                if (z < boundsMinZ)
                    boundsMinZ = z;
                if (z > boundsMaxZ)
                    boundsMaxZ = z;
            }
        }

        // Model Bounds
        this.minX = boundsMinX;
        this.minY = boundsMinY;
        this.minZ = boundsMinZ;
        this.maxX = boundsMaxX;
        this.maxY = boundsMaxY;
        this.maxZ = boundsMaxZ;
        this.width = boundsMaxX - boundsMinX;
        this.height = boundsMaxY - boundsMinY;
        this.length = boundsMaxZ - boundsMinZ;
    }

    // Accessible \\

    public int getVertexHandle() {
        return vertexHandle;
    }

    public int getVertexCount() {
        return vertexCount;
    }

    public float getMinX() {
        return minX;
    }

    public float getMinY() {
        return minY;
    }

    public float getMinZ() {
        return minZ;
    }

    public float getMaxX() {
        return maxX;
    }

    public float getMaxY() {
        return maxY;
    }

    public float getMaxZ() {
        return maxZ;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public float getLength() {
        return length;
    }
}