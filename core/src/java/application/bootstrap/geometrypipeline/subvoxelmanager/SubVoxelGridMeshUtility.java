package application.bootstrap.geometrypipeline.subvoxelmanager;

import java.util.Arrays;

import application.bootstrap.geometrypipeline.subvoxel.SubVoxelGridStruct;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelQuadListStruct;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.extras.Coordinate3Long;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongArrays;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

class SubVoxelGridMeshUtility extends EngineUtility {

    /*
     * The one greedy mesher every sub-voxel surface is built by: item models,
     * vehicles and trees alike. It sweeps one plane of sub-voxel faces at a
     * time across every block the plane crosses, keeps only faces open to an
     * empty cell or to a see-through part other than their own, and merges
     * each part's faces greedily across the whole plane, block boundaries
     * included, so a long plank or a tall trunk is one quad end to end. A
     * block wholly one part can only show faces where it meets another block,
     * so its inner slices are skipped outright, and two such blocks that hide
     * each other show nothing between them. Walls follow on their own planes:
     * a wall faces its open side, hides where cubes bury it on both sides, and
     * takes the place of a cube face it covers. Only blocks inside the emit
     * region make faces; the blocks around it are read to decide what is
     * exposed. Quads come out in sub-voxels, free of any vertex format.
     */

    private static final int RESOLUTION = EngineSetting.SUB_VOXEL_RESOLUTION;
    private static final int WALL_CELL_STRIDE = 3;
    private static final int PLANE_WORDS = RESOLUTION * RESOLUTION / Long.SIZE;
    private static final int BLOCK_PLANE_WORDS = EngineSetting.AXIS_COUNT * RESOLUTION * PLANE_WORDS;
    private static final long[] FULL_PLANES = createFullPlanes();
    private static final long[] EMPTY_PLANES = new long[BLOCK_PLANE_WORDS];

    // Mesh \\

    static void mesh(
            SubVoxelGridStruct grid,
            boolean[] partOpaque,
            int minBlockX,
            int minBlockY,
            int minBlockZ,
            int maxBlockX,
            int maxBlockY,
            int maxBlockZ,
            SubVoxelMeshAsyncContainer scratch,
            SubVoxelQuadListStruct out) {

        scratch.regionMin[EngineSetting.AXIS_X] = minBlockX;
        scratch.regionMin[EngineSetting.AXIS_Y] = minBlockY;
        scratch.regionMin[EngineSetting.AXIS_Z] = minBlockZ;
        scratch.regionMax[EngineSetting.AXIS_X] = maxBlockX;
        scratch.regionMax[EngineSetting.AXIS_Y] = maxBlockY;
        scratch.regionMax[EngineSetting.AXIS_Z] = maxBlockZ;
        releasePlanes(scratch);

        boolean byPlanes = !grid.hasWalls() && isAllOpaque(partOpaque);

        for (int axis = 0; axis < EngineSetting.AXIS_COUNT; axis++) {

            gatherLayers(grid, axis, scratch);

            for (int face = 0; face < EngineSetting.SUB_VOXEL_FACE_COUNT; face++) {

                if (SubVoxelMeshUtility.getFaceNormal(face, axis) == 0)
                    continue;

                for (int i = 0; i < scratch.layerOrder.size(); i++) {

                    int layer = scratch.layerOrder.getInt(i);

                    for (int slice = 0; slice < RESOLUTION; slice++)
                        meshPlane(grid, scratch.layer2Blocks.get(layer), face, axis, layer, slice, partOpaque,
                                byPlanes, scratch, out);
                }
            }

            releaseLayers(scratch);
        }

        if (grid.hasWalls())
            meshWalls(grid, scratch, out);
    }

    // Layers \\

    // Every block of the emit region by its block coordinate along the axis, in order
    private static void gatherLayers(SubVoxelGridStruct grid, int axis, SubVoxelMeshAsyncContainer scratch) {

        LongIterator blocks = grid.getBlocks().iterator();

        while (blocks.hasNext()) {

            long block = blocks.nextLong();

            scratch.blockScratch[EngineSetting.AXIS_X] = Coordinate3Long.unpackX(block);
            scratch.blockScratch[EngineSetting.AXIS_Y] = Coordinate3Long.unpackY(block);
            scratch.blockScratch[EngineSetting.AXIS_Z] = Coordinate3Long.unpackZ(block);

            if (!isInRegion(scratch.blockScratch, scratch))
                continue;

            int layer = scratch.blockScratch[axis];
            LongArrayList layerBlocks = scratch.layer2Blocks.get(layer);

            if (layerBlocks == null) {
                layerBlocks = scratch.spareBlockLists.isEmpty() ? new LongArrayList()
                        : scratch.spareBlockLists.pop();
                scratch.layer2Blocks.put(layer, layerBlocks);
                scratch.layerOrder.add(layer);
            }

            layerBlocks.add(block);
        }

        scratch.layerOrder.sort(null);
    }

