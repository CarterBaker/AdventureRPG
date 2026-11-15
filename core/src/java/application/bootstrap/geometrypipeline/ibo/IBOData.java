package application.bootstrap.geometrypipeline.ibo;

import engine.root.DataPackage;
import engine.root.EngineSetting;

public class IBOData extends DataPackage {

    /*
     * Immutable GPU index buffer payload. Holds the raw OpenGL buffer handle,
     * the index count uploaded during mesh assembly and the GL type of one
     * index: unsigned short unless the mesh needs more vertices than it reaches. Owned by IBOHandle or
     * IBOInstance for its lifetime.
     */

    // Internal
    private final int indexHandle;
    private final int indexCount;
    private final int indexType;

    // Constructor \\

    public IBOData(int indexHandle, int indexCount) {
        this(indexHandle, indexCount, EngineSetting.GL_UNSIGNED_SHORT);
    }

    public IBOData(int indexHandle, int indexCount, int indexType) {

        // Internal
        this.indexHandle = indexHandle;
        this.indexCount = indexCount;
        this.indexType = indexType;
    }

    // Accessible \\

    public int getIndexHandle() {
        return indexHandle;
    }

    public int getIndexCount() {
        return indexCount;
    }

    public int getIndexType() {
        return indexType;
    }
}