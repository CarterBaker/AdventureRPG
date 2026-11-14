package application.bootstrap.geometrypipeline.ibo;

import engine.root.DataPackage;

public class IBOData extends DataPackage {

    /*
     * Immutable GPU index buffer payload. Holds the raw OpenGL buffer handle
     * and index count uploaded during mesh assembly. Owned by IBOHandle or
     * IBOInstance for its lifetime.
     */

    // Internal
    private final int indexHandle;
    private final int indexCount;

    // Constructor \\

    public IBOData(int indexHandle, int indexCount) {

        // Internal
        this.indexHandle = indexHandle;
        this.indexCount = indexCount;
    }

    // Accessible \\

    public int getIndexHandle() {
        return indexHandle;
    }

    public int getIndexCount() {
        return indexCount;
    }
}