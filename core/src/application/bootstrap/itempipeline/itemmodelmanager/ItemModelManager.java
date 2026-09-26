package application.bootstrap.itempipeline.itemmodelmanager;

import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.geometrypipeline.modelmanager.ModelManager;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ItemModelManager extends ManagerPackage {

    /*
     * Hands out item models for one frame's draws. Every item drawn with its
     * own transform needs its own model, since a model's uniforms are read
     * when the frame is drawn, not when it is queued. Models are pooled per
     * material and pointed at the item's mesh as they are acquired; the pool
     * rewinds in the global render pass, before any context acquires, so a
     * model is only ever reused once its last frame has been drawn.
     */

    // Internal
    private ModelManager modelManager;

    // Pool
    private Int2ObjectOpenHashMap<ObjectArrayList<ModelInstance>> materialID2Models;
    private Int2IntOpenHashMap materialID2Cursor;

    // Base \\

    @Override
    protected void create() {

        // Pool
        this.materialID2Models = new Int2ObjectOpenHashMap<>();
        this.materialID2Cursor = new Int2IntOpenHashMap();
    }

    @Override
    protected void get() {
        this.modelManager = get(ModelManager.class);
    }

    @Override
    protected void render() {
        materialID2Cursor.clear();
    }

    // Acquire \\

    public ModelInstance acquireModel(MeshHandle meshHandle, int materialID) {

        ObjectArrayList<ModelInstance> models = materialID2Models.get(materialID);

        if (models == null) {
            models = new ObjectArrayList<>();
            materialID2Models.put(materialID, models);
        }

        int cursor = materialID2Cursor.get(materialID);

        if (cursor == models.size())
            models.add(modelManager.createModel(meshHandle, materialID));

        ModelInstance model = models.get(cursor);
        model.updateMeshData(meshHandle.getMeshData());
        materialID2Cursor.put(materialID, cursor + 1);

        return model;
    }
}
