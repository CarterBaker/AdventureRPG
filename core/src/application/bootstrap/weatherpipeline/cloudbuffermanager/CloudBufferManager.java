package application.bootstrap.weatherpipeline.cloudbuffermanager;

import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.weatherpipeline.cloud.CloudHandle;
import application.bootstrap.weatherpipeline.cloudbuffer.CloudBufferData;
import application.bootstrap.weatherpipeline.cloudbuffer.CloudBufferInstance;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CloudBufferManager extends ManagerPackage {

    /*
     * Owns one CloudBufferInstance per distinct cloud archetype
     * (CloudHandle) — mirrors SkinnedBufferManager exactly, just keyed by
     * cloud type instead of (mesh, material): the instance VBO is an
     * ordinary GL buffer object, shareable across every window's context
     * (LWJGL3 windows share GL object namespaces — see
     * Lwjgl3Application.newWindow()), so it is created once, here, and
     * never per-window. The VAO wrapping it IS context-local and cannot be
     * shared, so it is deliberately NOT built here — RenderSystem owns a
     * per-window VAO cache over these handles, exactly like it already
     * does for skinned buffers.
     *
     * Every buffer here is fully rebuilt every frame by CloudRenderSystem
     * from OverheadManager's live cell grid — there is no dirty-tracking
     * or partial update path, matching SkinnedBufferData's own reasoning
     * (an OverheadManager cell's drifted position changes every frame, so
     * "is this stale" is never a meaningful question).
     */

    // Registry
    private Object2ObjectOpenHashMap<CloudHandle, CloudBufferInstance> cloudHandle2Buffer;
    private ObjectArrayList<CloudBufferInstance> activeBuffers;

    // Internal \\

    @Override
    protected void create() {
        this.cloudHandle2Buffer = new Object2ObjectOpenHashMap<>();
        this.activeBuffers = new ObjectArrayList<>();
    }

    // Acquire \\

    public CloudBufferInstance getOrCreateCloudBuffer(
            CloudHandle cloudHandle,
            MeshHandle cardMeshHandle,
            int[] instanceAttrSizes) {

        CloudBufferInstance existing = cloudHandle2Buffer.get(cloudHandle);

        if (existing != null)
            return existing;

        CloudBufferData data = new CloudBufferData(cardMeshHandle, instanceAttrSizes);
        CloudBufferInstance instance = create(CloudBufferInstance.class);
        instance.constructor(data);

        allocateGpuObjects(instance);

        cloudHandle2Buffer.put(cloudHandle, instance);
        activeBuffers.add(instance);

        return instance;
    }

    // Frame Lifecycle \\

    public void clearAll() {

        Object[] elements = activeBuffers.elements();
        int count = activeBuffers.size();

        for (int i = 0; i < count; i++)
            ((CloudBufferInstance) elements[i]).clear();
    }

    public void upload(CloudBufferInstance instance) {

        if (instance.isEmpty())
            return;

        if (instance.needsGpuRealloc())
            reallocateGpuObjects(instance);

        CloudBufferGLSLUtility.uploadInstanceVBO(
                instance.getInstanceVBO(),
                instance.getInstanceData(),
                instance.getInstanceCount() * instance.getFloatsPerInstance());
    }

    // GPU Allocation \\

    private void allocateGpuObjects(CloudBufferInstance instance) {

        int vbo = CloudBufferGLSLUtility.createDynamicInstanceVBO(
                instance.getMaxInstances(), instance.getFloatsPerInstance());

        instance.setInstanceVBO(vbo);
    }

    private void reallocateGpuObjects(CloudBufferInstance instance) {

        CloudBufferGLSLUtility.deleteBuffer(instance.getInstanceVBO());

        int vbo = CloudBufferGLSLUtility.createDynamicInstanceVBO(
                instance.getMaxInstances(), instance.getFloatsPerInstance());

        instance.setInstanceVBO(vbo);
        instance.clearNeedsGpuRealloc();
    }

    // Disposal \\

    @Override
    protected void dispose() {

        Object[] elements = activeBuffers.elements();
        int count = activeBuffers.size();

        for (int i = 0; i < count; i++)
            CloudBufferGLSLUtility.deleteBuffer(((CloudBufferInstance) elements[i]).getInstanceVBO());

        cloudHandle2Buffer.clear();
        activeBuffers.clear();
    }

    // Accessible \\

    /*
     * Live registry, keyed by cloud archetype. CloudRenderSystem reads this
     * directly each frame to register non-empty buffers into a window's
     * render queue — treat as read-only outside this class.
     */
    public Object2ObjectOpenHashMap<CloudHandle, CloudBufferInstance> getBufferMap() {
        return cloudHandle2Buffer;
    }
}