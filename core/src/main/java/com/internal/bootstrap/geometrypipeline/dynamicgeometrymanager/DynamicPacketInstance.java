package com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager;

import java.util.concurrent.atomic.AtomicReference;

import com.internal.bootstrap.geometrypipeline.modelmanager.ModelHandle;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOHandle;
import com.internal.bootstrap.renderpipeline.rendercall.RenderCallHandle;
import com.internal.core.engine.InstancePackage;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class DynamicPacketInstance extends InstancePackage {

    // Internal
    private AtomicReference<DynamicPacketState> state;
    private VAOHandle vaoHandle;
    private Int2ObjectOpenHashMap<ObjectArrayList<DynamicModelHandle>> materialID2ModelCollection;
    private ObjectArrayList<ModelHandle> modelHandleCollection;
    private Int2ObjectOpenHashMap<RenderCallHandle> renderCallID2RenderCall;

    // Internal \\

    public void constructor(VAOHandle vaoHandle) {

        // Internal
        this.state = new AtomicReference<>(DynamicPacketState.EMPTY);
        this.vaoHandle = vaoHandle;
        this.materialID2ModelCollection = new Int2ObjectOpenHashMap<>();
    }

    // State Management \\

    public boolean tryLock() {
        return state.compareAndSet(DynamicPacketState.EMPTY, DynamicPacketState.GENERATING);
    }

    public void unlock() {
        state.set(DynamicPacketState.EMPTY);
    }

    public void setReady() {
        state.set(DynamicPacketState.READY);
    }

    // Dynamic Packet \\

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

    public boolean merge(DynamicPacketInstance other) {

        if (other == null || other.materialID2ModelCollection == null)
            return true;

        try {

            for (var entry : other.materialID2ModelCollection.int2ObjectEntrySet()) {

                int materialId = entry.getIntKey();
                ObjectArrayList<DynamicModelHandle> sourceModels = entry.getValue();

                if (sourceModels == null)
                    continue;

                for (DynamicModelHandle sourceModel : sourceModels) {

                    if (sourceModel == null || sourceModel.isEmpty())
                        continue;

                    FloatArrayList vertices = sourceModel.getVertices();

                    if (vertices == null || vertices.isEmpty())
                        continue;

                    addVertices(materialId, vertices);
                }
            }

            return true;
        }

        catch (Exception e) {
            return false;
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

    // Utility \\

    public DynamicPacketState getState() {
        return state.get();
    }

    public void setState(DynamicPacketState state) {
        this.state.set(state);
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
}