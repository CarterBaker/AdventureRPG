package application.bootstrap.geometrypipeline.vao;

import java.util.Arrays;

import engine.root.DataPackage;
import engine.root.EngineSetting;

public class VAOData extends DataPackage {

    /*
     * Immutable VAO layout descriptor. Holds the OpenGL vertex array handle and,
     * per attribute, its component count, its GL component type and whether an
     * integer type is normalised on its way to the shader; a layout given no
     * types is all floats. The stride is counted in 4-byte words, so it is the
     * float count of a float layout and the int count of a packed one. Created
     * either as a zero-handle layout template inside VAOHandle, or as a fully
     * GPU-backed instance inside VAOInstance.
     */

    // Internal
    private final int attributeHandle;
    private final int vertStride;
    private final int strideBytes;
    private final int[] attrSizes;
    private final int[] attrTypes;
    private final boolean[] attrNormalized;
    private final int[] attrOffsets;

    // Constructor \\

    public VAOData(int attributeHandle, int[] attrSizes) {
        this(attributeHandle, attrSizes, createFloatTypes(attrSizes.length), new boolean[attrSizes.length]);
    }

    public VAOData(int attributeHandle, int[] attrSizes, int[] attrTypes, boolean[] attrNormalized) {

        // Internal
        this.attributeHandle = attributeHandle;
        this.attrSizes = attrSizes;
        this.attrTypes = attrTypes;
        this.attrNormalized = attrNormalized;
        this.attrOffsets = new int[attrSizes.length];

        int bytes = 0;

        for (int i = 0; i < attrSizes.length; i++) {
            attrOffsets[i] = bytes;
            bytes += resolveAttrBytes(attrSizes[i], attrTypes[i]);
        }

        this.strideBytes = bytes;
        this.vertStride = bytes / Float.BYTES;
    }

    private static int[] createFloatTypes(int count) {

        int[] types = new int[count];
        Arrays.fill(types, EngineSetting.GL_FLOAT);

        return types;
    }

    // A packed type holds all its components in one word; every other type holds one component per element
    private static int resolveAttrBytes(int size, int type) {

        if (type == EngineSetting.GL_INT_2_10_10_10_REV)
            return Integer.BYTES;

        if (type == EngineSetting.GL_UNSIGNED_BYTE)
            return size;

        if (type == EngineSetting.GL_UNSIGNED_SHORT)
            return size * Short.BYTES;

        return size * Float.BYTES;
    }

    // Accessible \\

    public int getAttributeHandle() {
        return attributeHandle;
    }

    public int getVertStride() {
        return vertStride;
    }

    public int getStrideBytes() {
        return strideBytes;
    }

    public int[] getAttrSizes() {
        return attrSizes;
    }

    public int[] getAttrTypes() {
        return attrTypes;
    }

    public boolean[] getAttrNormalized() {
        return attrNormalized;
    }

    public int[] getAttrOffsets() {
        return attrOffsets;
    }
}
