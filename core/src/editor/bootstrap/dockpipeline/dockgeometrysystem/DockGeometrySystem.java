package editor.bootstrap.dockpipeline.dockgeometrysystem;

import application.bootstrap.geometrypipeline.dynamicmodel.DynamicModelHandle;
import application.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketInstance;
import application.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketState;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.geometrypipeline.modelmanager.ModelManager;
import application.bootstrap.geometrypipeline.vao.VAOHandle;
import application.bootstrap.geometrypipeline.vaomanager.VAOManager;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class DockGeometrySystem extends SystemPackage {

    private static final String DOCK_VAO_NAME = "DockChromeVAO";
    private static final String DOCK_MATERIAL_NAME = "DockChrome";

    private VAOManager vaoManager;
    private MaterialManager materialManager;
    private ModelManager modelManager;
    private RenderManager renderManager;

    private VAOHandle dockVao;
    private int dockMaterialID;
    private DynamicPacketInstance dynamicPacket;
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
        this.dockVao = vaoManager.getVAOHandleFromVAOName(DOCK_VAO_NAME);
        this.dockMaterialID = materialManager.getMaterialHandleFromMaterialName(DOCK_MATERIAL_NAME).getMaterialData().getMaterialID();
        this.dynamicPacket = create(DynamicPacketInstance.class);
        this.dynamicPacket.constructor(dockVao);
        this.frameModels = new ObjectArrayList<>();
    }

    public void beginFrame() {
        clearFrameModels();
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

        for (Int2ObjectMap.Entry<ObjectArrayList<DynamicModelHandle>> entry : dynamicPacket.getMaterialID2ModelCollection().int2ObjectEntrySet()) {
            int materialID = entry.getIntKey();
            ObjectArrayList<DynamicModelHandle> models = entry.getValue();

            for (int i = 0; i < models.size(); i++) {
                DynamicModelHandle model = models.get(i);
                if (model.isEmpty())
                    continue;

                MaterialInstance material = materialManager.cloneMaterial(materialID);
                ModelInstance modelInstance = modelManager.createModel(
                        model.getVAOHandle(),
                        model.getVertices(),
                        model.getIndices(),
                        material);

                frameModels.add(modelInstance);
                renderManager.pushScreenCall(modelInstance, window);
            }
        }
    }

    private void addVertex(FloatArrayList verts, float x, float y, float[] c) {
        verts.add(x);
        verts.add(y);
        verts.add(c[0]);
        verts.add(c[1]);
        verts.add(c[2]);
        verts.add(c[3]);
    }

    private void clearFrameModels() {
        for (int i = 0; i < frameModels.size(); i++)
            modelManager.removeMesh(frameModels.get(i));
        frameModels.clear();
    }
}
