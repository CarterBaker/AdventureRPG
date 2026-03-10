package com.internal.bootstrap.worldpipeline.worlditemrendersystem;

import com.internal.bootstrap.geometrypipeline.mesh.MeshHandle;
import com.internal.bootstrap.shaderpipeline.material.MaterialInstance;
import com.internal.bootstrap.worldpipeline.worlditem.WorldItemInstance;
import com.internal.core.engine.HandlePackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

public class ItemInstanceBuffer extends HandlePackage {

    private static final int FLOATS_PER_INSTANCE = 6;
    private static final int INITIAL_CAPACITY = 64;

    private MeshHandle meshHandle;
    private MaterialInstance material;
    private int compositeVAO;
    private int instanceVBO;
    private int indexCount;

    private float[] instanceData;
    private ObjectArrayList<WorldItemInstance> trackedInstances;
    private FloatBuffer uploadBuffer;
    private int instanceCount;
    private int maxInstances;
    private boolean dirty;

    // Constructor \\

    public void constructor(MeshHandle meshHandle, MaterialInstance material) {
        this.meshHandle = meshHandle;
        this.material = material;
        this.indexCount = meshHandle.getIBOHandle().getIBOStruct().indexCount;
        this.maxInstances = INITIAL_CAPACITY;
        this.instanceData = new float[maxInstances * FLOATS_PER_INSTANCE];
        this.trackedInstances = new ObjectArrayList<>();
        this.instanceCount = 0;
        this.dirty = false;

        this.instanceVBO = GLSLUtility.createDynamicInstanceVBO(maxInstances);
        this.compositeVAO = GLSLUtility.createInstancedVAO(
                meshHandle.getVBOHandle().getVBOStruct().vertexHandle,
                meshHandle.getVAOInstance().getVAOStruct().attrSizes,
                meshHandle.getIBOHandle().getIBOStruct().indexHandle,
                instanceVBO);

        this.uploadBuffer = ByteBuffer
                .allocateDirect(maxInstances * FLOATS_PER_INSTANCE * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
    }

    // Management \\

    public void addInstance(WorldItemInstance instance,
            int chunkX, int chunkZ,
            float localX, float localY, float localZ,
            int orientation) {
        if (instanceCount >= maxInstances)
            grow();
        int offset = instanceCount * FLOATS_PER_INSTANCE;
        instanceData[offset] = chunkX;
        instanceData[offset + 1] = chunkZ;
        instanceData[offset + 2] = localX;
        instanceData[offset + 3] = localZ;
        instanceData[offset + 4] = localY;
        instanceData[offset + 5] = orientation;
        trackedInstances.add(instance);
        instanceCount++;
        dirty = true;
    }

    public void removeInstance(WorldItemInstance instance) {
        int index = trackedInstances.indexOf(instance);
        if (index == -1)
            return;
        int last = instanceCount - 1;
        if (index != last) {
            int src = last * FLOATS_PER_INSTANCE;
            int dst = index * FLOATS_PER_INSTANCE;
            System.arraycopy(instanceData, src, instanceData, dst, FLOATS_PER_INSTANCE);
            trackedInstances.set(index, trackedInstances.get(last));
        }
        trackedInstances.remove(last);
        instanceCount--;
        dirty = true;
    }

    public void clear() {
        instanceCount = 0;
        trackedInstances.clear();
        dirty = true;
    }

    // Draw \\

    public void draw() {
        if (instanceCount == 0)
            return;
        if (dirty)
            upload();
        GLSLUtility.drawElementsInstanced(compositeVAO, indexCount, instanceCount);
    }

    // Upload \\

    private void upload() {
        int floatCount = instanceCount * FLOATS_PER_INSTANCE;
        uploadBuffer.clear();
        uploadBuffer.put(instanceData, 0, floatCount);
        uploadBuffer.flip();
        GLSLUtility.updateInstanceVBO(instanceVBO, uploadBuffer, floatCount);
        dirty = false;
    }

    // Grow \\

    private void grow() {
        maxInstances *= 2;
        instanceData = Arrays.copyOf(instanceData, maxInstances * FLOATS_PER_INSTANCE);
        GLSLUtility.deleteBuffer(instanceVBO);
        GLSLUtility.deleteVAO(compositeVAO);
        instanceVBO = GLSLUtility.createDynamicInstanceVBO(maxInstances);
        compositeVAO = GLSLUtility.createInstancedVAO(
                meshHandle.getVBOHandle().getVBOStruct().vertexHandle,
                meshHandle.getVAOInstance().getVAOStruct().attrSizes,
                meshHandle.getIBOHandle().getIBOStruct().indexHandle,
                instanceVBO);
        uploadBuffer = ByteBuffer
                .allocateDirect(maxInstances * FLOATS_PER_INSTANCE * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        dirty = true;
    }

    // Accessible \\

    public MeshHandle getMeshHandle() {
        return meshHandle;
    }

    public MaterialInstance getMaterial() {
        return material;
    }

    public boolean isEmpty() {
        return instanceCount == 0;
    }

    public int getInstanceCount() {
        return instanceCount;
    }

    // Dispose \\

    public void dispose() {
        GLSLUtility.deleteBuffer(instanceVBO);
        GLSLUtility.deleteVAO(compositeVAO);
    }
}