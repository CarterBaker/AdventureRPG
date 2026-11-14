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
     * never be seen and is left out, so the twigs a crown hides cost nothing.
     * A leaf cluster is a sheaf of leaf cards through its centre: upright
     * cards turned evenly about it from a start its seed rolls, and one level
     * card across it. Each vertex carries the way to the cluster's centre, its
     * radii, its seed and its corner on the card, so the leaf shader cuts the
     * transparent sprigs out of each card and rounds the sheaf's light into a
     * dome, and the wind carries the whole sheaf as one. A coarser tier draws
     * its wood as boxes of bark rounded at their upright edges, capped where
     * their ends show. Positions are in blocks in the caller's frame.
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

        float[] corners = new float[EngineSetting.QUAD_VERTEX_COUNT * EngineSetting.AXIS_COUNT];
        float[] bounds = new float[EngineSetting.AXIS_COUNT * 2];
        int[] cell = new int[EngineSetting.AXIS_COUNT];

        for (int quad = 0; quad < quads.size(); quad++) {

            resolveWoodCorners(quads, quad, corners, bounds);

            if (isHidden(corners, bounds, hiders))
                continue;

            int part = quads.getPart(quad);
            TreeHandle treeHandle = partSpecies.get(part / EngineSetting.TREE_WOOD_PART_COUNT);
            int woodPart = part % EngineSetting.TREE_WOOD_PART_COUNT;
            float[] partCorners = treeHandle.getPartCorners();
            float cornerU = partCorners[woodPart * EngineSetting.TREE_PART_CORNER_FLOATS];
            float cornerV = partCorners[woodPart * EngineSetting.TREE_PART_CORNER_FLOATS + 1];
            float color = treeHandle.getPartColors()[woodPart];
            int face = quads.getFace(quad);
            float meta = face | resolveEdges(grid, quads, quad, cell) << EngineSetting.TREE_META_EDGE_SHIFT;

            for (int vertex = 0; vertex < EngineSetting.QUAD_VERTEX_COUNT; vertex++) {

                int corner = vertex * EngineSetting.AXIS_COUNT;
                float localU = isHighU(vertex) ? quads.getWidth(quad) : 0f;
                float localV = isHighV(vertex) ? quads.getHeight(quad) : 0f;

                out.add(originX + corners[corner] * SCALE);
                out.add(originY + corners[corner + 1] * SCALE);
                out.add(originZ + corners[corner + 2] * SCALE);
                out.add(cornerU);
                out.add(cornerV);
                out.add(meta);
                out.add(color);
                out.add(localU);
                out.add(localV);
                out.add(quads.getWidth(quad));
                out.add(quads.getHeight(quad));
                addPadding(out, EngineSetting.TREE_WOOD_PADDING_FLOATS);
            }
        }
    }

    // A box of one wood part, in blocks of the caller's frame — its four upright faces, rounded over at their
    // upright edges, and its top and foot as well when capped, every edge then rounding over; scratch holds the
    // box's corner, size and the corner being written, three floats each
    public static void emitWoodBox(
            float minX,
            float minY,
            float minZ,
            float maxX,
            float maxY,
            float maxZ,
            float cornerU,
            float cornerV,
            float color,
            boolean capped,
            float[] scratch,
            FloatArrayList out) {

        int min = 0;
        int size = EngineSetting.AXIS_COUNT;
        int corner = EngineSetting.AXIS_COUNT * 2;

        scratch[min] = minX;
        scratch[min + 1] = minY;
        scratch[min + 2] = minZ;
        scratch[size] = (maxX - minX) * EngineSetting.SUB_VOXEL_RESOLUTION;
        scratch[size + 1] = (maxY - minY) * EngineSetting.SUB_VOXEL_RESOLUTION;
        scratch[size + 2] = (maxZ - minZ) * EngineSetting.SUB_VOXEL_RESOLUTION;

        for (int face = 0; face < EngineSetting.SUB_VOXEL_FACE_COUNT; face++) {

            int axis = SubVoxelMeshUtility.resolveAxis(face);

            if (axis == EngineSetting.AXIS_Y && !capped)
                continue;

            int uAxis = (axis + 1) % EngineSetting.AXIS_COUNT;
            int vAxis = (axis + 2) % EngineSetting.AXIS_COUNT;
            float width = scratch[size + uAxis];
            float height = scratch[size + vAxis];
            int uEdge = resolveBoxEdge(uAxis, capped);
            int vEdge = resolveBoxEdge(vAxis, capped);
            int edges = uEdge
                    | uEdge << EngineSetting.TREE_EDGE_BITS
                    | vEdge << EngineSetting.TREE_EDGE_BITS * 2
                    | vEdge << EngineSetting.TREE_EDGE_BITS * 3;
            float meta = face | edges << EngineSetting.TREE_META_EDGE_SHIFT;

            scratch[corner + axis] = SubVoxelMeshUtility.getFaceNormal(face, axis) > 0 ? scratch[size + axis] : 0f;

            for (int vertex = 0; vertex < EngineSetting.QUAD_VERTEX_COUNT; vertex++) {

                float localU = isHighU(vertex) ? width : 0f;
                float localV = isHighV(vertex) ? height : 0f;

                scratch[corner + uAxis] = localU;
                scratch[corner + vAxis] = localV;

                out.add(scratch[min] + scratch[corner] * SCALE);
                out.add(scratch[min + 1] + scratch[corner + 1] * SCALE);
                out.add(scratch[min + 2] + scratch[corner + 2] * SCALE);
                out.add(cornerU);
                out.add(cornerV);
                out.add(meta);
                out.add(color);
                out.add(localU);
                out.add(localV);
                out.add(width);
                out.add(height);
                addPadding(out, EngineSetting.TREE_WOOD_PADDING_FLOATS);
            }
        }
    }

    // A box's edge running along an axis rounds over unless it runs upright to an open end
    private static int resolveBoxEdge(int axis, boolean capped) {
        return axis == EngineSetting.AXIS_Y && !capped ? EngineSetting.TREE_EDGE_FLAT : EngineSetting.TREE_EDGE_CONVEX;
    }

    // A quad's four corners in grid sub-voxels, three floats each, running low-low, high-low, high-high, low-high on
    // the face's axes, and the box they span, its low corner then its high
    private static void resolveWoodCorners(
            SubVoxelQuadListStruct quads,
            int quad,
            float[] corners,
            float[] bounds) {

        int axis = SubVoxelMeshUtility.resolveAxis(quads.getFace(quad));
        int uAxis = (axis + 1) % EngineSetting.AXIS_COUNT;
        int vAxis = (axis + 2) % EngineSetting.AXIS_COUNT;
        int lowU = quads.getU(quad);
        int lowV = quads.getV(quad);
        int highU = lowU + quads.getWidth(quad);
        int highV = lowV + quads.getHeight(quad);

        for (int vertex = 0; vertex < EngineSetting.QUAD_VERTEX_COUNT; vertex++) {

            int corner = vertex * EngineSetting.AXIS_COUNT;

            corners[corner + axis] = quads.getPlane(quad);
            corners[corner + uAxis] = isHighU(vertex) ? highU : lowU;
            corners[corner + vAxis] = isHighV(vertex) ? highV : lowV;
        }

        bounds[axis] = quads.getPlane(quad);
        bounds[uAxis] = lowU;
        bounds[vAxis] = lowV;
        bounds[EngineSetting.AXIS_COUNT + axis] = quads.getPlane(quad);
        bounds[EngineSetting.AXIS_COUNT + uAxis] = highU;
        bounds[EngineSetting.AXIS_COUNT + vAxis] = highV;
    }

    private static boolean isHighU(int vertex) {
        return vertex == 1 || vertex == 2;
    }

    private static boolean isHighV(int vertex) {
        return vertex >= 2;
    }

    // Edges \\

    // The four edges' kinds packed two bits each — low u, high u, low v, high v
    private static int resolveEdges(SubVoxelGridStruct grid, SubVoxelQuadListStruct quads, int quad, int[] cell) {

        int face = quads.getFace(quad);
        int axis = SubVoxelMeshUtility.resolveAxis(face);
        int normal = SubVoxelMeshUtility.getFaceNormal(face, axis);
        int solid = normal > 0 ? quads.getPlane(quad) - 1 : quads.getPlane(quad);
        int u = quads.getU(quad);
        int v = quads.getV(quad);
        int width = quads.getWidth(quad);
        int height = quads.getHeight(quad);

        int lowU = classifyEdge(grid, axis, solid, normal, true, u - 1, v, height, cell);
        int highU = classifyEdge(grid, axis, solid, normal, true, u + width, v, height, cell);
        int lowV = classifyEdge(grid, axis, solid, normal, false, v - 1, u, width, cell);
        int highV = classifyEdge(grid, axis, solid, normal, false, v + height, u, width, cell);

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
            int length,
            int[] cell) {

        int uAxis = (axis + 1) % EngineSetting.AXIS_COUNT;
        int vAxis = (axis + 2) % EngineSetting.AXIS_COUNT;
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

    // True when every corner of a quad lies inside one cluster — a cluster whose bounds fall short of the quad's
    // own is passed over before any corner is tried
    private static boolean isHidden(float[] corners, float[] bounds, FloatArrayList hiders) {

        float[] spheres = hiders.elements();
        int high = EngineSetting.AXIS_COUNT;

        for (int hider = 0; hider < hiders.size(); hider += EngineSetting.TREE_HIDER_FLOATS) {

            float centerX = spheres[hider];
            float centerY = spheres[hider + 1];
            float centerZ = spheres[hider + 2];
            float radiusH = spheres[hider + 3];
            float radiusV = spheres[hider + 4];

            if (bounds[0] <= centerX - radiusH || bounds[high] >= centerX + radiusH
                    || bounds[1] <= centerY - radiusV || bounds[high + 1] >= centerY + radiusV
                    || bounds[2] <= centerZ - radiusH || bounds[high + 2] >= centerZ + radiusH)
                continue;

            boolean inside = true;

            for (int corner = 0; corner < corners.length && inside; corner += EngineSetting.AXIS_COUNT) {

                float dx = (corners[corner] - centerX) / radiusH;
                float dy = (corners[corner + 1] - centerY) / radiusV;
                float dz = (corners[corner + 2] - centerZ) / radiusH;

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

    // The leaf cards of one cluster: its upright cards turned evenly about its centre from where its seed starts
    // them, each as wide as the cluster and as tall, then its level card across it
    public static void emitCluster(
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

        float turnStep = (float) Math.PI / EngineSetting.TREE_LEAF_UPRIGHT_CARDS;

        for (int card = 0; card < EngineSetting.TREE_LEAF_UPRIGHT_CARDS; card++) {

            float turn = (seed + card) * turnStep;
            float acrossX = (float) Math.cos(turn) * radiusH;
            float acrossZ = (float) Math.sin(turn) * radiusH;

            emitCard(
                    centerX, centerY, centerZ, radiusH, radiusV, acrossX, acrossZ, 0f, radiusV, 0f,
                    cornerU, cornerV, color, seed, card, out);
        }

        emitCard(
                centerX, centerY, centerZ, radiusH, radiusV, radiusH, 0f, 0f, 0f, radiusH,
                cornerU, cornerV, color, seed, EngineSetting.TREE_LEAF_UPRIGHT_CARDS, out);
    }

    // One card through the cluster's centre, reaching its level across half and its up half to either side
    private static void emitCard(
            float centerX,
            float centerY,
            float centerZ,
            float radiusH,
            float radiusV,
            float acrossX,
            float acrossZ,
            float upX,
            float upY,
            float upZ,
            float cornerU,
            float cornerV,
            int color,
            float seed,
            int card,
            FloatArrayList out) {

        for (int vertex = 0; vertex < EngineSetting.QUAD_VERTEX_COUNT; vertex++) {

            float cardU = isHighU(vertex) ? 1f : -1f;
            float cardV = isHighV(vertex) ? 1f : -1f;
            float offsetX = acrossX * cardU + upX * cardV;
            float offsetY = upY * cardV;
            float offsetZ = acrossZ * cardU + upZ * cardV;

            out.add(centerX + offsetX);
            out.add(centerY + offsetY);
            out.add(centerZ + offsetZ);
            out.add(cornerU);
            out.add(cornerV);
            out.add(card);
            out.add(color);
            out.add(-offsetX);
            out.add(-offsetY);
            out.add(-offsetZ);
            out.add(radiusH);
            out.add(radiusV);
            out.add(seed);
            out.add(cardU);
            out.add(cardV);
            addPadding(out, EngineSetting.TREE_LEAF_PADDING_FLOATS);
        }
    }

    // Utility \\

    private static void addPadding(FloatArrayList out, int slots) {
        for (int slot = 0; slot < slots; slot++)
            out.add(0f);
    }
}
