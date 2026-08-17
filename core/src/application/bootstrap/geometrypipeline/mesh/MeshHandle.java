package application.bootstrap.geometrypipeline.mesh;

import application.bootstrap.geometrypipeline.ibo.IBOHandle;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.vao.VAOInstance;
import application.bootstrap.geometrypipeline.vbo.VBOHandle;
import engine.root.HandlePackage;
import engine.util.mathematics.vectors.Vector3;

public class MeshHandle extends HandlePackage {

    /*
     * A fully GPU-resident static mesh assembled from JSON at bootstrap. Owned
     * exclusively by MeshManager for the engine lifetime. External systems receive
     * a ModelInstance built from this handle's MeshData — never the handle itself.
     * rigHandle is null for ordinary static meshes and non-null only for meshes
     * whose JSON declared a "rig" — the source of truth for whether this mesh's
     * vertex data carries bone index/weight attributes at all. A rigged mesh's
     * width/height/length (see MeshData) are its own raw, unscaled model-space
     * extent — divide an entity's actual size by these to get the per-axis
     * ratio that stretches this mesh onto that entity's bounding box exactly.
     */

    // Internal
    private VAOInstance vaoInstance;
    private VBOHandle vboHandle;
    private IBOHandle iboHandle;
    private MeshData meshData;

    // Rig
    private RigHandle rigHandle;

    // Constructor \\

    public void constructor(
            VAOInstance vaoInstance,
            VBOHandle vboHandle,
            IBOHandle iboHandle,
            RigHandle rigHandle,
            Vector3 boundsMin,
            Vector3 boundsMax) {

        // Internal
        this.vaoInstance = vaoInstance;
        this.vboHandle = vboHandle;
        this.iboHandle = iboHandle;
        this.meshData = new MeshData(
                vaoInstance.getVAOData(),
                vboHandle.getVBOData(),
                iboHandle.getIBOData(),
                boundsMin,
                boundsMax);

        // Rig
        this.rigHandle = rigHandle;
    }

    // Accessible \\

    public VAOInstance getVAOInstance() {
        return vaoInstance;
    }

    public VBOHandle getVBOHandle() {
        return vboHandle;
    }

    public IBOHandle getIBOHandle() {
        return iboHandle;
    }

    public MeshData getMeshData() {
        return meshData;
    }

    public int getAttributeHandle() {
        return meshData.getAttributeHandle();
    }

    public int[] getAttrSizes() {
        return meshData.getVAOData().getAttrSizes();
    }

    public int getVertexHandle() {
        return meshData.getVertexHandle();
    }

    public int getIndexHandle() {
        return meshData.getIndexHandle();
    }

    public int getIndexCount() {
        return meshData.getIndexCount();
    }

    public RigHandle getRigHandle() {
        return rigHandle;
    }

    public boolean hasRig() {
        return rigHandle != null;
    }

    public Vector3 getBoundsMin() {
        return meshData.getBoundsMin();
    }

    public Vector3 getBoundsMax() {
        return meshData.getBoundsMax();
    }

    public float getWidth() {
        return meshData.getWidth();
    }

    public float getHeight() {
        return meshData.getHeight();
    }

    public float getLength() {
        return meshData.getLength();
    }
}