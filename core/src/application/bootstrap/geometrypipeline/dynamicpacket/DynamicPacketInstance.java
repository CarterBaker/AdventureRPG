package application.bootstrap.geometrypipeline.dynamicpacket;

import java.util.concurrent.atomic.AtomicReference;

import application.bootstrap.geometrypipeline.dynamicmodel.DynamicModelHandle;
import application.bootstrap.geometrypipeline.vao.VAOHandle;
import engine.root.InstancePackage;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class DynamicPacketInstance extends InstancePackage {

    /*
     * Thread-safe geometry packet for one sub-chunk, chunk, or mega chunk.
     * Accumulates dynamic quad geometry into per-material DynamicModelHandle
     * buckets during a build pass. State transitions are atomic — EMPTY →
     * GENERATING → READY — to prevent concurrent writes from the build
     * thread and reads from the render thread. The state reference and the
     * material bucket map are allocated once in create() and reused for the
     * pooled object's whole lifetime. clear() never discards a material's
     * DynamicModelHandle buckets, even once they hold zero vertices — every
     * bucket already carries a FloatArrayList/ShortArrayList grown to
     * whatever size a previous build needed, and the game's block materials
     * are a small, fixed palette reused across every location a pooled
     * chunk is ever handed out to, so buckets built for one location are
     * just as likely to be needed again after that chunk is reassigned
     * somewhere else. WorldRenderManager.updateEntries() already skips
     * empty buckets and trims/disposes their GPU-side entries on its own,
     * so retaining empty buckets here costs nothing downstream.
     */

    // Internal
    private AtomicReference<DynamicPacketState> state;
    private VAOHandle vaoHandle;

    // Model Management
    private Int2ObjectOpenHashMap<ObjectArrayList<DynamicModelHandle>> materialID2ModelCollection;

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

        // Model Management — reset in place rather than reallocate
        clear();
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

    public boolean addVertices(int materialId, FloatArrayList vertList) {

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

            int added = target.tryAddVertices(vertList, processed, total - processed);

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

                if (!addVertices(materialId, applyOffset(vertices, offsetIndices, offsets)))
                    return false;
            }
        }

        return true;
    }

    private FloatArrayList applyOffset(FloatArrayList vertices, int[] offsetIndices, float[] offsets) {

        int stride = vaoHandle.getVAOData().getVertStride();
        FloatArrayList result = new FloatArrayList(vertices.size());

        for (int i = 0; i < vertices.size(); i += stride) {
            for (int j = 0; j < stride; j++) {

                float value = vertices.getFloat(i + j);

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
            Object[] elements = modelList.elements();
            int count = modelList.size();
            for (int i = 0; i < count; i++)
                ((DynamicModelHandle) elements[i]).clear();
        }

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