    private static void releaseLayers(SubVoxelMeshAsyncContainer scratch) {

        for (LongArrayList layerBlocks : scratch.layer2Blocks.values()) {
            layerBlocks.clear();
            scratch.spareBlockLists.push(layerBlocks);
        }

        scratch.layer2Blocks.clear();
        scratch.layerOrder.clear();
    }

    private static boolean isInRegion(int[] block, SubVoxelMeshAsyncContainer scratch) {

        for (int axis = 0; axis < EngineSetting.AXIS_COUNT; axis++)
            if (block[axis] < scratch.regionMin[axis] || block[axis] > scratch.regionMax[axis])
                return false;

        return true;
    }

    // Plane \\

    // One slice of one layer of blocks facing one way: every exposed face gathered, then merged across the plane
    private static void meshPlane(
            SubVoxelGridStruct grid,
            LongArrayList blocks,
            int face,
            int axis,
            int layer,
            int slice,
            boolean[] partOpaque,
            boolean byPlanes,
            SubVoxelMeshAsyncContainer scratch,
            SubVoxelQuadListStruct out) {

        int normal = SubVoxelMeshUtility.getFaceNormal(face, axis);
        int uAxis = (axis + 1) % EngineSetting.AXIS_COUNT;
        int vAxis = (axis + 2) % EngineSetting.AXIS_COUNT;
        int boundarySlice = normal > 0 ? RESOLUTION - 1 : 0;
        int[] blockScratch = scratch.blockScratch;

        for (int i = 0; i < blocks.size(); i++) {

            long block = blocks.getLong(i);

            blockScratch[EngineSetting.AXIS_X] = Coordinate3Long.unpackX(block);
            blockScratch[EngineSetting.AXIS_Y] = Coordinate3Long.unpackY(block);
            blockScratch[EngineSetting.AXIS_Z] = Coordinate3Long.unpackZ(block);

            byte[] cells = grid.getBlockCells(
                    blockScratch[EngineSetting.AXIS_X], blockScratch[EngineSetting.AXIS_Y],
                    blockScratch[EngineSetting.AXIS_Z]);
            int uniformPart = grid.getUniformPart(cells);
            byte[] facing = cells;

            if (slice != boundarySlice && uniformPart != EngineSetting.INDEX_NOT_FOUND)
                continue;


            if (slice == boundarySlice) {

                facing = grid.getBlockCells(
                        blockScratch[EngineSetting.AXIS_X] + (axis == EngineSetting.AXIS_X ? normal : 0),
                        blockScratch[EngineSetting.AXIS_Y] + (axis == EngineSetting.AXIS_Y ? normal : 0),
                        blockScratch[EngineSetting.AXIS_Z] + (axis == EngineSetting.AXIS_Z ? normal : 0));

                if (uniformPart != EngineSetting.INDEX_NOT_FOUND
                        && hides(grid.getUniformPart(facing), uniformPart, partOpaque))
                    continue;
            }

            int[] mask = acquireMask(scratch);
            boolean exposed = byPlanes
                    ? fillMaskByPlanes(grid, block, cells, uniformPart, axis, normal, slice, scratch, mask)
                    : fillMask(grid, cells, facing, axis, normal, slice, partOpaque, scratch, mask);

            if (exposed) {

                long key = Coordinate2Long.pack(blockScratch[uAxis], blockScratch[vAxis]);

                scratch.planeMasks.put(key, mask);
                scratch.planeOrder.add(key);
            } else
                scratch.spareMasks.add(mask);
        }

        int plane = layer * RESOLUTION + (normal > 0 ? slice + 1 : slice);

        mergePlane(face, plane, scratch, out);
    }

    // Bit Planes \\

