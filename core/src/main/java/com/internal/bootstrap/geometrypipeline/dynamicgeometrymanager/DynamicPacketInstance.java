package com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager;

import com.internal.bootstrap.geometrypipeline.modelmanager.ModelHandle;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOHandle;
import com.internal.bootstrap.renderpipeline.rendercall.RenderCallHandle;
import com.internal.core.engine.InstancePackage;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class DynamicPacketInstance extends InstancePackage {

    // Internal
    private volatile DynamicPacketState state;
    private VAOHandle vaoHandle;
    private Int2ObjectOpenHashMap<ObjectArrayList<DynamicModelHandle>> materialID2ModelCollection;
    private ObjectArrayList<ModelHandle> modelHandleCollection;
    private Int2ObjectOpenHashMap<RenderCallHandle> renderCallID2RenderCall;

    // Internal \\

    public void constructor(VAOHandle vaoHandle) {

        // Internal
        this.state = DynamicPacketState.EMPTY;
        this.vaoHandle = vaoHandle;
        this.materialID2ModelCollection = new Int2ObjectOpenHashMap<>();
    }

    // Utility \\

    public DynamicPacketState getState() {
        return state;
    }

    public void setState(DynamicPacketState state) {
        this.state = state;
    }

    public Int2ObjectOpenHashMap<ObjectArrayList<DynamicModelHandle>> getMaterialID2ModelCollection() {
        return materialID2ModelCollection;
    }

    public ObjectArrayList<DynamicModelHandle> getModelsForMaterial(int materialId) {
        return materialID2ModelCollection.get(materialId);
    }

    public int getMaterialCount() {
        return materialID2ModelCollection.size();
    }

    public int getTotalModelCount() {
        int total = 0;
        for (ObjectArrayList<DynamicModelHandle> models : materialID2ModelCollection.values()) {
            total += models.size();
        }
        return total;
    }

    public int getTotalVertexCount() {
        int total = 0;
        for (ObjectArrayList<DynamicModelHandle> models : materialID2ModelCollection.values()) {
            for (DynamicModelHandle model : models) {
                total += model.getVertexCount();
            }
        }
        return total;
    }

    // Accessible \\

    public void addVertices(int materialId, FloatArrayList vertList) {

        int floatsPerQuad = vaoHandle.getVertStride() * 4;

        if (vertList.size() % floatsPerQuad != 0)
            throwException("Vertex data must be quad-aligned");

        ObjectArrayList<DynamicModelHandle> modelList = materialID2ModelCollection.computeIfAbsent(
                materialId,
                k -> new ObjectArrayList<>());

        int processed = 0;
        int totalFloats = vertList.size();

        while (processed < totalFloats) {

            // Try to find an existing model with space
            DynamicModelHandle targetModel = null;
            for (DynamicModelHandle model : modelList) {
                if (!model.isFull()) {
                    targetModel = model;
                    break;
                }
            }

            // Create new model if needed
            if (targetModel == null) {
                targetModel = create(DynamicModelHandle.class);
                targetModel.constructor(materialId, vaoHandle);
                modelList.add(targetModel);
            }

            // Add as many vertices as possible to this model
            int remaining = totalFloats - processed;
            int added = targetModel.tryAddVertices(vertList, processed, remaining);

            if (added == 0)
                throwException("Failed to add vertices to model");

            processed += added;
        }
    }

    public void merge(DynamicPacketInstance other) {

        for (var entry : other.materialID2ModelCollection.int2ObjectEntrySet()) {

            int materialId = entry.getIntKey();
            ObjectArrayList<DynamicModelHandle> sourceModels = entry.getValue();

            for (DynamicModelHandle sourceModel : sourceModels) {

                if (sourceModel.isEmpty())
                    continue;

                addVertices(materialId, sourceModel.getVertices());
            }
        }
    }

    public void clear() {

        for (ObjectArrayList<DynamicModelHandle> modelList : materialID2ModelCollection.values()) {
            for (DynamicModelHandle model : modelList) {
                model.clear();
            }
            modelList.clear();
        }

        materialID2ModelCollection.clear();

        setState(DynamicPacketState.EMPTY);
    }

    public void setModelHandleCollection(ObjectArrayList<ModelHandle> modelHandleCollection) {
        this.modelHandleCollection = modelHandleCollection;
    }

    public ObjectArrayList<ModelHandle> getModelHandleCollection() {
        return modelHandleCollection;
    }

    public void clearModelHandleCollection() {
        modelHandleCollection.clear();
    }

    public void setRenderCallCollection(Int2ObjectOpenHashMap<RenderCallHandle> renderCallID2RenderCall) {
        this.renderCallID2RenderCall = renderCallID2RenderCall;
    }

    public Int2ObjectOpenHashMap<RenderCallHandle> getRenderCallCollection() {
        return renderCallID2RenderCall;
    }

    public void clearRenderCallCollection() {
        renderCallID2RenderCall.clear();
    }
}