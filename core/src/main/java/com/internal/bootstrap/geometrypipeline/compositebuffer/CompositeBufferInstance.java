package com.internal.bootstrap.geometrypipeline.compositebuffer;

import com.internal.bootstrap.geometrypipeline.mesh.MeshHandle;
import com.internal.core.engine.InstancePackage;
import com.internal.core.engine.settings.EngineSetting;

import java.util.Arrays;

/*
 * Holds the GPU handles and CPU-side instance data for one instanced draw.
 * Owns no GL calls — all GPU operations go through CompositeBufferManager.
 * Grows the CPU array automatically; CompositeBufferManager detects the growth
 * via needsGpuRealloc() and recreates the VBO/VAO to match.
 *
 * Instance layout is caller-defined via instanceAttrSizes passed to
 * CompositeBufferManager.constructor(), e.g. [4, 2] for vec4 + vec2.
 *
 * Upload tracking uses a version counter rather than a boolean flag.
 * cpuVersion increments on every write. uploadedVersion tracks what the
 * GPU last saw. needsUpload() is true whenever they differ — no silent
 * no-ops, no ambiguity on first upload.
 */
public class CompositeBufferInstance extends InstancePackage {

    // GPU handles — written by CompositeBufferManager
    private int compositeVAO;
    private int instanceVBO;

    // Mesh state — written by CompositeBufferManager
    private MeshHandle meshHandle;
    private int[] instanceAttrSizes;
    private int floatsPerInstance;
    private int indexCount;

    // CPU-side instance data
    private float[] instanceData;
    private int instanceCount;
    private int maxInstances;

    // Version tracking — replaces dirty flag
    private int cpuVersion;
    private int uploadedVersion;

    // Realloc flag — separate concern from upload tracking
    private boolean needsGpuRealloc;

    // Internal — called by CompositeBufferManager.constructor() \\

    public void init(MeshHandle meshHandle, int[] instanceAttrSizes) {
        this.meshHandle = meshHandle;
        this.instanceAttrSizes = instanceAttrSizes;
        this.indexCount = meshHandle.getIBOHandle().getIBOStruct().indexCount;

        int floats = 0;
        for (int s : instanceAttrSizes)
            floats += s;
        this.floatsPerInstance = floats;

        this.maxInstances = EngineSetting.COMPOSITE_BUFFER_INITIAL_CAPACITY;
        this.instanceData = new float[maxInstances * floatsPerInstance];
        this.instanceCount = 0;
        this.cpuVersion = 0;
        this.uploadedVersion = 0;
        this.needsGpuRealloc = false;
    }

    public void setCompositeVAO(int vao) {
        this.compositeVAO = vao;
    }

    public void setInstanceVBO(int vbo) {
        this.instanceVBO = vbo;
    }

    public void markUploaded() {
        this.uploadedVersion = cpuVersion;
    }

    public void clearNeedsGpuRealloc() {
        this.needsGpuRealloc = false;
    }

    // Instance Management \\

    public int addInstance(float[] data) {
        if (instanceCount >= maxInstances)
            grow();
        System.arraycopy(data, 0, instanceData, instanceCount * floatsPerInstance, floatsPerInstance);
        cpuVersion++;
        return instanceCount++;
    }

    public void updateInstance(int index, float[] data) {
        System.arraycopy(data, 0, instanceData, index * floatsPerInstance, floatsPerInstance);
        cpuVersion++;
    }

    public int removeInstance(int index) {
        int last = instanceCount - 1;
        if (index != last)
            System.arraycopy(instanceData, last * floatsPerInstance,
                    instanceData, index * floatsPerInstance, floatsPerInstance);
        instanceCount--;
        cpuVersion++;
        return last;
    }

    public void clear() {
        instanceCount = 0;
        cpuVersion++;
    }

    // Grow \\

    private void grow() {
        maxInstances *= 2;
        instanceData = Arrays.copyOf(instanceData, maxInstances * floatsPerInstance);
        needsGpuRealloc = true;
    }

    // Accessible \\

    public boolean needsUpload() {
        return cpuVersion != uploadedVersion;
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

    public int getIndexCount() {
        return indexCount;
    }

    public int getCompositeVAO() {
        return compositeVAO;
    }

    public int getInstanceVBO() {
        return instanceVBO;
    }

    public float[] getInstanceData() {
        return instanceData;
    }

    public int getInstanceCount() {
        return instanceCount;
    }

    public int getMaxInstances() {
        return maxInstances;
    }

    public boolean needsGpuRealloc() {
        return needsGpuRealloc;
    }

    public boolean isEmpty() {
        return instanceCount == 0;
    }
}