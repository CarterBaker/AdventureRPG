package application.bootstrap.geometrypipeline.dynamicpacket;

import java.util.concurrent.atomic.AtomicReference;

import application.bootstrap.geometrypipeline.dynamicmodel.DynamicModelHandle;
import application.bootstrap.geometrypipeline.vao.VAOHandle;
import engine.root.InstancePackage;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

public class DynamicPacketInstance extends InstancePackage {

    /*
     * Geometry packet for one subchunk, chunk or mega, bucketed per material.
     * Builders already hold the owner's lock, so its state is status only.
     * clear() is the full reset used by pooling and dumps, and buckets are
     * reused for the pooled object's lifetime. Merges shift copied vertices in
     * place inside the target bucket, so they allocate nothing.
     */

    // Internal
    private AtomicReference<DynamicPacketState> state;
    private VAOHandle vaoHandle;

    // Model Management
    private Int2ObjectOpenHashMap<ObjectArrayList<DynamicModelHandle>> materialID2ModelCollection;

    // Offsets
    private static final int[] EMPTY_OFFSET_INDICES = new int[0];
    private static final float[] EMPTY_OFFSETS = new float[0];

    // Internal \\

    @Override
    protected void create() {
        this.state = new AtomicReference<>(DynamicPacketState.EMPTY);
        this.materialID2ModelCollection = new Int2ObjectOpenHashMap<>();
    }

    // Constructor \\

    public void constructor(VAOHandle vaoHandle) {

        // Internal
        this.vaoHandle = vaoHandle;

        clear();
    }

    // State Management \\

    public void beginGenerating() {
        state.set(DynamicPacketState.GENERATING);
    }

    public void setReady() {
        state.set(DynamicPacketState.READY);
    }

    public void unlock() {
        state.set(DynamicPacketState.EMPTY);
    }

    // Dynamic Packet \\

    public boolean addVertices(int materialId, FloatArrayList vertList) {
        return addVertices(materialId, vertList, EMPTY_OFFSET_INDICES, EMPTY_OFFSETS);
    }

    private boolean addVertices(int materialId, FloatArrayList vertList, int[] offsetIndices, float[] offsets) {

        int floatsPerQuad = vaoHandle.getVAOData().getVertStride() * 4;

        if (vertList.size() % floatsPerQuad != 0)
            return false;

        ObjectArrayList<DynamicModelHandle> modelList = materialID2ModelCollection.computeIfAbsent(
                materialId, k -> new ObjectArrayList<>());

        int processed = 0;
        int total = vertList.size();

        while (processed < total) {

            DynamicModelHandle target = null;
            Object[] existing = modelList.elements();
            int existingCount = modelList.size();

            for (int i = 0; i < existingCount; i++) {
                DynamicModelHandle candidate = (DynamicModelHandle) existing[i];
                if (!candidate.isFull()) {
                    target = candidate;
                    break;
                }
            }

            boolean addToMaterialBucket = false;

            if (target == null) {
                target = create(DynamicModelHandle.class);
                target.constructor(materialId, vaoHandle);
                addToMaterialBucket = true;
            }

            int added = target.tryAddVertices(vertList, processed, total - processed, offsetIndices, offsets);

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

        int stride = vaoHandle.getVAOData().getVertStride();

        if (offsetIndices.length != offsets.length)
            throwException("offsetIndices and offsets must have same length");

        for (int index : offsetIndices) {
            if (index >= stride)
                throwException("offsetIndex " + index + " exceeds vertStride " + stride);
        }

        ObjectIterator<Int2ObjectMap.Entry<ObjectArrayList<DynamicModelHandle>>> iterator = other
                .materialID2ModelCollection
                .int2ObjectEntrySet()
                .fastIterator();

        while (iterator.hasNext()) {

            Int2ObjectMap.Entry<ObjectArrayList<DynamicModelHandle>> entry = iterator.next();
            int materialId = entry.getIntKey();
            ObjectArrayList<DynamicModelHandle> sourceModels = entry.getValue();

            if (sourceModels == null)
                continue;

            Object[] sources = sourceModels.elements();
            int sourceCount = sourceModels.size();

            for (int i = 0; i < sourceCount; i++) {

                DynamicModelHandle source = (DynamicModelHandle) sources[i];

                if (source == null || source.isEmpty())
                    continue;

                if (!addVertices(materialId, source.getVertices(), offsetIndices, offsets))
                    return false;
            }
        }

        return true;
    }

    public void clearModels() {

        for (ObjectArrayList<DynamicModelHandle> modelList : materialID2ModelCollection.values()) {
            Object[] elements = modelList.elements();
            int count = modelList.size();
            for (int i = 0; i < count; i++)
                ((DynamicModelHandle) elements[i]).clear();
        }
    }

    public void clear() {
        clearModels();
        unlock();
    }

    // Accessible \\

    public DynamicPacketState getState() {
        return state.get();
    }

    public boolean hasModels() {

        for (ObjectArrayList<DynamicModelHandle> models : materialID2ModelCollection.values()) {
            Object[] elements = models.elements();
            int count = models.size();
            for (int i = 0; i < count; i++)
                if (!((DynamicModelHandle) elements[i]).isEmpty())
                    return true;
        }

        return false;
    }

    public Int2ObjectOpenHashMap<ObjectArrayList<DynamicModelHandle>> getMaterialID2ModelCollection() {
        return materialID2ModelCollection;
    }
}