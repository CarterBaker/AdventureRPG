package com.internal.bootstrap.geometrypipeline.compositebuffer;

import com.internal.bootstrap.geometrypipeline.mesh.MeshHandle;
import com.internal.core.engine.InstancePackage;

import java.util.Arrays;

/*
 * Holds the GPU handles and CPU-side instance data for one instanced draw.
 * Owns no GL calls — all GPU operations go through CompositeBufferManager.
 * Grows the CPU array automatically; CompositeBufferManager detects the growth
 * via needsGpuRealloc() and recreates the VBO/VAO to match.
 *
 * Instance layout is caller-defined via instanceAttrSizes passed to
 * CompositeBufferManager.constructor(), e.g. [4, 2] for vec4 + vec2.
 */
public class CompositeBufferInstance extends InstancePackage {

    private static final int INITIAL_CAPACITY = 64;

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

    // State flags
    private boolean dirty;
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

        this.maxInstances = INITIAL_CAPACITY;
        this.instanceData = new float[maxInstances * floatsPerInstance];
        this.instanceCount = 0;
        this.dirty = false;
        this.needsGpuRealloc = false;
    }

    public void setCompositeVAO(int vao) {
        this.compositeVAO = vao;
    }

    public void setInstanceVBO(int vbo) {
        this.instanceVBO = vbo;
    }

    public void clearDirty() {
        this.dirty = false;
    }

    public void clearNeedsGpuRealloc() {
        this.needsGpuRealloc = false;
    }

    // Instance Management \\

    /*
     * Appends one instance. Returns the slot index — store it on the owning
     * instance (e.g. WorldItemInstance) to enable O(1) removal later.
     */
    public int addInstance(float[] data) {
        if (instanceCount >= maxInstances)
            grow();
        System.arraycopy(data, 0, instanceData, instanceCount * floatsPerInstance, floatsPerInstance);
        dirty = true;
        return instanceCount++;
    }

    /*
     * Overwrites an existing slot in-place. No reallocation.
     */
    public void updateInstance(int index, float[] data) {
        System.arraycopy(data, 0, instanceData, index * floatsPerInstance, floatsPerInstance);
        dirty = true;
    }

    /*
     * Swap-removes by slot index. O(1).
     * Returns the slot that was swapped into the vacated position so the
     * caller can update its owner's stored slot index.
     * If the removed slot was already the last, returns index unchanged.
     */
    public int removeInstance(int index) {
        int last = instanceCount - 1;
        if (index != last)
            System.arraycopy(instanceData, last * floatsPerInstance,
                    instanceData, index * floatsPerInstance, floatsPerInstance);
        instanceCount--;
        dirty = true;
        return last;
    }

    public void clear() {
        instanceCount = 0;
        dirty = true;
    }

    // Grow \\

    private void grow() {
        maxInstances *= 2;
        instanceData = Arrays.copyOf(instanceData, maxInstances * floatsPerInstance);
        needsGpuRealloc = true;
    }

    // Accessible \\

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

    public boolean isDirty() {
        return dirty;
    }

    public boolean needsGpuRealloc() {
        return needsGpuRealloc;
    }

    public boolean isEmpty() {
        return instanceCount == 0;
    }
}