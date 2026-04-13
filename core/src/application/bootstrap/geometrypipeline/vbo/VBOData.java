package application.bootstrap.geometrypipeline.vbo;

import application.core.engine.DataPackage;

public class VBOData extends DataPackage {

    /*
     * Immutable GPU vertex buffer payload. Holds the raw OpenGL buffer handle
     * and vertex count uploaded during mesh assembly. Owned by VBOHandle or
     * VBOInstance for its lifetime.
     */

    // Internal
    private final int vertexHandle;
    private final int vertexCount;

    // Constructor \\

    public VBOData(int vertexHandle, int vertexCount) {

        // Internal
        this.vertexHandle = vertexHandle;
        this.vertexCount = vertexCount;
    }

    // Accessible \\

    public int getVertexHandle() {
        return vertexHandle;
    }

    public int getVertexCount() {
        return vertexCount;
    }
}