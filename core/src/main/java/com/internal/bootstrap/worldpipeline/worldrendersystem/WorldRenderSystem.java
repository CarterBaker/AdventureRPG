package com.internal.bootstrap.worldpipeline.worldrendersystem;

import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicModelHandle;
import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicPacketInstance;
import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicPacketState;
import com.internal.bootstrap.geometrypipeline.modelmanager.ModelHandle;
import com.internal.bootstrap.geometrypipeline.modelmanager.ModelManager;
import com.internal.bootstrap.renderpipeline.rendersystem.RenderSystem;
import com.internal.bootstrap.shaderpipeline.materialmanager.MaterialHandle;
import com.internal.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOHandle;
import com.internal.core.engine.SystemPackage;
import com.internal.core.engine.settings.EngineSetting;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WorldRenderSystem extends SystemPackage {

    // Internal
    private MaterialManager materialManager;
    private ModelManager modelManager;
    private RenderSystem renderSystem;

    // Coordinate -> List of ModelHandles for that chunk
    private Long2ObjectOpenHashMap<ObjectArrayList<ModelHandle>> coordinate2Models;

    // Internal \\

    @Override
    protected void create() {
        // Internal
        this.coordinate2Models = new Long2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        // Internal
        this.materialManager = get(MaterialManager.class);
        this.modelManager = get(ModelManager.class);
        this.renderSystem = get(RenderSystem.class);
    }

    @Override
    protected void update() {

        // Push all models to render system each frame
        for (ObjectArrayList<ModelHandle> modelList : coordinate2Models.values())
            for (ModelHandle model : modelList)
                renderSystem.pushRenderCall(model, 0);
    }

    // World Render System \\

    public void moveWorldInstance(long coordinate, UBOHandle uboHandle) {

        // Get the model list for this coordinate
        ObjectArrayList<ModelHandle> modelList = coordinate2Models.get(coordinate);

        if (modelList == null)
            return;

        // Iterate through all models and update their UBO
        for (int i = 0; i < modelList.size(); i++) {
            ModelHandle model = modelList.get(i);
            MaterialHandle material = model.getMaterial();
            material.setUBO(EngineSetting.GRID_COORDINATE_UBO, uboHandle);
        }
    }

    public boolean renderWorldInstance(WorldRenderInstance worldRenderInstance) {

        if (worldRenderInstance.getGridSlotHandle() == null)
            return false;

        long coordinate = worldRenderInstance.getCoordinate();

        // Check if chunk is already rendered and un-render it first
        if (coordinate2Models.containsKey(coordinate))
            removeWorldInstance(coordinate);

        DynamicPacketInstance dynamicPacket = worldRenderInstance.getDynamicPacketInstance();

        if (dynamicPacket.getState() != DynamicPacketState.READY)
            return false;

        // Create list to store all models for this chunk
        ObjectArrayList<ModelHandle> modelList = new ObjectArrayList<>();

        // Iterate through each material in the dynamic packet
        var materialCollection = dynamicPacket.getMaterialID2ModelCollection();

        for (var entry : materialCollection.int2ObjectEntrySet()) {

            int materialID = entry.getIntKey();
            ObjectArrayList<DynamicModelHandle> dynamicModels = entry.getValue();

            // Process each dynamic model for this material
            for (DynamicModelHandle dynamicModel : dynamicModels) {

                if (dynamicModel.isEmpty())
                    continue;

                // Clone the material to get a unique instance
                MaterialHandle clonedMaterial = materialManager.cloneMaterial(materialID);

                // Set the grid coordinate UBO for this material
                clonedMaterial.setUBO(
                        EngineSetting.GRID_COORDINATE_UBO,
                        worldRenderInstance.getGridSlotHandle().getSlotUBO());

                // Create model from dynamic model data
                ModelHandle modelHandle = modelManager.createModel(
                        dynamicModel.getVAOHandle(),
                        dynamicModel.getVertices(),
                        dynamicModel.getIndices(),
                        clonedMaterial);

                // Add to model list
                modelList.add(modelHandle);
            }
        }

        if (modelList.isEmpty())
            return false;

        // Store the model list mapped to this coordinate
        coordinate2Models.put(coordinate, modelList);

        return true;
    }

    public void removeWorldInstance(long coordinate) {

        // Get the model list for this coordinate
        ObjectArrayList<ModelHandle> modelList = coordinate2Models.get(coordinate);

        if (modelList == null)
            return;

        // Remove each model (this unloads VBO/IBO from GPU)
        for (ModelHandle model : modelList)
            modelManager.removeMesh(model);

        // Clear the list and remove from map
        modelList.clear();
        coordinate2Models.remove(coordinate);
    }
}