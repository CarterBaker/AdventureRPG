package com.internal.bootstrap.geometrypipeline.compositebuffer;

import com.internal.bootstrap.geometrypipeline.mesh.MeshHandle;
import com.internal.core.engine.DataPackage;
import com.internal.core.engine.settings.EngineSetting;

public class CompositeBufferData extends DataPackage {

    /*
     * Holds all mutable state for one instanced draw — GPU handles, mesh
     * references, CPU-side instance data, version tracking, and realloc flag.
     * All mutation is driven by CompositeBufferInstance.
     */

    // GPU Handles
    private int compositeVAO;
    private int instanceVBO;

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

    public int getCompositeVAO() {
        return compositeVAO;
    }

    public void setCompositeVAO(int compositeVAO) {
        this.compositeVAO = compositeVAO;
    }

    public int getInstanceVBO() {
        return instanceVBO;
    }

    public void setInstanceVBO(int instanceVBO) {
        this.instanceVBO = instanceVBO;
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