package com.internal.bootstrap.worldpipeline.dynamicgeometrymanager;

import com.internal.bootstrap.geometrypipeline.vaomanager.VAOHandle;
import com.internal.core.engine.InstancePackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.matrices.Matrix4;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class DynamicModelInstance extends InstancePackage {

    // Internal
    final int modelID;
    final VAOHandle vaoHandle;
    private Matrix4 transform;

    private int floatsPerQuad;
    private Int2ObjectOpenHashMap<ObjectArrayList<DynamicMeshInstance>> materialID2MeshCollection;

    private boolean rendering;

    // Base \\

    DynamicModelInstance(int modelID, VAOHandle vaoHandle) {

        // Internal
        this.modelID = modelID;
        this.vaoHandle = vaoHandle;
        this.transform = new Matrix4();

        this.floatsPerQuad = vaoHandle.getVertStride() * 4;
        this.materialID2MeshCollection = new Int2ObjectOpenHashMap<>();

        this.rendering = false;
    }

    // Data \\

    public void addVertices(int materialId, FloatArrayList vertList) {

        if (vertList.size() % floatsPerQuad != 0) // TODO: Add my own error
            throw new IllegalArgumentException("Vertex data must be quad-aligned");

        addVerticesInternal(materialId, vertList, 0, vertList.size());
    }

    public void merge(DynamicModelInstance other) {

        for (var entry : other.materialID2MeshCollection.int2ObjectEntrySet()) {

            int materialId = entry.getIntKey();
            ObjectArrayList<DynamicMeshInstance> sourceMeshes = entry.getValue();

            for (DynamicMeshInstance sourceMesh : sourceMeshes) {

                if (sourceMesh.isEmpty())
                    continue;

                // Try fast path: complete mesh transfer to existing mesh
                if (tryMergeCompleteMesh(materialId, sourceMesh))
                    continue;

                // Fall back to splitting logic
                addVerticesInternal(materialId, sourceMesh.getVerticesList(), 0,
                        sourceMesh.getVerticesList().size());
            }
        }
    }

    private boolean tryMergeCompleteMesh(int materialId, DynamicMeshInstance sourceMesh) {

        ObjectArrayList<DynamicMeshInstance> meshList = materialID2MeshCollection.get(materialId);

        if (meshList == null)
            return false;

        // Try to fit into existing meshes
        for (DynamicMeshInstance targetMesh : meshList)
            if (!targetMesh.isFull() && targetMesh.tryAddCompleteMesh(sourceMesh))
                return true;

        // Try creating one new mesh for the whole thing
        if (sourceMesh.getVertexCount() <= EngineSetting.MESH_VERT_LIMIT) {
            DynamicMeshInstance newMesh = new DynamicMeshInstance(vaoHandle);
            meshList.add(newMesh);
            return newMesh.tryAddCompleteMesh(sourceMesh);
        }

        return false;
    }

    private void addVerticesInternal(int materialId, FloatArrayList vertList, int offset, int length) {

        ObjectArrayList<DynamicMeshInstance> meshList = materialID2MeshCollection.computeIfAbsent(
                materialId,
                k -> new ObjectArrayList<>());

        int processed = 0;

        // Try to fill existing meshes first
        for (DynamicMeshInstance mesh : meshList) {

            if (processed >= length)
                break;

            if (mesh.isFull())
                continue;

            int added = mesh.tryAddVertices(vertList, offset + processed, length - processed);
            processed += added * vaoHandle.getVertStride();
        }

        // Create new meshes for remaining data
        while (processed < length) {

            DynamicMeshInstance newMesh = new DynamicMeshInstance(vaoHandle);
            meshList.add(newMesh);

            int added = newMesh.tryAddVertices(vertList, offset + processed, length - processed);

            if (added == 0) // TODO: Add my own error
                throw new IllegalStateException("Failed to add vertices to new mesh");

            processed += added * vaoHandle.getVertStride();
        }
    }

    public void consolidateMeshes(float minFillThreshold) {

        if (minFillThreshold <= 0.0f || minFillThreshold >= 1.0f) // TODO: Add my own error
            throw new IllegalArgumentException("Threshold must be between 0 and 1 exclusive");

        for (var entry : materialID2MeshCollection.int2ObjectEntrySet()) {

            int materialId = entry.getIntKey();
            ObjectArrayList<DynamicMeshInstance> meshList = entry.getValue();

            if (meshList.size() <= 1)
                continue;

            // Find sparse meshes
            ObjectArrayList<DynamicMeshInstance> sparseMeshes = new ObjectArrayList<>();
            ObjectArrayList<DynamicMeshInstance> denseMeshes = new ObjectArrayList<>();

            for (DynamicMeshInstance mesh : meshList) {

                if (mesh.getFillPercentage() < minFillThreshold && !mesh.isEmpty())
                    sparseMeshes.add(mesh);

                else if (!mesh.isEmpty())
                    denseMeshes.add(mesh);
            }

            // Nothing to consolidate
            if (sparseMeshes.size() <= 1)
                continue;

            // Extract all vertices from sparse meshes
            FloatArrayList consolidatedVerts = new FloatArrayList();
            for (DynamicMeshInstance sparse : sparseMeshes) {
                FloatArrayList verts = sparse.getVerticesList();
                consolidatedVerts.addElements(consolidatedVerts.size(), verts.elements(), 0, verts.size());
                sparse.clear();
            }

            // Rebuild mesh list with dense meshes only
            meshList.clear();
            meshList.addAll(denseMeshes);

            // Re-add consolidated data
            if (!consolidatedVerts.isEmpty())
                addVerticesInternal(materialId, consolidatedVerts, 0, consolidatedVerts.size());
        }
    }

    public void clear() {

        for (ObjectArrayList<DynamicMeshInstance> meshList : materialID2MeshCollection.values()) {

            for (DynamicMeshInstance mesh : meshList)
                mesh.clear();

            meshList.clear();
        }

        materialID2MeshCollection.clear();
    }

    // Accessible \\

    public int getModelID() {
        return modelID;
    }

    public Matrix4 getTransform() {
        return transform;
    }

    public void setTransform(Matrix4 transform) {
        this.transform = transform;
    }

    public Int2ObjectOpenHashMap<ObjectArrayList<DynamicMeshInstance>> getMaterialID2MeshCollection() {
        return materialID2MeshCollection;
    }

    public ObjectArrayList<DynamicMeshInstance> getMeshesForMaterial(int materialId) {
        return materialID2MeshCollection.get(materialId);
    }

    public int getTotalVertexCount() {

        int total = 0;
        for (ObjectArrayList<DynamicMeshInstance> meshList : materialID2MeshCollection.values())
            for (DynamicMeshInstance mesh : meshList)
                total += mesh.getVertexCount();

        return total;
    }

    public int getTotalMeshCount() {

        int total = 0;
        for (ObjectArrayList<DynamicMeshInstance> meshList : materialID2MeshCollection.values())
            total += meshList.size();

        return total;
    }

    public int getMaterialCount() {
        return materialID2MeshCollection.size();
    }

    public boolean rendering() {
        return rendering;
    }

    public void setRendering(boolean rendering) {
        this.rendering = rendering;
    }

    public void pushMeshToGPU() {

        if (rendering)
            return; // TODO: This should throw an error

        var entryIter = materialID2MeshCollection.int2ObjectEntrySet().fastIterator();
        while (entryIter.hasNext()) {

            var entry = entryIter.next();
            ObjectArrayList<DynamicMeshInstance> meshList = entry.getValue();

            int size = meshList.size();
            for (int i = 0; i < size; i++) {

                DynamicMeshInstance mesh = meshList.get(i);

                if (!mesh.isEmpty())
                    mesh.pushToGPU();
            }
        }
    }

    public void pullMeshFromGPU() {

        if (!rendering)
            return; // TODO: This should throw an error

        var entryIter = materialID2MeshCollection.int2ObjectEntrySet().fastIterator();
        while (entryIter.hasNext()) {

            var entry = entryIter.next();
            ObjectArrayList<DynamicMeshInstance> meshList = entry.getValue();

            int size = meshList.size();
            for (int i = 0; i < size; i++) {

                DynamicMeshInstance mesh = meshList.get(i);

                if (!mesh.isEmpty())
                    mesh.pullFromGPU();
            }
        }
    }
}