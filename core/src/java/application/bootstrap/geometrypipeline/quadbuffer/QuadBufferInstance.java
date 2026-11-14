package application.bootstrap.geometrypipeline.quadbuffer;

import application.bootstrap.geometrypipeline.mesh.MeshData;
import engine.root.InstancePackage;

public class QuadBufferInstance extends InstancePackage {

    /*
     * Runtime draw unit for packed quad records. Wraps a QuadBufferData and
     * delegates to it. Created, uploaded, emptied and removed through
     * QuadBufferManager by whoever owns the draw.
     */

    // Internal
    private QuadBufferData quadBufferData;

    // Constructor \\

    public void constructor(QuadBufferData quadBufferData) {

        // Internal
        this.quadBufferData = quadBufferData;
    }

    // Accessible \\

    public QuadBufferData getQuadBufferData() {
        return quadBufferData;
    }

    public int getTextureHandle() {
        return quadBufferData.getTextureHandle();
    }

    public MeshData getMeshData() {
        return quadBufferData.getMeshData();
    }
}
