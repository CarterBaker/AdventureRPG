package com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager;

import java.util.concurrent.atomic.AtomicReference;

import com.internal.bootstrap.geometrypipeline.vaomanager.VAOHandle;
import com.internal.core.engine.InstancePackage;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class DynamicPacketInstance extends InstancePackage {

    // Internal
    private AtomicReference<DynamicPacketState> state;
    private VAOHandle vaoHandle;

    // model Management
    private Int2ObjectOpenHashMap<ObjectArrayList<DynamicModelHandle>> materialID2ModelCollection;

    // Internal \\

    public void constructor(VAOHandle vaoHandle) {

        // Internal
        this.state = new AtomicReference<>(DynamicPacketState.EMPTY);
        this.vaoHandle = vaoHandle;

        // model Management
        this.materialID2ModelCollection = new Int2ObjectOpenHashMap<>();
    }

    // State Management \\

    public boolean tryLock() {
        return state.compareAndSet(DynamicPacketState.EMPTY, DynamicPacketState.GENERATING);
    }

    public void setReady() {
        state.set(DynamicPacketState.READY);
    }

    public void unlock() {
        state.set(DynamicPacketState.EMPTY);
    }

    // Dynamic Packet \\

    public boolean addVertices(
            int materialId,
            FloatArrayList vertList) {

        int floatsPerQuad = vaoHandle.getVertStride() * 4;

        // Enforce quad alignment
        if (vertList.size() % floatsPerQuad != 0)
            return false;

        ObjectArrayList<DynamicModelHandle> modelList = materialID2ModelCollection.computeIfAbsent(
                materialId, k -> new ObjectArrayList<>());

        int processed = 0;
        int total = vertList.size();

        while (processed < total) {

            // Find first model with remaining capacity
            DynamicModelHandle target = null;
            for (DynamicModelHandle model : modelList)
                if (!model.isFull()) {
                    target = model;
                    break;
                }

            // Allocate new model if all are full
            boolean addToMaterialBucket = false;
            if (target == null) {
                target = create(DynamicModelHandle.class);
                target.constructor(materialId, vaoHandle);
                addToMaterialBucket = true;
            }

            // Attempt to append vertices
            int added = target.tryAddVertices(
                    vertList,
                    processed,
                    total - processed);

            if (added <= 0)
                return false;

            else if (addToMaterialBucket)
                modelList.add(target);

            processed += added;
        }

        return true;
    }

    public boolean merge(DynamicPacketInstance other, int[] offsetIndices, float[] offsets) {
        if (other == null || other.materialID2ModelCollection == null)
            return true;

        int stride = vaoHandle.getVertStride();

        // Validation
        if (offsetIndices.length != offsets.length)
            throw new IllegalArgumentException("offsetIndices and offsets must have same length");

        for (int index : offsetIndices) {
            if (index >= stride)
                throw new IllegalArgumentException("offsetIndex " + index + " exceeds vertStride " + stride);
        }

        for (var entry : other.materialID2ModelCollection.int2ObjectEntrySet()) {
            int materialId = entry.getIntKey();
            ObjectArrayList<DynamicModelHandle> sourceModels = entry.getValue();

            if (sourceModels == null)
                continue;

            for (DynamicModelHandle source : sourceModels) {
                if (source == null || source.isEmpty())
                    continue;

                FloatArrayList vertices = source.getVertices();
                if (vertices == null || vertices.isEmpty())
                    continue;

                FloatArrayList offsetVertices = applyOffset(vertices, offsetIndices, offsets);

                if (!addVertices(materialId, offsetVertices))
                    return false;
            }
        }

        return true;
    }

    private FloatArrayList applyOffset(FloatArrayList vertices, int[] offsetIndices, float[] offsets) {
        int stride = vaoHandle.getVertStride();
        FloatArrayList result = new FloatArrayList(vertices.size());

        for (int i = 0; i < vertices.size(); i += stride) {
            // Copy entire vertex
            for (int j = 0; j < stride; j++) {
                float value = vertices.getFloat(i + j);

                // Apply offsets to specified indices
                for (int k = 0; k < offsetIndices.length; k++) {
                    if (j == offsetIndices[k]) {
                        value += offsets[k];
                        break;
                    }
                }

                result.add(value);
            }
        }

        return result;
    }

    public void clear() {

        for (ObjectArrayList<DynamicModelHandle> modelList : materialID2ModelCollection.values()) {

            for (DynamicModelHandle model : modelList)
                model.clear();

            modelList.clear();
        }

        materialID2ModelCollection.clear();

        unlock();
    }

    // Accessible \\

    public DynamicPacketState getState() {
        return state.get();
    }

    public boolean hasModels() {

        for (ObjectArrayList<DynamicModelHandle> models : materialID2ModelCollection.values())
            if (models.size() > 0)
                return true;

        return false;
    }

    public Int2ObjectOpenHashMap<ObjectArrayList<DynamicModelHandle>> getMaterialID2ModelCollection() {
        return materialID2ModelCollection;
    }
}