    // When every part is opaque and nothing is walled, a face shows exactly where a cell meets an empty one, so a
    // whole slice is decided by four words of its plane against the plane beyond it, and only the cells that show
    // are read
    private static boolean fillMaskByPlanes(
            SubVoxelGridStruct grid,
            long block,
            byte[] cells,
            int uniformPart,
            int axis,
            int normal,
            int slice,
            SubVoxelMeshAsyncContainer scratch,
            int[] mask) {

        long[] planes = resolvePlanes(block, cells, uniformPart, grid, scratch);
        int facingSlice = slice + normal;
        long[] facingPlanes = planes;

        if (facingSlice < 0 || facingSlice >= RESOLUTION) {

            int[] blockScratch = scratch.blockScratch;
            int neighbourX = blockScratch[EngineSetting.AXIS_X] + (axis == EngineSetting.AXIS_X ? normal : 0);
            int neighbourY = blockScratch[EngineSetting.AXIS_Y] + (axis == EngineSetting.AXIS_Y ? normal : 0);
            int neighbourZ = blockScratch[EngineSetting.AXIS_Z] + (axis == EngineSetting.AXIS_Z ? normal : 0);
            byte[] neighbourCells = grid.getBlockCells(neighbourX, neighbourY, neighbourZ);

            facingPlanes = neighbourCells == null
                    ? EMPTY_PLANES
                    : resolvePlanes(
                            Coordinate3Long.pack(neighbourX, neighbourY, neighbourZ), neighbourCells,
                            grid.getUniformPart(neighbourCells), grid, scratch);
            facingSlice = Math.floorMod(facingSlice, RESOLUTION);
        }

        int base = (axis * RESOLUTION + slice) * PLANE_WORDS;
        int facingBase = (axis * RESOLUTION + facingSlice) * PLANE_WORDS;
        long[] exposedWords = scratch.exposedScratch;
        long any = 0L;

        for (int word = 0; word < PLANE_WORDS; word++) {
            exposedWords[word] = planes[base + word] & ~facingPlanes[facingBase + word];
            any |= exposedWords[word];
        }

        if (any == 0L)
            return false;

        int strideU = resolveStride((axis + 1) % EngineSetting.AXIS_COUNT);
        int strideV = resolveStride((axis + 2) % EngineSetting.AXIS_COUNT);
        int cellBase = slice * resolveStride(axis);

        Arrays.fill(mask, EngineSetting.SUB_VOXEL_EMPTY_CELL);

        for (int word = 0; word < PLANE_WORDS; word++) {

            long bits = exposedWords[word];

            while (bits != 0L) {

                int bit = word * Long.SIZE + Long.numberOfTrailingZeros(bits);
                int a = bit % RESOLUTION;
                int b = bit / RESOLUTION;

                mask[bit] = cells[cellBase + b * strideV + a * strideU] & 0xFF;
                bits &= bits - 1L;
            }
        }

        return true;
    }

    // A block's cells as bit planes, worked out once per pass — every slice along each axis, its bits in the a, b
    // order the mask of that axis uses
    private static long[] resolvePlanes(
            long block,
            byte[] cells,
            int uniformPart,
            SubVoxelGridStruct grid,
            SubVoxelMeshAsyncContainer scratch) {

        if (uniformPart != EngineSetting.INDEX_NOT_FOUND)
            return FULL_PLANES;

        long[] planes = scratch.block2Planes.get(block);

        if (planes != null)
            return planes;

        planes = scratch.sparePlanes.isEmpty() ? new long[BLOCK_PLANE_WORDS] : scratch.sparePlanes.pop();
        Arrays.fill(planes, 0L);

        int index = 0;

        for (int z = 0; z < RESOLUTION; z++)
            for (int y = 0; y < RESOLUTION; y++)
                for (int x = 0; x < RESOLUTION; x++, index++) {

                    if (cells[index] == EngineSetting.SUB_VOXEL_EMPTY_CELL)
                        continue;

                    setPlaneBit(planes, EngineSetting.AXIS_X, x, y + z * RESOLUTION);
                    setPlaneBit(planes, EngineSetting.AXIS_Y, y, z + x * RESOLUTION);
                    setPlaneBit(planes, EngineSetting.AXIS_Z, z, x + y * RESOLUTION);
                }

        scratch.block2Planes.put(block, planes);

        return planes;
    }

    private static void setPlaneBit(long[] planes, int axis, int slice, int bit) {
        planes[(axis * RESOLUTION + slice) * PLANE_WORDS + bit / Long.SIZE] |= 1L << (bit % Long.SIZE);
    }

    private static void releasePlanes(SubVoxelMeshAsyncContainer scratch) {

        for (long[] planes : scratch.block2Planes.values())
            scratch.sparePlanes.push(planes);

        scratch.block2Planes.clear();
    }

