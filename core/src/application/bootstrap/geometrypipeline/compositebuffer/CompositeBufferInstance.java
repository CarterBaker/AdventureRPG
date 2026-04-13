package application.bootstrap.geometrypipeline.compositebuffer;

import java.util.Arrays;

import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import engine.root.InstancePackage;

public class CompositeBufferInstance extends InstancePackage {

    /*
     * Runtime instanced draw unit. Holds a CompositeBufferData and drives all
     * CPU-side mutation — adding, updating, removing, and clearing instances.
     * GPU reallocation is detected by CompositeBufferManager via needsGpuRealloc().
     * Upload staleness is tracked via a version counter rather than a dirty flag.
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

    public int getCompositeVAO() {
        return compositeBufferData.getCompositeVAO();
    }

    public void setCompositeVAO(int vao) {
        compositeBufferData.setCompositeVAO(vao);
    }

    public int getInstanceVBO() {
        return compositeBufferData.getInstanceVBO();
    }

    public void setInstanceVBO(int vbo) {
        compositeBufferData.setInstanceVBO(vbo);
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