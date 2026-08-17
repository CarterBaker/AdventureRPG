package application.bootstrap.geometrypipeline.mesh;

import application.bootstrap.geometrypipeline.ibo.IBOData;
import application.bootstrap.geometrypipeline.vao.VAOData;
import application.bootstrap.geometrypipeline.vbo.VBOData;
import engine.root.DataPackage;
import engine.util.mathematics.vectors.Vector3;

public class MeshData extends DataPackage {

    /*
     * Flat aggregation of VAO, VBO, and IBO data for one GPU-resident mesh.
     * Provides direct convenience accessors for all render-critical handles
     * and counts without requiring callers to reach through each sub-data
     * object. boundsMin/boundsMax are this mesh's own raw, unscaled vertex
     * position extent in its authored bind-pose model space — read directly
     * off the assembled vertex floats at bootstrap, never off the GPU
     * buffer. width/height/length are derived from those bounds once here
     * so callers needing an entity-scale ratio never recompute them. Meshes
     * with no meaningful bounds (runtime dynamic geometry, referenced VBOs
     * with no quad entries) carry zero bounds — they are never scaled to an
     * entity's size and have no use for them.
     */

    // Internal
    private final VAOData vaoData;
    private final VBOData vboData;
    private final IBOData iboData;

    // Bounds
    private final Vector3 boundsMin;
    private final Vector3 boundsMax;
    private final float width;
    private final float height;
    private final float length;

    // Constructor \\

    public MeshData(VAOData vaoData, VBOData vboData, IBOData iboData, Vector3 boundsMin, Vector3 boundsMax) {

        // Internal
        this.vaoData = vaoData;
        this.vboData = vboData;
        this.iboData = iboData;

        // Bounds
        this.boundsMin = boundsMin;
        this.boundsMax = boundsMax;
        this.width = boundsMax.x - boundsMin.x;
        this.height = boundsMax.y - boundsMin.y;
        this.length = boundsMax.z - boundsMin.z;
    }

    // Accessible \\

    public VAOData getVAOData() {
        return vaoData;
    }

    public VBOData getVBOData() {
        return vboData;
    }

    public IBOData getIBOData() {
        return iboData;
    }

    public int getAttributeHandle() {
        return vaoData.getAttributeHandle();
    }

    public int getVertStride() {
        return vaoData.getVertStride();
    }

    public int getVertexHandle() {
        return vboData.getVertexHandle();
    }

    public int getVertexCount() {
        return vboData.getVertexCount();
    }

    public int getIndexHandle() {
        return iboData.getIndexHandle();
    }

    public int getIndexCount() {
        return iboData.getIndexCount();
    }

    public Vector3 getBoundsMin() {
        return boundsMin;
    }

    public Vector3 getBoundsMax() {
        return boundsMax;
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