    private static long[] createFullPlanes() {

        long[] planes = new long[BLOCK_PLANE_WORDS];

        Arrays.fill(planes, -1L);

        return planes;
    }

    private static boolean isAllOpaque(boolean[] partOpaque) {

        if (partOpaque == null)
            return true;

        for (int i = 0; i < partOpaque.length; i++)
            if (!partOpaque[i])
                return false;

        return true;
    }

    // Cells \\

    // Each cell of the block's slice holds its one-based part where its face shows, else empty — true when any
    // face shows. Cells are read by stride along the plane's two axes, so the sweep touches nothing but the cells
    private static boolean fillMask(
            SubVoxelGridStruct grid,
            byte[] cells,
            byte[] facing,
            int axis,
            int normal,
            int slice,
            boolean[] partOpaque,
            SubVoxelMeshAsyncContainer scratch,
            int[] mask) {

        int uAxis = (axis + 1) % EngineSetting.AXIS_COUNT;
        int vAxis = (axis + 2) % EngineSetting.AXIS_COUNT;
        int strideU = resolveStride(uAxis);
        int strideV = resolveStride(vAxis);
        int base = slice * resolveStride(axis);
        int facingBase = Math.floorMod(slice + normal, RESOLUTION) * resolveStride(axis);
        boolean walled = grid.hasWalls();
        boolean exposed = false;

        for (int b = 0; b < RESOLUTION; b++) {

            int row = b * strideV;

            for (int a = 0; a < RESOLUTION; a++) {

                int offset = row + a * strideU;
                int entry = cells[base + offset] & 0xFF;
                int maskIndex = a + b * RESOLUTION;

                mask[maskIndex] = EngineSetting.SUB_VOXEL_EMPTY_CELL;

                if (entry == EngineSetting.SUB_VOXEL_EMPTY_CELL)
                    continue;

                int facingEntry = facing == null ? EngineSetting.SUB_VOXEL_EMPTY_CELL
                        : facing[facingBase + offset] & 0xFF;

                if (facingEntry != EngineSetting.SUB_VOXEL_EMPTY_CELL
                        && hides(facingEntry - 1, entry - 1, partOpaque))
                    continue;

                if (walled && isFaceWalled(grid, axis, normal, slice, a, b, scratch))
                    continue;

                mask[maskIndex] = entry;
                exposed = true;
            }
        }

        return exposed;
    }

    // How far apart neighbouring cells along an axis lie in a block's cells
    private static int resolveStride(int axis) {
        return axis == EngineSetting.AXIS_X ? 1 : axis == EngineSetting.AXIS_Y ? RESOLUTION : RESOLUTION * RESOLUTION;
    }

    // True when a wall covers the face of the cell at a, b on the slice — a wall takes the place of the face
    private static boolean isFaceWalled(
            SubVoxelGridStruct grid,
            int axis,
            int normal,
            int slice,
            int a,
            int b,
            SubVoxelMeshAsyncContainer scratch) {

        int[] cellScratch = scratch.cellScratch;
        int[] blockScratch = scratch.blockScratch;

        cellScratch[(axis + 1) % EngineSetting.AXIS_COUNT] = a;
        cellScratch[(axis + 2) % EngineSetting.AXIS_COUNT] = b;
        cellScratch[axis] = slice;

        return grid.hasWall(
                axis,
                toFacePlane(blockScratch, cellScratch, axis, slice, normal, EngineSetting.AXIS_X),
                toFacePlane(blockScratch, cellScratch, axis, slice, normal, EngineSetting.AXIS_Y),
                toFacePlane(blockScratch, cellScratch, axis, slice, normal, EngineSetting.AXIS_Z));
    }

    // A grid coordinate of the plane a cell's face lies on, the plane's own coordinate along the face's axis
    private static int toFacePlane(int[] block, int[] cell, int axis, int slice, int normal, int component) {

        int inBlock = component == axis ? slice + (normal > 0 ? 1 : 0) : cell[component];

        return block[component] * RESOLUTION + inBlock;
    }

    // True when the facing part covers the face of the given part — a part never shows against itself, and an
    // opaque part hides everything behind it
    private static boolean hides(int facingPart, int part, boolean[] partOpaque) {

        if (facingPart == EngineSetting.INDEX_NOT_FOUND)
            return false;

        return facingPart == part || partOpaque == null || partOpaque[facingPart];
    }

    // Merge \\

