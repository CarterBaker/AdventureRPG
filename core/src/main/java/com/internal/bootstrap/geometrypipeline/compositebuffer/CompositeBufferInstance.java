package com.internal.bootstrap.geometrypipeline.compositebuffer;

import com.internal.bootstrap.geometrypipeline.mesh.MeshHandle;
import com.internal.bootstrap.renderpipeline.window.WindowInstance;
import com.internal.core.engine.InstancePackage;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.util.Arrays;

public class CompositeBufferInstance extends InstancePackage {

    /*
     * Runtime instanced draw unit. Holds a CompositeBufferData and drives all
     * CPU-side mutation — adding, updating, removing, and clearing instances.
     * GPU reallocation is detected by CompositeBufferManager via needsGpuRealloc().
     * Upload staleness is tracked via a version counter rather than a dirty flag.
     *
     * Composite VAOs and instance VBOs are window-specific — each window gets
     * its own GPU objects created lazily on first render.
     */

    // Internal
    private CompositeBufferData compositeBufferData;

    // Constructor \\

    public void constructor(CompositeBufferData compositeBufferData) {

        // Internal
        this.compositeBufferData = compositeBufferData;
    }

    // Accessible \\

    public CompositeBufferData getCompositeBufferData() {
        return compositeBufferData;
    }

    public boolean hasCompositeVAOForWindow(WindowInstance window) {
        return compositeBufferData.getWindow2CompositeVAO().containsKey(window.getWindowID());
    }

    public int getCompositeVAOForWindow(WindowInstance window) {
        return compositeBufferData.getWindow2CompositeVAO().get(window.getWindowID());
    }

    public void setCompositeVAOForWindow(WindowInstance window, int vao) {
        compositeBufferData.getWindow2CompositeVAO().put(window.getWindowID(), vao);
    }

    public boolean hasInstanceVBOForWindow(WindowInstance window) {
        return compositeBufferData.getWindow2InstanceVBO().containsKey(window.getWindowID());
    }

    public int getInstanceVBOForWindow(WindowInstance window) {
        return compositeBufferData.getWindow2InstanceVBO().get(window.getWindowID());
    }

    public void setInstanceVBOForWindow(WindowInstance window, int vbo) {
        compositeBufferData.getWindow2InstanceVBO().put(window.getWindowID(), vbo);
    }

    public Int2IntOpenHashMap getWindow2CompositeVAO() {
        return compositeBufferData.getWindow2CompositeVAO();
    }

    public Int2IntOpenHashMap getWindow2InstanceVBO() {
        return compositeBufferData.getWindow2InstanceVBO();
    }

    public MeshHandle getMeshHandle() {
        return compositeBufferData.getMeshHandle();
    }

    public int[] getInstanceAttrSizes() {
        return compositeBufferData.getInstanceAttrSizes();
    }

    public int getFloatsPerInstance() {
        return compositeBufferData.getFloatsPerInstance();
    }

    public int getIndexCount() {
        return compositeBufferData.getIndexCount();
    }

    public float[] getInstanceData() {
        return compositeBufferData.getInstanceData();
    }

    public int getInstanceCount() {
        return compositeBufferData.getInstanceCount();
    }

    public int getMaxInstances() {
        return compositeBufferData.getMaxInstances();
    }

    public boolean needsUpload() {
        return compositeBufferData.getCpuVersion() != compositeBufferData.getUploadedVersion();
    }

    public boolean needsGpuRealloc() {
        return compositeBufferData.isNeedsGpuRealloc();
    }

    public boolean isEmpty() {
        return compositeBufferData.getInstanceCount() == 0;
    }

    // Instance Management \\

    public int addInstance(float[] data) {

        int instanceCount = compositeBufferData.getInstanceCount();
        int floatsPerInstance = compositeBufferData.getFloatsPerInstance();

        if (instanceCount >= compositeBufferData.getMaxInstances())
            grow();

        System.arraycopy(data, 0, compositeBufferData.getInstanceData(),
                instanceCount * floatsPerInstance, floatsPerInstance);

        compositeBufferData.setCpuVersion(compositeBufferData.getCpuVersion() + 1);
        compositeBufferData.setInstanceCount(instanceCount + 1);

        return instanceCount;
    }

    public void updateInstance(int index, float[] data) {

        int floatsPerInstance = compositeBufferData.getFloatsPerInstance();

        System.arraycopy(data, 0, compositeBufferData.getInstanceData(),
                index * floatsPerInstance, floatsPerInstance);

        compositeBufferData.setCpuVersion(compositeBufferData.getCpuVersion() + 1);
    }

    public int removeInstance(int index) {

        int floatsPerInstance = compositeBufferData.getFloatsPerInstance();
        int last = compositeBufferData.getInstanceCount() - 1;

        if (index != last)
            System.arraycopy(compositeBufferData.getInstanceData(), last * floatsPerInstance,
                    compositeBufferData.getInstanceData(), index * floatsPerInstance, floatsPerInstance);

        compositeBufferData.setInstanceCount(last);
        compositeBufferData.setCpuVersion(compositeBufferData.getCpuVersion() + 1);

        return last;
    }

    public void clear() {
        compositeBufferData.setInstanceCount(0);
        compositeBufferData.setCpuVersion(compositeBufferData.getCpuVersion() + 1);
    }

    private void grow() {

        int newMax = compositeBufferData.getMaxInstances() * 2;
        float[] grown = Arrays.copyOf(
                compositeBufferData.getInstanceData(),
                newMax * compositeBufferData.getFloatsPerInstance());

        compositeBufferData.setMaxInstances(newMax);
        compositeBufferData.setInstanceData(grown);
        compositeBufferData.setNeedsGpuRealloc(true);
    }

    // Upload Tracking \\

    public void markUploaded() {
        compositeBufferData.setUploadedVersion(compositeBufferData.getCpuVersion());
    }

    public void clearNeedsGpuRealloc() {
        compositeBufferData.setNeedsGpuRealloc(false);
    }
}