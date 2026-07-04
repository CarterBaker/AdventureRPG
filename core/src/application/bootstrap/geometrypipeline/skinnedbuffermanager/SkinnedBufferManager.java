package application.bootstrap.geometrypipeline.skinnedbuffermanager;

import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedBufferData;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedBufferInstance;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class SkinnedBufferManager extends ManagerPackage {

    /*
     * Creates, grows, uploads, and disposes SkinnedBufferInstances — one per
     * distinct (rigged MeshHandle, MaterialInstance) combination. Keyed by
     * material as well as mesh because the material controls which shader
     * and textures the whole instanced draw call binds — two entities
     * sharing a mesh but wearing different skins cannot share a draw call,
     * exactly as RenderBatchStruct never merges two materials into one
     * batch. For entities to actually batch together, callers must resolve
     * a shared MaterialInstance per skin type rather than cloning one per
     * entity — cloning per entity here would make every entity its own
     * batch of one.
     *
     * The instance VBO (per-instance model matrices) and bone palette
     * texture are both ordinary buffer/texture objects — shareable across
     * GL contexts — so they live here, created once. The instanced VAO
     * wrapping them is context-local and cannot be shared, so it is
     * deliberately NOT built here — RenderSystem owns a per-window VAO
     * cache over these handles, mirroring how CompositeRenderSystem caches
     * per-window VAOs over CompositeBufferData.
     */

    // Registry
    private Object2ObjectOpenHashMap<MeshHandle, Object2ObjectOpenHashMap<MaterialInstance, SkinnedBufferInstance>> mesh2Material2SkinnedBuffer;
    private ObjectArrayList<SkinnedBufferInstance> activeBuffers;

    // Internal \\

    @Override
    protected void create() {

        // Registry
        this.mesh2Material2SkinnedBuffer = new Object2ObjectOpenHashMap<>();
        this.activeBuffers = new ObjectArrayList<>();
    }

    // Acquire \\

    public SkinnedBufferInstance getSkinnedBuffer(MeshHandle meshHandle, MaterialInstance material) {

        if (!meshHandle.hasRig())
            throwException("Cannot create a skinned buffer for a mesh with no rig. Check MeshHandle.hasRig() first.");

        Object2ObjectOpenHashMap<MaterialInstance, SkinnedBufferInstance> material2SkinnedBuffer = mesh2Material2SkinnedBuffer
                .get(meshHandle);

        if (material2SkinnedBuffer == null) {
            material2SkinnedBuffer = new Object2ObjectOpenHashMap<>();
            mesh2Material2SkinnedBuffer.put(meshHandle, material2SkinnedBuffer);
        }

        SkinnedBufferInstance existing = material2SkinnedBuffer.get(material);

        if (existing != null)
            return existing;

        SkinnedBufferData data = new SkinnedBufferData(meshHandle);
        SkinnedBufferInstance instance = create(SkinnedBufferInstance.class);
        instance.constructor(data);

        allocateGpuObjects(instance);

        material2SkinnedBuffer.put(material, instance);
        activeBuffers.add(instance);

        return instance;
    }

    // Frame Lifecycle \\

    public void clearAll() {

        Object[] elements = activeBuffers.elements();
        int count = activeBuffers.size();

        for (int i = 0; i < count; i++)
            ((SkinnedBufferInstance) elements[i]).clear();
    }

    public void upload(SkinnedBufferInstance instance) {

        if (instance.isEmpty())
            return;

        if (instance.needsGpuRealloc())
            reallocateGpuObjects(instance);

        SkinnedBufferData data = instance.getSkinnedBufferData();
        int instanceCount = data.getInstanceCount();

        GLSLUtility.uploadInstanceVBO(
                data.getInstanceVBO(),
                data.getInstanceModelData(),
                instanceCount * EngineSetting.SKINNED_INSTANCE_MODEL_FLOATS);

        GLSLUtility.uploadBonePalette(
                data.getBonePaletteTexture(),
                data.getBoneMatrixData(),
                data.getBoneCapacity(),
                instanceCount);
    }

    // GPU Allocation \\

    private void allocateGpuObjects(SkinnedBufferInstance instance) {

        SkinnedBufferData data = instance.getSkinnedBufferData();

        int vbo = GLSLUtility.createDynamicInstanceVBO(data.getMaxInstances());
        int texture = GLSLUtility.createBonePaletteTexture(data.getBoneCapacity(), data.getMaxInstances());

        data.setInstanceVBO(vbo);
        data.setBonePaletteTexture(texture);
    }

    private void reallocateGpuObjects(SkinnedBufferInstance instance) {

        SkinnedBufferData data = instance.getSkinnedBufferData();

        GLSLUtility.deleteBuffer(data.getInstanceVBO());
        GLSLUtility.deleteTexture(data.getBonePaletteTexture());

        int vbo = GLSLUtility.createDynamicInstanceVBO(data.getMaxInstances());
        int texture = GLSLUtility.createBonePaletteTexture(data.getBoneCapacity(), data.getMaxInstances());

        data.setInstanceVBO(vbo);
        data.setBonePaletteTexture(texture);

        instance.clearNeedsGpuRealloc();
    }

    // Disposal \\

    @Override
    protected void dispose() {

        Object[] elements = activeBuffers.elements();
        int count = activeBuffers.size();

        for (int i = 0; i < count; i++) {
            SkinnedBufferData data = ((SkinnedBufferInstance) elements[i]).getSkinnedBufferData();
            GLSLUtility.deleteBuffer(data.getInstanceVBO());
            GLSLUtility.deleteTexture(data.getBonePaletteTexture());
        }

        mesh2Material2SkinnedBuffer.clear();
        activeBuffers.clear();
    }
}