    // Every mask of the plane, row by row, its faces grown as far across the plane as the same part reaches
    private static void mergePlane(
            int face,
            int plane,
            SubVoxelMeshAsyncContainer scratch,
            SubVoxelQuadListStruct out) {

        LongArrays.quickSort(
                scratch.planeOrder.elements(), 0, scratch.planeOrder.size(),
                SubVoxelGridMeshUtility::comparePlaneKeys);

        for (int i = 0; i < scratch.planeOrder.size(); i++)
            mergePlaneMask(scratch.planeOrder.getLong(i), face, plane, scratch, out);

        releasePlane(scratch);
    }

    // Blocks of a plane row by row, so faces grow along a row before they grow across rows
    private static int comparePlaneKeys(long first, long second) {

        int byRow = Integer.compare(Coordinate2Long.unpackY(first), Coordinate2Long.unpackY(second));

        return byRow != 0 ? byRow : Integer.compare(Coordinate2Long.unpackX(first), Coordinate2Long.unpackX(second));
    }

    private static void mergePlaneMask(
            long key,
            int face,
            int plane,
            SubVoxelMeshAsyncContainer scratch,
            SubVoxelQuadListStruct out) {

        int[] mask = scratch.planeMasks.get(key);
        int originU = Coordinate2Long.unpackX(key) * RESOLUTION;
        int originV = Coordinate2Long.unpackY(key) * RESOLUTION;

        for (int b = 0; b < RESOLUTION; b++)
            for (int a = 0; a < RESOLUTION; a++) {

                int value = mask[a + b * RESOLUTION];

                if (value == EngineSetting.SUB_VOXEL_EMPTY_CELL)
                    continue;

                int u = originU + a;
                int v = originV + b;
                int width = measureWidth(value, u, v, scratch);
                int height = measureHeight(value, u, v, width, scratch);

                for (int h = 0; h < height; h++)
                    for (int w = 0; w < width; w++)
                        setPlaneCell(u + w, v + h, EngineSetting.SUB_VOXEL_EMPTY_CELL, scratch);

                out.add(face, plane, u, v, width, height, value - 1);
            }
    }

    private static int measureWidth(int value, int u, int v, SubVoxelMeshAsyncContainer scratch) {

        int width = 1;

        while (getPlaneCell(u + width, v, scratch) == value)
            width++;

        return width;
    }

    private static int measureHeight(int value, int u, int v, int width, SubVoxelMeshAsyncContainer scratch) {

        int height = 1;

        while (true) {

            for (int w = 0; w < width; w++)
                if (getPlaneCell(u + w, v + height, scratch) != value)
                    return height;

            height++;
        }
    }

    // A cell of the plane in plane sub-voxels, empty where no block's mask reaches
    private static int getPlaneCell(int u, int v, SubVoxelMeshAsyncContainer scratch) {

        int[] mask = findPlaneMask(u, v, scratch);

        return mask == null ? EngineSetting.SUB_VOXEL_EMPTY_CELL
                : mask[Math.floorMod(u, RESOLUTION) + Math.floorMod(v, RESOLUTION) * RESOLUTION];
    }

    private static void setPlaneCell(int u, int v, int value, SubVoxelMeshAsyncContainer scratch) {
        findPlaneMask(u, v, scratch)[Math.floorMod(u, RESOLUTION) + Math.floorMod(v, RESOLUTION) * RESOLUTION] = value;
    }

    private static int[] findPlaneMask(int u, int v, SubVoxelMeshAsyncContainer scratch) {

        long key = Coordinate2Long.pack(Math.floorDiv(u, RESOLUTION), Math.floorDiv(v, RESOLUTION));

        if (key == scratch.lastMaskKey && scratch.lastMask != null)
            return scratch.lastMask;

        scratch.lastMaskKey = key;
        scratch.lastMask = scratch.planeMasks.get(key);

        return scratch.lastMask;
    }

    private static int[] acquireMask(SubVoxelMeshAsyncContainer scratch) {
        return scratch.spareMasks.isEmpty() ? new int[RESOLUTION * RESOLUTION] : scratch.spareMasks.pop();
    }

    // Every mask of the plane goes back to the spares for the next
    private static void releasePlane(SubVoxelMeshAsyncContainer scratch) {

        for (int i = 0; i < scratch.planeOrder.size(); i++)
            scratch.spareMasks.add(scratch.planeMasks.get(scratch.planeOrder.getLong(i)));

        scratch.planeMasks.clear();
        scratch.planeOrder.clear();
        scratch.lastMask = null;
    }

