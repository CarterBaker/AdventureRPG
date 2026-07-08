package application.bootstrap.weatherpipeline.cloudbuffer;

import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import engine.root.DataPackage;
import engine.root.EngineSetting;

public class CloudBufferData extends DataPackage {

    /*
     * Holds all mutable state for one instanced cloud draw — the shared
     * GPU instance buffer handle, the shared cloud card mesh reference,
     * the per-instance attribute layout, and the CPU-side instance float
     * array. All mutation is driven by CloudBufferInstance. No cpu/uploaded
     * version pair — every instance here is re-submitted in full every
     * single frame by CloudRenderSystem from OverheadManager's live cell
     * grid, so "is this stale" is never a meaningful question — see
     * CloudBufferManager's own doc comment.
     */

    // GPU Handle — shareable across every window's context
    private int instanceVBO;

    // Mesh — the single shared cloud card mesh, identical for every
    // cloud archetype
    private final MeshHandle meshHandle;

    // Instance Layout
    private final int[] instanceAttrSizes;
    private final int floatsPerInstance;

    // Instance Data
    private float[] instanceData;
    private int instanceCount;
    private int maxInstances;

    // Realloc
    private boolean needsGpuRealloc;

    // Constructor \\

    public CloudBufferData(MeshHandle meshHandle, int[] instanceAttrSizes) {

        // Mesh
        this.meshHandle = meshHandle;
        this.instanceAttrSizes = instanceAttrSizes;

        int floats = 0;
        for (int size : instanceAttrSizes)
            floats += size;
        this.floatsPerInstance = floats;

        // Instance Data
        this.maxInstances = EngineSetting.CLOUD_INSTANCE_INITIAL_CAPACITY;
        this.instanceData = new float[maxInstances * floatsPerInstance];
        this.instanceCount = 0;

        // Realloc
        this.needsGpuRealloc = false;
    }

    // Accessible \\

    public int getInstanceVBO() {
        return instanceVBO;
    }

    public void setInstanceVBO(int instanceVBO) {
        this.instanceVBO = instanceVBO;
    }

    public MeshHandle getMeshHandle() {
        return meshHandle;
    }

    public int[] getInstanceAttrSizes() {
        return instanceAttrSizes;
    }

    public int getFloatsPerInstance() {
        return floatsPerInstance;
    }

    public float[] getInstanceData() {
        return instanceData;
    }

    public void setInstanceData(float[] instanceData) {
        this.instanceData = instanceData;
    }

    public int getInstanceCount() {
        return instanceCount;
    }

    public void setInstanceCount(int instanceCount) {
        this.instanceCount = instanceCount;
    }

    public int getMaxInstances() {
        return maxInstances;
    }

    public void setMaxInstances(int maxInstances) {
        this.maxInstances = maxInstances;
    }

    public boolean isNeedsGpuRealloc() {
        return needsGpuRealloc;
    }

    public void setNeedsGpuRealloc(boolean needsGpuRealloc) {
        this.needsGpuRealloc = needsGpuRealloc;
    }
}