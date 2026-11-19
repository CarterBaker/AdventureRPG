package application.bootstrap.itempipeline.itemmodelmanager;

import application.bootstrap.geometrypipeline.mesh.MeshData;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.geometrypipeline.modelmanager.ModelManager;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import engine.root.EngineSetting;
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
     * model is only ever reused once its last frame has been drawn. A painted
     * item's mesh is laid out as a painted mesh is, with its own UVs, so it
     * draws with the painted twin of whatever item material it was asked for,
     * as a renderer picks a shader's permutation by the vertex layout it is
     * given: the engine's twins are paired by name, and a viewer registers its
     * own.
     */

    // Internal
    private ModelManager modelManager;
    private MaterialManager materialManager;

    // Pool
    private Int2ObjectOpenHashMap<ObjectArrayList<ModelInstance>> materialID2Models;
    private Int2IntOpenHashMap materialID2Cursor;

    // Twins — each item material's twin for painted meshes, the engine's paired once on first use
    private Int2IntOpenHashMap materialID2PaintedMaterialID;
    private boolean twinsResolved;

    // Base \\

    @Override
    protected void create() {

        // Pool
        this.materialID2Models = new Int2ObjectOpenHashMap<>();
        this.materialID2Cursor = new Int2IntOpenHashMap();

        // Twins
        this.materialID2PaintedMaterialID = new Int2IntOpenHashMap();
        this.materialID2PaintedMaterialID.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);
    }

    @Override
    protected void get() {
        this.modelManager = get(ModelManager.class);
        this.materialManager = get(MaterialManager.class);
    }

    @Override
    protected void render() {
        materialID2Cursor.clear();
    }

    // Acquire \\

    public ModelInstance acquireModel(MeshHandle meshHandle, int materialID) {
        return acquireModel(meshHandle.getMeshData(), materialID);
    }

    public ModelInstance acquireModel(MeshData meshData, int materialID) {

        int drawnID = resolveMaterial(materialID, meshData);
        ObjectArrayList<ModelInstance> models = materialID2Models.get(drawnID);

        if (models == null) {
            models = new ObjectArrayList<>();
            materialID2Models.put(drawnID, models);
        }

        int cursor = materialID2Cursor.get(drawnID);

        if (cursor == models.size())
            models.add(modelManager.createModel(meshData, drawnID));

        ModelInstance model = models.get(cursor);
        model.updateMeshData(meshData);
        materialID2Cursor.put(drawnID, cursor + 1);

        return model;
    }

    // Twins \\

    // The material a mesh draws with: the one asked for, or its painted twin for a painted mesh
    public int resolveMaterial(int materialID, MeshData meshData) {

        if (meshData.getVertStride() != EngineSetting.PAINTED_ITEM_VERTEX_STRIDE)
            return materialID;

        resolveTwins();

        int paintedID = materialID2PaintedMaterialID.get(materialID);

        if (paintedID == EngineSetting.INDEX_NOT_FOUND)
            throwException("Material ID " + materialID + " has no painted twin to draw a painted item with.");

        return paintedID;
    }

    // A material of a viewer's own given the twin its painted items draw with
    public void registerPaintedTwin(int materialID, int paintedMaterialID) {
        materialID2PaintedMaterialID.put(materialID, paintedMaterialID);
    }

    private void resolveTwins() {

        if (twinsResolved)
            return;

        String[][] twins = EngineSetting.ITEM_PAINTED_MATERIAL_TWINS;

        for (int i = 0; i < twins.length; i++)
            registerPaintedTwin(
                    materialManager.getMaterialIDFromMaterialName(twins[i][0]),
                    materialManager.getMaterialIDFromMaterialName(twins[i][1]));

        this.twinsResolved = true;
    }
}
