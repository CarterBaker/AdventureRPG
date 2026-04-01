package program.bootstrap.geometrypipeline.mesh;

import program.bootstrap.geometrypipeline.ibo.IBOInstance;
import program.bootstrap.geometrypipeline.vao.VAOInstance;
import program.bootstrap.geometrypipeline.vbo.VBOInstance;
import program.core.engine.InstancePackage;

public class MeshInstance extends InstancePackage {

    /*
     * A fully GPU-resident mesh created at runtime. Owns its VAO, VBO, and IBO
     * instances. Freed via MeshManager.removeMesh() by whoever created it.
     */

    // Internal
    private VAOInstance vaoInstance;
    private VBOInstance vboInstance;
    private IBOInstance iboInstance;
    private MeshData meshData;

    // Constructor \\

    public void constructor(
            VAOInstance vaoInstance,
            VBOInstance vboInstance,
            IBOInstance iboInstance) {

        // Internal
        this.vaoInstance = vaoInstance;
        this.vboInstance = vboInstance;
        this.iboInstance = iboInstance;
        this.meshData = new MeshData(
                vaoInstance.getVAOData(),
                vboInstance.getVBOData(),
                iboInstance.getIBOData());
    }

    // Accessible \\

    public VAOInstance getVAOInstance() {
        return vaoInstance;
    }

    public VBOInstance getVBOInstance() {
        return vboInstance;
    }

    public IBOInstance getIBOInstance() {
        return iboInstance;
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
}