package application.bootstrap.geometrypipeline.vbo;

import engine.root.DataPackage;

public class VBOData extends DataPackage {

    /*
     * Immutable GPU vertex buffer payload. Holds the raw OpenGL buffer handle,
     * vertex count, and the model-space bounding box of the uploaded vertex
     * data. Position is assumed to occupy the first 3 floats of every vertex,
     * matching engine convention across every geometry branch and builder.
     * width/height/length are derived once here so any caller needing to
     * scale this mesh against a target size never has to re-walk the vertex
     * data. Owned by VBOHandle or VBOInstance for its lifetime.
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

        if (vertStride < 3)
            throwException("Vertex stride must be at least 3 to hold a position — got " + vertStride);

        // Internal
        this.vertexHandle = vertexHandle;
        this.vertexCount = vertices.length / vertStride;

        float boundsMinX = Float.MAX_VALUE;
        float boundsMinY = Float.MAX_VALUE;
        float boundsMinZ = Float.MAX_VALUE;
        float boundsMaxX = -Float.MAX_VALUE;
        float boundsMaxY = -Float.MAX_VALUE;
        float boundsMaxZ = -Float.MAX_VALUE;

        for (int i = 0; i < vertices.length; i += vertStride) {

            float x = vertices[i];
            float y = vertices[i + 1];
            float z = vertices[i + 2];

            if (x < boundsMinX)
                boundsMinX = x;
            if (y < boundsMinY)
                boundsMinY = y;
            if (z < boundsMinZ)
                boundsMinZ = z;

            if (x > boundsMaxX)
                boundsMaxX = x;
            if (y > boundsMaxY)
                boundsMaxY = y;
            if (z > boundsMaxZ)
                boundsMaxZ = z;
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