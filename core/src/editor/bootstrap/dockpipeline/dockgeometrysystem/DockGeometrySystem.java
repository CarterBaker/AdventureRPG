package editor.bootstrap.dockpipeline.dockgeometrysystem;

import application.bootstrap.geometrypipeline.dynamicmodel.DynamicModelHandle;
import application.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketInstance;
import application.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketState;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.geometrypipeline.modelmanager.ModelManager;
import application.bootstrap.geometrypipeline.vao.VAOHandle;
import application.bootstrap.geometrypipeline.vaomanager.VAOManager;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class DockGeometrySystem extends SystemPackage {

    /*
     * Builds and submits dock chrome geometry each frame.
     * Per-vertex color quads accumulated into a DynamicPacketInstance,
     * flushed per container window, submitted as screen calls at order 1
     * so chrome always composites on top of FBO tab content.
     * Models are destroyed at the start of each frame — geometry
     * is rebuilt every frame since rects change with splits and drags.
     */

    // Dependencies
    private VAOManager vaoManager;
    private MaterialManager materialManager;
    private ModelManager modelManager;
    private RenderManager renderManager;

    // Geometry
    private VAOHandle dockVao;
    private int dockMaterialID;
    private DynamicPacketInstance dynamicPacket;

    // Frame model tracking — destroyed each frame
    private ObjectArrayList<ModelInstance> frameModels;

    @Override
    protected void get() {
        this.vaoManager = get(VAOManager.class);
        this.materialManager = get(MaterialManager.class);
        this.modelManager = get(ModelManager.class);
        this.renderManager = get(RenderManager.class);
    }

    @Override
    protected void awake() {
        this.dockVao = vaoManager.getVAOHandleFromVAOName(EngineSetting.DOCK_CHROME_VAO);
        this.dockMaterialID = materialManager
                .getMaterialHandleFromMaterialName(EngineSetting.DOCK_CHROME_MATERIAL)
                .getMaterialData().getMaterialID();
        this.dynamicPacket = create(DynamicPacketInstance.class);
        this.dynamicPacket.constructor(dockVao);
        this.frameModels = new ObjectArrayList<>();
    }

    // Frame Lifecycle \\

    public void beginFrame() {
        destroyFrameModels();
        dynamicPacket.clear();
        dynamicPacket.tryLock();
    }

    public void pushRect(int x, int y, int width, int height, float[] color) {
        if (width <= 0 || height <= 0)
            return;

        FloatArrayList verts = new FloatArrayList(24);
        addVertex(verts, x, y, color);
        addVertex(verts, x + width, y, color);
        addVertex(verts, x + width, y + height, color);
        addVertex(verts, x, y + height, color);

        dynamicPacket.addVertices(dockMaterialID, verts);
    }

    public void flushAndSubmit(WindowInstance window) {
        dynamicPacket.setReady();

        if (dynamicPacket.getState() != DynamicPacketState.READY)
            return;

        Int2ObjectMap<ObjectArrayList<DynamicModelHandle>> buckets = dynamicPacket.getMaterialID2ModelCollection();

        for (Int2ObjectMap.Entry<ObjectArrayList<DynamicModelHandle>> entry : buckets.int2ObjectEntrySet()) {

            ObjectArrayList<DynamicModelHandle> handles = entry.getValue();

            for (int i = 0; i < handles.size(); i++) {
                DynamicModelHandle handle = handles.get(i);

                if (handle.isEmpty())
                    continue;

                ModelInstance model = modelManager.createModel(
                        handle.getVAOHandle(),
                        handle.getVertices(),
                        handle.getIndices(),
                        materialManager.cloneMaterial(entry.getIntKey()));

                frameModels.add(model);
                renderManager.pushScreenCall(model, window, 1);
            }
        }
    }

    // Vertex Building \\

    private void addVertex(FloatArrayList verts, float x, float y, float[] c) {
        verts.add(x);
        verts.add(y);
        verts.add(c[0]);
        verts.add(c[1]);
        verts.add(c[2]);
        verts.add(c[3]);
    }

    // Cleanup \\

    private void destroyFrameModels() {
        for (int i = 0; i < frameModels.size(); i++)
            modelManager.removeMesh(frameModels.get(i));
        frameModels.clear();
    }
}