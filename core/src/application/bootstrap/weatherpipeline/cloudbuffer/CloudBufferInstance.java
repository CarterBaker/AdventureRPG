package application.bootstrap.weatherpipeline.cloudbuffer;

import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import engine.root.InstancePackage;

public class CloudBufferInstance extends InstancePackage {

    /*
     * Runtime instanced cloud draw unit — one per distinct cloud archetype.
     * Holds a CloudBufferData and drives all CPU-side mutation. clear() is
     * called once at the start of each frame's rebuild pass (see
     * CloudBufferManager.clearAll(), driven by CloudRenderSystem.render());
     * addInstance() is then called once per active OverheadManager cell
     * using this cloud archetype. There is no removeInstance/updateInstance
     * — every frame starts from empty and rebuilds fully, matching
     * SkinnedBufferInstance's own reasoning. GPU upload and drawing are the
     * responsibility of CloudBufferManager and RenderSystem respectively.
     */

    // Internal
    private CloudBufferData cloudBufferData;

    // Constructor \\

    public void constructor(CloudBufferData cloudBufferData) {
        this.cloudBufferData = cloudBufferData;
    }

    // Accessible \\

    public CloudBufferData getCloudBufferData() {
        return cloudBufferData;
    }

    public int getInstanceVBO() {
        return cloudBufferData.getInstanceVBO();
    }

    public void setInstanceVBO(int instanceVBO) {
        cloudBufferData.setInstanceVBO(instanceVBO);
    }

    public MeshHandle getMeshHandle() {
        return cloudBufferData.getMeshHandle();
    }

    public int[] getInstanceAttrSizes() {
        return cloudBufferData.getInstanceAttrSizes();
    }

    public int getFloatsPerInstance() {
        return cloudBufferData.getFloatsPerInstance();
    }

    public int getIndexCount() {
        return cloudBufferData.getMeshHandle().getIndexCount();
    }

    public float[] getInstanceData() {
        return cloudBufferData.getInstanceData();
    }

    public int getInstanceCount() {
        return cloudBufferData.getInstanceCount();
    }

    public int getMaxInstances() {
        return cloudBufferData.getMaxInstances();
    }

    public boolean needsGpuRealloc() {
        return cloudBufferData.isNeedsGpuRealloc();
    }

    public boolean isEmpty() {
        return cloudBufferData.getInstanceCount() == 0;
    }

    // Instance Management \\

    public int addInstance(float[] data) {

        int instanceCount = cloudBufferData.getInstanceCount();
        int floatsPerInstance = cloudBufferData.getFloatsPerInstance();

        if (instanceCount >= cloudBufferData.getMaxInstances())
            grow();

        System.arraycopy(data, 0, cloudBufferData.getInstanceData(),
                instanceCount * floatsPerInstance, floatsPerInstance);

        cloudBufferData.setInstanceCount(instanceCount + 1);

        return instanceCount;
    }

    public void clear() {
        cloudBufferData.setInstanceCount(0);
    }

    private void grow() {

        int newMax = cloudBufferData.getMaxInstances() * 2;
        float[] grown = java.util.Arrays.copyOf(
                cloudBufferData.getInstanceData(),
                newMax * cloudBufferData.getFloatsPerInstance());

        cloudBufferData.setMaxInstances(newMax);
        cloudBufferData.setInstanceData(grown);
        cloudBufferData.setNeedsGpuRealloc(true);
    }

    // Upload Tracking \\

    public void clearNeedsGpuRealloc() {
        cloudBufferData.setNeedsGpuRealloc(false);
    }
}