    // Walls \\

    // Every shown wall grouped by the face and plane it shows on, each group merged like a plane of cube faces
    private static void meshWalls(
            SubVoxelGridStruct grid,
            SubVoxelMeshAsyncContainer scratch,
            SubVoxelQuadListStruct out) {

        ObjectIterator<Long2ByteMap.Entry> walls = grid.getWalls();

        while (walls.hasNext()) {

            Long2ByteMap.Entry entry = walls.next();
            long wall = entry.getLongKey();

            scratch.cellScratch[EngineSetting.AXIS_X] = SubVoxelGridStruct.unpackWallX(wall);
            scratch.cellScratch[EngineSetting.AXIS_Y] = SubVoxelGridStruct.unpackWallY(wall);
            scratch.cellScratch[EngineSetting.AXIS_Z] = SubVoxelGridStruct.unpackWallZ(wall);

            for (int axis = 0; axis < EngineSetting.AXIS_COUNT; axis++)
                scratch.blockScratch[axis] = scratch.cellScratch[axis] / RESOLUTION;

            if (isInRegion(scratch.blockScratch, scratch))
                gatherWall(grid, SubVoxelGridStruct.unpackWallAxis(wall), entry.getByteValue() & 0xFF, scratch);
        }

        scratch.wallPlaneOrder.sort(null);

        for (int i = 0; i < scratch.wallPlaneOrder.size(); i++)
            meshWallPlane(scratch.wallPlaneOrder.getLong(i), scratch, out);

        for (IntArrayList cells : scratch.wallPlane2Cells.values()) {
            cells.clear();
            scratch.spareWallLists.push(cells);
        }

        scratch.wallPlane2Cells.clear();
        scratch.wallPlaneOrder.clear();
    }

    // The face a wall shows: toward its open side, positive when both are open, none when cubes bury it
    private static void gatherWall(
            SubVoxelGridStruct grid,
            int axis,
            int entry,
            SubVoxelMeshAsyncContainer scratch) {

        int[] cell = scratch.cellScratch;
        boolean frontOpen = !grid.isFilled(cell[0], cell[1], cell[2]);
        boolean backOpen = !grid.isFilled(
                cell[0] - (axis == EngineSetting.AXIS_X ? 1 : 0),
                cell[1] - (axis == EngineSetting.AXIS_Y ? 1 : 0),
                cell[2] - (axis == EngineSetting.AXIS_Z ? 1 : 0));

        if (!frontOpen && !backOpen)
            return;

        int face = SubVoxelMeshUtility.resolveFace(axis, frontOpen);
        long planeKey = Coordinate2Long.pack(face, cell[axis]);
        IntArrayList cells = scratch.wallPlane2Cells.get(planeKey);

        if (cells == null) {
            cells = scratch.spareWallLists.isEmpty() ? new IntArrayList() : scratch.spareWallLists.pop();
            scratch.wallPlane2Cells.put(planeKey, cells);
            scratch.wallPlaneOrder.add(planeKey);
        }

        cells.add(cell[(axis + 1) % EngineSetting.AXIS_COUNT]);
        cells.add(cell[(axis + 2) % EngineSetting.AXIS_COUNT]);
        cells.add(entry);
    }

    private static void meshWallPlane(
            long planeKey,
            SubVoxelMeshAsyncContainer scratch,
            SubVoxelQuadListStruct out) {

        IntArrayList cells = scratch.wallPlane2Cells.get(planeKey);

        for (int i = 0; i < cells.size(); i += WALL_CELL_STRIDE) {

            int u = cells.getInt(i);
            int v = cells.getInt(i + 1);
            long key = Coordinate2Long.pack(Math.floorDiv(u, RESOLUTION), Math.floorDiv(v, RESOLUTION));
            int[] mask = scratch.planeMasks.get(key);

            if (mask == null) {
                mask = acquireMask(scratch);
                Arrays.fill(mask, EngineSetting.SUB_VOXEL_EMPTY_CELL);
                scratch.planeMasks.put(key, mask);
                scratch.planeOrder.add(key);
            }

            mask[Math.floorMod(u, RESOLUTION) + Math.floorMod(v, RESOLUTION) * RESOLUTION] = cells.getInt(i + 2);
        }

        scratch.lastMask = null;
        mergePlane(Coordinate2Long.unpackX(planeKey), Coordinate2Long.unpackY(planeKey), scratch, out);
    }
}
