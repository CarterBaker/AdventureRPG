package application.bootstrap.geometrypipeline.meshmanager;

import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class MeshLayoutUtility extends EngineUtility {

    /*
     * Tells the two layouts a quad mesh file may be in apart and rewrites the
     * first as the second. The quad layout lists a "vbo" of quad entries,
     * every corner carrying its own data; the compact layout holds a "mesh"
     * object of typed arrays in which a vertex is stored once and quads
     * index it. A rigged mesh is rewritten by RiggedMeshArpgUtility; a static
     * quad mesh becomes its distinct "vertices", every attribute of each in
     * the order its layout declares, and the "quads" joining four of them.
     * Files that mix raw vertices or an index list with their quads, and
     * sub-voxel meshes, have no compact layout and are left as they are.
     * The ARPG tool upgrades files through here.
     */

    // Detection \\

    public static boolean isCompact(ArpgObjectStruct meshArpg) {
        return ArpgUtility.hasObject(meshArpg, "mesh");
    }

    // A file whose every "vbo" element is a quad entry and that declares no indices of its own
    public static boolean isQuadLayout(ArpgObjectStruct meshArpg) {

        if (isCompact(meshArpg) || !ArpgUtility.hasArray(meshArpg, "vbo") || meshArpg.getAsArray("vbo").size() == 0
                || (meshArpg.has("ibo") && !meshArpg.get("ibo").isNull()))
            return false;

        for (ArpgElementStruct element : meshArpg.getAsArray("vbo"))
            if (!element.isObject() || !element.getAsObject().has("quad"))
                return false;

        return true;
    }

    // Upgrade \\

    public static ArpgObjectStruct upgrade(ArpgObjectStruct meshArpg, String sourceName) {

        if (RiggedMeshArpgUtility.isRigged(meshArpg))
            return RiggedMeshArpgUtility.upgrade(meshArpg, sourceName);

        ArpgObjectStruct upgradedArpg = new ArpgObjectStruct();
        ArpgObjectStruct compactArpg = upgradeQuads(meshArpg.getAsArray("vbo"), sourceName);

        for (String key : meshArpg.keySet())
            upgradedArpg.add(key.equals("vbo") ? "mesh" : key, key.equals("vbo") ? compactArpg : meshArpg.get(key));

        return upgradedArpg;
    }

    // Corners whose every float matches become one vertex, numbered in the order they first appear
    private static ArpgObjectStruct upgradeQuads(ArpgArrayStruct vboArpg, String sourceName) {

        Object2IntOpenHashMap<String> corner2Vertex = new Object2IntOpenHashMap<>();
        StringBuilder cornerKey = new StringBuilder();
        FloatArrayList vertices = new FloatArrayList();
        IntArrayList quads = new IntArrayList();
        int vertStride = EngineSetting.INDEX_NOT_FOUND;

        corner2Vertex.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);

        for (ArpgElementStruct element : vboArpg) {

            ArpgArrayStruct corners = element.getAsObject().getAsArray("quad");

            if (corners.size() != EngineSetting.QUAD_VERTEX_COUNT)
                throwException("Quad 'quad' array must have exactly 4 corners in file: " + sourceName);

            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                ArpgArrayStruct corner = corners.get(c).getAsArray();

                if (vertStride == EngineSetting.INDEX_NOT_FOUND)
                    vertStride = corner.size();

                if (corner.size() != vertStride)
                    throwException("Quad corner has " + corner.size() + " floats, expected " + vertStride
                            + " in file: " + sourceName);

                cornerKey.setLength(0);

                for (ArpgElementStruct value : corner)
                    cornerKey.append(Float.floatToIntBits(value.getAsFloat() + 0f)).append(',');

                String key = cornerKey.toString();
                int vertex = corner2Vertex.getInt(key);

                if (vertex == EngineSetting.INDEX_NOT_FOUND) {

                    vertex = corner2Vertex.size();
                    corner2Vertex.put(key, vertex);

                    for (ArpgElementStruct value : corner)
                        vertices.add(value.getAsFloat());
                }

                quads.add(vertex);
            }
        }

        ArpgObjectStruct compactArpg = new ArpgObjectStruct();
        compactArpg.add("vertices", ArpgUtility.toBuffer(vertices.elements(), vertices.size()));
        compactArpg.add("quads", ArpgUtility.toIndexBuffer(quads.elements(), quads.size()));

        return compactArpg;
    }
}
