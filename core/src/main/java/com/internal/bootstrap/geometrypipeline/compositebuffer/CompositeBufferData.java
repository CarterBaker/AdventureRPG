package com.internal.bootstrap.geometrypipeline.compositebuffer;

import com.internal.bootstrap.geometrypipeline.mesh.MeshHandle;
import com.internal.core.engine.DataPackage;
import com.internal.core.engine.settings.EngineSetting;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

public class CompositeBufferData extends DataPackage {

    /*
     * Holds all mutable state for one instanced draw — GPU handles, mesh
     * references, CPU-side instance data, version tracking, and realloc flag.
     * All mutation is driven by CompositeBufferInstance.
     *
     * Composite VAOs are window-specific — each window gets its own VAO object
     * pointing to the shared mesh VBO/IBO and this buffer's instance VBO.
     * The window2CompositeVAO map grows lazily as new windows render this buffer.
     */

    // GPU Handles
    private Int2IntOpenHashMap window2CompositeVAO;
    private Int2IntOpenHashMap window2InstanceVBO;

    // Mesh State
    private MeshHandle meshHandle;
    private int[] instanceAttrSizes;
    private int floatsPerInstance;
    private int indexCount;

    // Instance Data
    private float[] instanceData;
    private int instanceCount;
    private int maxInstances;

    // Version Tracking
    private int cpuVersion;
    private int uploadedVersion;

    // Realloc
    private boolean needsGpuRealloc;

    // Constructor \\

    public CompositeBufferData(MeshHandle meshHandle, int[] instanceAttrSizes) {

        // GPU Handles
        this.window2CompositeVAO = new Int2IntOpenHashMap();
        this.window2InstanceVBO = new Int2IntOpenHashMap();

        // Mesh State
        this.meshHandle = meshHandle;
        this.instanceAttrSizes = instanceAttrSizes;
        this.indexCount = meshHandle.getIBOHandle().getIBOData().getIndexCount();

        int floats = 0;
        for (int s : instanceAttrSizes)
            floats += s;

        this.floatsPerInstance = floats;

        // Instance Data
        this.maxInstances = EngineSetting.COMPOSITE_BUFFER_INITIAL_CAPACITY;
        this.instanceData = new float[maxInstances * floatsPerInstance];
        this.instanceCount = 0;

        // Version Tracking
        this.cpuVersion = 0;
        this.uploadedVersion = 0;

        // Realloc
        this.needsGpuRealloc = false;
    }

    // Accessible \\

    public Int2IntOpenHashMap getWindow2CompositeVAO() {
        return window2CompositeVAO;
    }

    public Int2IntOpenHashMap getWindow2InstanceVBO() {
        return window2InstanceVBO;
    }

    public MeshHandle getMeshHandle() {
        return meshHandle;
    }

    public void setMeshHandle(MeshHandle meshHandle) {
        this.meshHandle = meshHandle;
    }

    public int[] getInstanceAttrSizes() {
        return instanceAttrSizes;
    }

    public void setInstanceAttrSizes(int[] instanceAttrSizes) {
        this.instanceAttrSizes = instanceAttrSizes;
    }

    public int getFloatsPerInstance() {
        return floatsPerInstance;
    }

    public void setFloatsPerInstance(int floatsPerInstance) {
        this.floatsPerInstance = floatsPerInstance;
    }

    public int getIndexCount() {
        return indexCount;
    }

    public void setIndexCount(int indexCount) {
        this.indexCount = indexCount;
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

    public int getCpuVersion() {
        return cpuVersion;
    }

    public void setCpuVersion(int cpuVersion) {
        this.cpuVersion = cpuVersion;
    }

    public int getUploadedVersion() {
        return uploadedVersion;
    }

    public void setUploadedVersion(int uploadedVersion) {
        this.uploadedVersion = uploadedVersion;
    }

    public boolean isNeedsGpuRealloc() {
        return needsGpuRealloc;
    }

    public void setNeedsGpuRealloc(boolean needsGpuRealloc) {
        this.needsGpuRealloc = needsGpuRealloc;
    }
}