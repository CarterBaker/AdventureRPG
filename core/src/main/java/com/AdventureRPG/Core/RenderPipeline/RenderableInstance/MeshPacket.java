package com.AdventureRPG.Core.RenderPipeline.RenderableInstance;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;

import com.AdventureRPG.Core.Bootstrap.InstanceFrame;
import com.AdventureRPG.Core.RenderPipeline.RenderManager.RenderManager;
import com.AdventureRPG.Core.Util.GlobalConstant;

public final class MeshPacket extends InstanceFrame {

    private final RenderManager renderManager;

    public final Int2ObjectOpenHashMap<List<MeshData>> packet;
    public final int stride;
    public final int vertsPerQuad;

    public MeshPacket() {

        this.renderManager = engineManager.get(RenderManager.class);

        this.packet = new Int2ObjectOpenHashMap<>();
        this.stride = GlobalConstant.VERT_STRIDE;
        this.vertsPerQuad = 4;
    }

    public void addVertices(
            int materialId,
            float... verts) {

        List<MeshData> list = packet.computeIfAbsent(materialId, k -> new ArrayList<>());
        MeshData current = list.isEmpty() ? null : list.get(list.size() - 1);

        if (current == null) {

            current = new MeshData(this, renderManager.createUniqueHandle(), materialId);
            list.add(current);
        }

        MeshData overflow = current.addVertices(verts);

        if (overflow != null)
            list.add(overflow);
    }

    public void merge(MeshPacket other) {

        for (var entry : other.packet.int2ObjectEntrySet()) {

            int mat = entry.getIntKey();
            List<MeshData> otherList = entry.getValue();
            List<MeshData> targetList = packet.computeIfAbsent(mat, k -> new ArrayList<>());

            for (MeshData src : otherList) {

                float[] srcVerts = src.getVerticesArray();
                int offset = 0;

                // Fill all existing MeshData sequentially first
                for (MeshData current : targetList) {

                    if (offset >= srcVerts.length)
                        break;

                    MeshData overflow = current.addVerticesPartial(srcVerts, offset, srcVerts.length - offset);

                    if (overflow != null)
                        offset += current.getVertexCount() * stride;

                    else {

                        offset = srcVerts.length;
                        break;
                    }
                }

                // Create new MeshData if needed
                while (offset < srcVerts.length) {

                    MeshData newMesh = new MeshData(this, renderManager.createUniqueHandle(), mat);
                    MeshData overflow = newMesh.addVerticesPartial(srcVerts, offset, srcVerts.length - offset);

                    targetList.add(newMesh);

                    if (overflow != null)
                        offset += newMesh.getVertexCount() * stride;
                    else
                        break;
                }
            }
        }
    }

    public void clear() {
        renderManager.removeModel(this);
        packet.clear();
    }

    // Accessible \\

    public int getTotalVertexCount() {

        int total = 0;

        for (List<MeshData> list : packet.values())
            for (MeshData data : list)
                total += data.getVertexCount();

        return total;
    }

    public int getTotalMeshCount() {

        int total = 0;

        for (List<MeshData> list : packet.values())
            total += list.size();

        return total;
    }
}
