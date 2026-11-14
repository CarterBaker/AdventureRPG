package application.bootstrap.geometrypipeline.vao;

import engine.root.DataPackage;

public class VAOData extends DataPackage {

    /*
     * Immutable VAO layout descriptor. Holds the OpenGL vertex array handle,
     * the total vertex stride, and per-attribute size array. Created either as
     * a zero-handle layout template inside VAOHandle, or as a fully GPU-backed
     * instance inside VAOInstance.
     */

    // Internal
    private final int attributeHandle;
    private final int vertStride;
    private final int[] attrSizes;

    // Constructor \\

    public VAOData(int attributeHandle, int[] attrSizes) {

        // Internal
        this.attributeHandle = attributeHandle;
        this.attrSizes = attrSizes;

        int stride = 0;
        for (int size : attrSizes)
            stride += size;
        this.vertStride = stride;
    }

    // Accessible \\

    public int getAttributeHandle() {
        return attributeHandle;
    }

    public int getVertStride() {
        return vertStride;
    }

    public int[] getAttrSizes() {
        return attrSizes;
    }
}