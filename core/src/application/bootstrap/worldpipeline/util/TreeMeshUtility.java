package application.bootstrap.worldpipeline.util;

import application.bootstrap.geometrypipeline.subvoxel.SubVoxelGridStruct;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelQuadListStruct;
import application.bootstrap.geometrypipeline.subvoxelmanager.SubVoxelMeshUtility;
import application.bootstrap.worldpipeline.tree.TreeHandle;
import application.bootstrap.worldpipeline.tree.TreeShapeStruct;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public final class TreeMeshUtility extends EngineUtility {

    /*
     * Writes tree geometry in the chunk vertex layout, for the chunk mesher
     * and a falling tree alike. Wood is the merged quads of a sub-voxel grid:
     * each vertex carries its face, its place on the quad and the quad's size
     * in sub-voxels, and how each of the quad's four edges meets the wood
     * beyond it — rounding over an outer edge, folding into an inner one, or
     * running on flat — so the bark shader can bevel the square wood round and
     * shade its creases. Each edge is read along its whole length from the
     * grid and takes the kind most of it is. A quad lying wholly inside a leaf
     * cluster, within the smallest the clump's lumps ever shrink it to, can
     * never be seen and is left out, so the twigs a crown hides cost nothing. A leaf cluster is the six faces
     * of the box around its ellipsoid, each vertex carrying the way to the
     * cluster's centre and its radii, so the leaf shader traces the rounded
     * clump inside the box and every vertex stays correct wherever a merge
     * moves the box. Positions are in blocks in the caller's frame.
     */

    private static final float SCALE = 1f / EngineSetting.SUB_VOXEL_RESOLUTION;

    // Wood \\

    // Every quad of the grid no leaf cluster hides, its part resolving to a species and its bark or heartwood
    // through partBase / 2; the grid's sub-voxel origin lies at the given block position of the caller's frame, and
    // the hiders are the clusters collectHiders() gathered in the grid's own sub-voxels
    public static void emitWood(
            SubVoxelGridStruct grid,
            SubVoxelQuadListStruct quads,
            ObjectArrayList<TreeHandle> partSpecies,
            FloatArrayList hiders,
            float originX,
            float originY,
            float originZ,
            FloatArrayList out) {

        float[] corner = new float[EngineSetting.AXIS_COUNT];

        for (int quad = 0; quad < quads.size(); quad++) {

            if (isHidden(quads, quad, hiders, corner))
                continue;

            int part = quads.getPart(quad);
            TreeHandle treeHandle = partSpecies.get(part / EngineSetting.TREE_WOOD_PART_COUNT);
            int woodPart = part % EngineSetting.TREE_WOOD_PART_COUNT;
            float[] corners = treeHandle.getPartCorners();
            float cornerU = corners[woodPart * EngineSetting.TREE_PART_CORNER_FLOATS];
            float cornerV = corners[woodPart * EngineSetting.TREE_PART_CORNER_FLOATS + 1];
            float color = treeHandle.getPartColors()[woodPart];
            int face = quads.getFace(quad);
            float meta = face | resolveEdges(grid, quads, quad) << EngineSetting.TREE_META_EDGE_SHIFT;

            for (int vertex = 0; vertex < EngineSetting.QUAD_VERTEX_COUNT; vertex++) {

                float localU = isHighU(vertex) ? quads.getWidth(quad) : 0f;
                float localV = isHighV(vertex) ? quads.getHeight(quad) : 0f;

                resolveWoodCorner(quads, quad, vertex, corner);

                out.add(originX + corner[0] * SCALE);
                out.add(originY + corner[1] * SCALE);
                out.add(originZ + corner[2] * SCALE);
                out.add(cornerU);
                out.add(cornerV);
                out.add(meta);
                out.add(color);
                out.add(localU);
                out.add(localV);
                out.add(quads.getWidth(quad));
                out.add(quads.getHeight(quad));
                addPadding(out);
            }
        }
    }

    // A quad corner in grid sub-voxels, corners running low-low, high-low, high-high, low-high on the face's axes
    private static void resolveWoodCorner(SubVoxelQuadListStruct quads, int quad, int vertex, float[] corner) {

        int axis = SubVoxelMeshUtility.resolveAxis(quads.getFace(quad));
        int uAxis = (axis + 1) % EngineSetting.AXIS_COUNT;
        int vAxis = (axis + 2) % EngineSetting.AXIS_COUNT;

        corner[axis] = quads.getPlane(quad);
        corner[uAxis] = quads.getU(quad) + (isHighU(vertex) ? quads.getWidth(quad) : 0);
        corner[vAxis] = quads.getV(quad) + (isHighV(vertex) ? quads.getHeight(quad) : 0);
    }

    private static boolean isHighU(int vertex) {
        return vertex == 1 || vertex == 2;
    }

    private static boolean isHighV(int vertex) {
        return vertex >= 2;
    }

    // Edges \\

    // The four edges' kinds packed two bits each — low u, high u, low v, high v
    private static int resolveEdges(SubVoxelGridStruct grid, SubVoxelQuadListStruct quads, int quad) {

        int face = quads.getFace(quad);
        int axis = SubVoxelMeshUtility.resolveAxis(face);
        int normal = SubVoxelMeshUtility.getFaceNormal(face, axis);
        int solid = normal > 0 ? quads.getPlane(quad) - 1 : quads.getPlane(quad);
        int u = quads.getU(quad);
        int v = quads.getV(quad);
        int width = quads.getWidth(quad);
        int height = quads.getHeight(quad);

        int lowU = classifyEdge(grid, axis, solid, normal, true, u - 1, v, height);
        int highU = classifyEdge(grid, axis, solid, normal, true, u + width, v, height);
        int lowV = classifyEdge(grid, axis, solid, normal, false, v - 1, u, width);
        int highV = classifyEdge(grid, axis, solid, normal, false, v + height, u, width);

        return lowU
                | highU << EngineSetting.TREE_EDGE_BITS
                | lowV << EngineSetting.TREE_EDGE_BITS * 2
                | highV << EngineSetting.TREE_EDGE_BITS * 3;
    }

    // The kind most of one edge is: the cells just past it in the quad's solid layer and the layer before it
    private static int classifyEdge(
            SubVoxelGridStruct grid,
            int axis,
            int solid,
            int normal,
            boolean alongV,
            int across,
            int from,
            int length) {

        int uAxis = (axis + 1) % EngineSetting.AXIS_COUNT;
        int vAxis = (axis + 2) % EngineSetting.AXIS_COUNT;
        int[] cell = new int[EngineSetting.AXIS_COUNT];
        int convex = 0;
        int concave = 0;

        for (int step = 0; step < length; step++) {

            cell[axis] = solid;
            cell[alongV ? uAxis : vAxis] = across;
            cell[alongV ? vAxis : uAxis] = from + step;

            if (!grid.isFilled(cell[0], cell[1], cell[2])) {
                convex++;
                continue;
            }

            cell[axis] = solid + normal;

            if (grid.isFilled(cell[0], cell[1], cell[2]))
                concave++;
        }

        int flat = length - convex - concave;

        if (convex >= concave && convex >= flat)
            return EngineSetting.TREE_EDGE_CONVEX;

        return concave >= flat ? EngineSetting.TREE_EDGE_CONCAVE : EngineSetting.TREE_EDGE_FLAT;
    }

    // Hiders \\

    // A shape's clusters, shrunk to what their lumps always cover, appended in grid sub-voxels for a shape whose
    // root centre lies at the given grid sub-voxel — centre, then horizontal and vertical radius
    public static void collectHiders(
            TreeShapeStruct shape,
            float rootX,
            float rootY,
            float rootZ,
            FloatArrayList hiders) {

        float resolution = EngineSetting.SUB_VOXEL_RESOLUTION;

        for (int leaf = 0; leaf < shape.getLeafCount(); leaf++) {
            hiders.add(rootX + shape.getLeafX(leaf) * resolution);
            hiders.add(rootY + shape.getLeafY(leaf) * resolution);
            hiders.add(rootZ + shape.getLeafZ(leaf) * resolution);
            hiders.add(shape.getLeafRadiusH(leaf) * resolution * EngineSetting.TREE_LEAF_HIDE_SHARE);
            hiders.add(shape.getLeafRadiusV(leaf) * resolution * EngineSetting.TREE_LEAF_HIDE_SHARE);
        }
    }

    // True when every corner of a quad lies inside one cluster
    private static boolean isHidden(
            SubVoxelQuadListStruct quads,
            int quad,
            FloatArrayList hiders,
            float[] corner) {

        float[] spheres = hiders.elements();

        for (int hider = 0; hider < hiders.size(); hider += EngineSetting.TREE_HIDER_FLOATS) {

            boolean inside = true;

            for (int vertex = 0; vertex < EngineSetting.QUAD_VERTEX_COUNT && inside; vertex++) {

                resolveWoodCorner(quads, quad, vertex, corner);

                float dx = (corner[0] - spheres[hider]) / spheres[hider + 3];
                float dy = (corner[1] - spheres[hider + 1]) / spheres[hider + 4];
                float dz = (corner[2] - spheres[hider + 2]) / spheres[hider + 3];

                inside = dx * dx + dy * dy + dz * dz < 1f;
            }

            if (inside)
                return true;
        }

        return false;
    }

    // Leaves \\

    // The clusters of a shape whose centres lie inside a box of the caller's frame, min inclusive and max exclusive,
    // the shape's root centre at the given position of that frame
    public static void emitLeaves(
            TreeShapeStruct shape,
            float rootX,
            float rootY,
            float rootZ,
            float minX,
            float minY,
            float minZ,
            float maxX,
            float maxY,
            float maxZ,
            FloatArrayList out) {

        TreeHandle treeHandle = shape.getTreeHandle();
        float[] corners = treeHandle.getPartCorners();
        int[] colors = treeHandle.getPartColors();

        for (int leaf = 0; leaf < shape.getLeafCount(); leaf++) {

            float centerX = rootX + shape.getLeafX(leaf);
            float centerY = rootY + shape.getLeafY(leaf);
            float centerZ = rootZ + shape.getLeafZ(leaf);

            if (centerX < minX || centerX >= maxX || centerY < minY || centerY >= maxY
                    || centerZ < minZ || centerZ >= maxZ)
                continue;

            int part = shape.isLeafAccent(leaf) ? EngineSetting.TREE_PART_ACCENT : EngineSetting.TREE_PART_LEAF;

            emitCluster(
                    centerX, centerY, centerZ,
                    shape.getLeafRadiusH(leaf), shape.getLeafRadiusV(leaf),
                    corners[part * EngineSetting.TREE_PART_CORNER_FLOATS],
                    corners[part * EngineSetting.TREE_PART_CORNER_FLOATS + 1],
                    colors[part],
                    shape.getLeafSeed(leaf),
                    out);
        }
    }

    // The six faces of the box around one cluster's ellipsoid
    private static void emitCluster(
            float centerX,
            float centerY,
            float centerZ,
            float radiusH,
            float radiusV,
            float cornerU,
            float cornerV,
            int color,
            float seed,
            FloatArrayList out) {

        float[] radius = { radiusH, radiusV, radiusH };
        float[] center = { centerX, centerY, centerZ };
        float[] corner = new float[EngineSetting.AXIS_COUNT];

        for (int face = 0; face < EngineSetting.SUB_VOXEL_FACE_COUNT; face++) {

            int axis = SubVoxelMeshUtility.resolveAxis(face);
            int uAxis = (axis + 1) % EngineSetting.AXIS_COUNT;
            int vAxis = (axis + 2) % EngineSetting.AXIS_COUNT;

            for (int vertex = 0; vertex < EngineSetting.QUAD_VERTEX_COUNT; vertex++) {

                corner[axis] = center[axis] + radius[axis] * SubVoxelMeshUtility.getFaceNormal(face, axis);
                corner[uAxis] = center[uAxis] + radius[uAxis] * (isHighU(vertex) ? 1f : -1f);
                corner[vAxis] = center[vAxis] + radius[vAxis] * (isHighV(vertex) ? 1f : -1f);

                out.add(corner[0]);
                out.add(corner[1]);
                out.add(corner[2]);
                out.add(cornerU);
                out.add(cornerV);
                out.add(face);
                out.add(color);
                out.add(center[0] - corner[0]);
                out.add(center[1] - corner[1]);
                out.add(center[2] - corner[2]);
                out.add(radiusH);
                out.add(radiusV);
                out.add(seed);
                out.add(0f);
                out.add(0f);
            }
        }
    }

    // Utility \\

    private static void addPadding(FloatArrayList out) {
        for (int slot = 0; slot < EngineSetting.TREE_WOOD_PADDING_FLOATS; slot++)
            out.add(0f);
    }
}
