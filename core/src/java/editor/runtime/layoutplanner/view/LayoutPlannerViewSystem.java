package editor.runtime.layoutplanner.view;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelGridStruct;
import application.bootstrap.geometrypipeline.subvoxelmanager.SubVoxelManager;
import application.bootstrap.worldpipeline.architecture.ArchitectureHandle;
import application.bootstrap.worldpipeline.architecturemanager.ArchitectureManager;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.layout.LayoutLotStruct;
import application.bootstrap.worldpipeline.layout.LayoutPlanStruct;
import application.bootstrap.worldpipeline.layout.LayoutWallStruct;
import application.bootstrap.worldpipeline.road.RoadPathStruct;
import application.bootstrap.worldpipeline.settlementmanager.SettlementManager;
import application.bootstrap.worldpipeline.structure.StructureHandle;
import application.bootstrap.worldpipeline.util.StructurePlacementUtility;
import application.bootstrap.worldpipeline.util.SubBlockUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.world.WorldPlacementStruct;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import editor.bootstrap.layoutplannerpipeline.layoutplanner.LayoutPlannerNode;
import editor.bootstrap.layoutplannerpipeline.layoutplannermanager.LayoutPlannerManager;
import editor.runtime.layoutplanner.LayoutPlannerSetting;
import editor.runtime.viewcamera.ViewCameraSubject;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.extras.Direction3Vector;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class LayoutPlannerViewSystem extends SystemPackage implements ViewCameraSubject {

    /*
     * Plans the settlement the Layout Planner designs and lays out what the
     * preview shows of it. Once the design has rested a moment after any
     * change to it, its type or the world, a type and architecture the game
     * has registered are planned through
     * SettlementManager.planPlacement(), the same path generation takes, at
     * the site and numbered layout the planner names, in its architecture;
     * then every structure it stands on its lots, built in that architecture
     * and turned as it will stand, every road along its centreline at its
     * width and every curtain wall at its height and thickness, gates left
     * open, is rasterized into a sub-voxel grid with one part to each tile
     * and greedily meshed by SubVoxelManager, the old meshes freed. A stage
     * whose eye is shut in the Layers tab is left out. Positions are blocks
     * from the plan's low corner, so nothing lies below zero.
     */

    // Internal
    private LayoutPlannerManager layoutPlannerManager;
    private SettlementManager settlementManager;
    private ArchitectureManager architectureManager;
    private WorldManager worldManager;
    private BlockManager blockManager;
    private SubVoxelManager subVoxelManager;
    private MeshManager meshManager;

    // Meshing
    private SubVoxelGridStruct grid;
    private Int2IntOpenHashMap blockID2Part;
    private IntArrayList partTiles;

    // Plan — what was asked for last, and how long it has rested
    private int askedRevision;
    private int askedSettlementRevision;
    private WorldHandle askedWorld;
    private int restFrames;

    // Shown — the meshes, the low corner they are laid from and what the plan holds
    private ObjectArrayList<MeshInstance> meshes;
    private double originX;
    private double originZ;
    private int originY;
    private int maxX;
    private int maxY;
    private int maxZ;
    private boolean planned;
    private int lotCount;
    private int roadCount;
    private int revision;

    // Base \\

    @Override
    protected void create() {

        // Meshing
        this.grid = new SubVoxelGridStruct();
        this.blockID2Part = new Int2IntOpenHashMap();
        this.blockID2Part.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);
        this.partTiles = new IntArrayList();

        // Plan
        this.askedRevision = EngineSetting.INDEX_NOT_FOUND;
        this.askedSettlementRevision = EngineSetting.INDEX_NOT_FOUND;
        this.restFrames = EngineSetting.INDEX_NOT_FOUND;

        // Shown
        this.meshes = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.layoutPlannerManager = get(LayoutPlannerManager.class);
        this.settlementManager = get(SettlementManager.class);
        this.architectureManager = get(ArchitectureManager.class);
        this.worldManager = get(WorldManager.class);
        this.blockManager = get(BlockManager.class);
        this.subVoxelManager = get(SubVoxelManager.class);
        this.meshManager = get(MeshManager.class);
    }

    @Override
    protected void dispose() {
        releaseMeshes();
    }

    // Update \\

    @Override
    protected void update() {

        WorldHandle world = worldManager.getActiveWorld();
        int asked = layoutPlannerManager.getRevision();
        int settlements = settlementManager.getRevision();

        if (asked != askedRevision || settlements != askedSettlementRevision || world != askedWorld) {
            this.askedRevision = asked;
            this.askedSettlementRevision = settlements;
            this.askedWorld = world;
            this.restFrames = LayoutPlannerSetting.PLAN_REST_FRAMES;
            return;
        }

        if (restFrames < 0)
            return;

        if (restFrames-- == 0)
            replan(world);
    }

    // The design planned afresh and laid out, or nothing shown when there is nothing to plan
    private void replan(WorldHandle world) {

        releaseMeshes();
        this.planned = false;
        this.lotCount = 0;
        this.roadCount = 0;
        this.revision++;

        String settlementName = layoutPlannerManager.getSettlementName();
        String architectureName = layoutPlannerManager.getArchitectureName();

        if (world == null || settlementName == null || !settlementManager.hasSettlement(settlementName)
                || (!architectureName.isEmpty() && !architectureManager.hasArchitecture(architectureName)))
            return;

        WorldPlacementStruct placement = layoutPlannerManager.resolvePlacement(world.getPlacements());

        LayoutPlanStruct plan = settlementManager.planPlacement(world, placement).getLayout();

        layOut(plan);
        this.planned = true;
        this.lotCount = plan.getLots().length;
        this.roadCount = plan.getPaths().length;
    }

    private void layOut(LayoutPlanStruct plan) {

        grid.clear();
        blockID2Part.clear();
        partTiles.clear();

        int margin = LayoutPlannerSetting.MARGIN_BLOCKS;

        this.originX = Math.floor(plan.getMinX()) - margin;
        this.originZ = Math.floor(plan.getMinZ()) - margin;
        this.originY = resolveLowestY(plan) - margin;
        this.maxX = (int) Math.ceil(plan.getMaxX() - originX) + margin;
        this.maxZ = (int) Math.ceil(plan.getMaxZ() - originZ) + margin;
        this.maxY = 0;

        if (!layoutPlannerManager.isHidden(LayoutPlannerNode.STREETS))
            for (int i = 0; i < plan.getPaths().length; i++)
                layRoad(plan.getPaths()[i]);

        if (!layoutPlannerManager.isHidden(LayoutPlannerNode.WALL))
            for (int i = 0; i < plan.getWalls().length; i++)
                layWall(plan.getWalls()[i]);

        if (!layoutPlannerManager.isHidden(LayoutPlannerNode.LOTS))
            for (int i = 0; i < plan.getLots().length; i++)
                layLot(plan.getLots()[i], plan.getArchitectureHandle());

        if (!grid.isEmpty())
            meshes.addAll(subVoxelManager.createGridMeshes(grid, partTiles.toIntArray()));
    }

    // The lowest height anything the plan lays reaches
    private int resolveLowestY(LayoutPlanStruct plan) {

        int lowest = Integer.MAX_VALUE;

        for (int i = 0; i < plan.getLots().length; i++) {
            LayoutLotStruct lot = plan.getLots()[i];
            lowest = Math.min(lowest, lot.getAnchorY() + lot.getStructureHandle().getMinOffsetY());
        }

        for (int i = 0; i < plan.getPaths().length; i++)
            for (int point = 0; point < plan.getPaths()[i].getPointCount(); point++)
                lowest = Math.min(lowest, (int) Math.floor(plan.getPaths()[i].getPointY(point)));

        for (int i = 0; i < plan.getWalls().length; i++)
            for (int point = 0; point < plan.getWalls()[i].getCenterline().getPointCount(); point++)
                lowest = Math.min(lowest, (int) Math.floor(plan.getWalls()[i].getCenterline().getPointY(point)));

        return lowest == Integer.MAX_VALUE ? 0 : lowest;
    }

    // Every block a lot's structure lays, built in the plan's architecture and turned as it stands
    private void layLot(LayoutLotStruct lot, ArchitectureHandle architectureHandle) {

        StructureHandle structure = lot.getStructureHandle();
        int[] offsetX = structure.getBlockOffsetX();
        int[] offsetY = structure.getBlockOffsetY();
        int[] offsetZ = structure.getBlockOffsetZ();
        short[] blockIDs = structure.getBlockIDs();
        short[] blockSlotIDs = structure.getBlockSlotIDs();
        byte[] blockMasks = structure.getBlockMasks();
        int turns = lot.getQuarterTurns();
        long baseX = (long) Math.floor(lot.getPlanX() - originX);
        long baseZ = (long) Math.floor(lot.getPlanZ() - originZ);

        for (int i = 0; i < blockIDs.length; i++) {

            short blockID = architectureHandle != null && blockSlotIDs[i] != EngineSetting.REGISTRY_RESERVED_ID
                    ? architectureHandle.resolveBlockID(blockSlotIDs[i], blockIDs[i])
                    : blockIDs[i];

            layBlock(
                    (int) (baseX + StructurePlacementUtility.rotateX(offsetX[i], offsetZ[i], turns)),
                    lot.getAnchorY() + offsetY[i] - originY,
                    (int) (baseZ + StructurePlacementUtility.rotateZ(offsetX[i], offsetZ[i], turns)),
                    blockID,
                    StructurePlacementUtility.rotateMask(blockMasks[i] & EngineSetting.SUB_BLOCK_MASK_FULL, turns));
        }
    }

    // A road's surface laid a block thick along its centreline, as wide as the road
    private void layRoad(RoadPathStruct path) {

        short surface = path.getRoadHandle().getSurfaceBlockIDs()[0];
        int half = (int) Math.floor(path.getRoadHandle().getHalfWidthBlocks());

        walk(path, half, 1, surface, null);
    }

    // A curtain wall raised along its centreline, as thick and high as the architecture builds it, gates left open
    private void layWall(LayoutWallStruct wall) {

        int half = (int) Math.floor(wall.getWall().getHalfThicknessBlocks());

        walk(wall.getCenterline(), half, wall.getWall().getHeightBlocks(), wall.getWall().getWallBlockID(), wall);
    }

    // A square column swept a block at a time along a centreline, skipping a wall's gates
    private void walk(RoadPathStruct path, int half, int height, short blockID, LayoutWallStruct wall) {

        for (int point = 1; point < path.getPointCount(); point++) {

            double fromX = path.getPointX(point - 1);
            double fromZ = path.getPointZ(point - 1);
            double length = Math.hypot(path.getPointX(point) - fromX, path.getPointZ(point) - fromZ);
            int samples = Math.max(1, (int) Math.ceil(length));

            for (int sample = 0; sample < samples; sample++) {

                float share = (float) sample / samples;
                float along = path.getPointDistance(point - 1)
                        + share * (path.getPointDistance(point) - path.getPointDistance(point - 1));

                if (wall != null && wall.isGate(along))
                    continue;

                int x = (int) Math.floor(fromX + (path.getPointX(point) - fromX) * share - originX);
                int z = (int) Math.floor(fromZ + (path.getPointZ(point) - fromZ) * share - originZ);
                int y = (int) Math.floor(path.getPointY(point - 1)
                        + (path.getPointY(point) - path.getPointY(point - 1)) * share) - originY;

                for (int dz = -half; dz <= half; dz++)
                    for (int dx = -half; dx <= half; dx++)
                        for (int dy = 0; dy < height; dy++)
                            layBlock(x + dx, y + dy, z + dz, blockID, SubBlockUtility.MASK_FULL);
            }
        }
    }

    // One block rasterized, a partial one only the octants it fills; air and anything outside the grid left out
    private void layBlock(int x, int y, int z, short blockID, int mask) {

        if (x < 0 || y < 0 || z < 0)
            return;

        int part = resolvePart(blockID);

        if (part == EngineSetting.INDEX_NOT_FOUND)
            return;

        this.maxY = Math.max(maxY, y + 1);
        this.maxX = Math.max(maxX, x + 1);
        this.maxZ = Math.max(maxZ, z + 1);

        if (mask == SubBlockUtility.MASK_FULL) {
            grid.fillBlock(x, y, z, part, false);
            return;
        }

        int resolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        int halfCell = resolution / SubBlockUtility.DIVISIONS;

        for (int octant = 0; octant < SubBlockUtility.OCTANT_COUNT; octant++) {

            if (!SubBlockUtility.hasOctant(mask, octant))
                continue;

            int minX = x * resolution + SubBlockUtility.getOctantX(octant) * halfCell;
            int minY = y * resolution + SubBlockUtility.getOctantY(octant) * halfCell;
            int minZ = z * resolution + SubBlockUtility.getOctantZ(octant) * halfCell;

            grid.fillBox(minX, minY, minZ, minX + halfCell, minY + halfCell, minZ + halfCell, part, false);
        }
    }

    // The grid part a block is drawn as, given one the first time it is met; INDEX_NOT_FOUND for air or past the
    // parts a grid holds
    private int resolvePart(short blockID) {

        int part = blockID2Part.get(blockID);

        if (part != EngineSetting.INDEX_NOT_FOUND || partTiles.size() >= EngineSetting.SUB_VOXEL_MAX_PARTS)
            return part;

        BlockHandle blockHandle = blockManager.getBlockHandleFromBlockID(blockID);

        if (blockHandle.getGeometry() == DynamicGeometryType.NONE)
            return EngineSetting.INDEX_NOT_FOUND;

        part = partTiles.size();
        partTiles.add(blockHandle.getItemTextureName() != null
                ? subVoxelManager.resolveTile(blockHandle.getItemTextureName())
                : blockHandle.getTextureForFace(Direction3Vector.NORTH));
        blockID2Part.put(blockID, part);

        return part;
    }

    private void releaseMeshes() {

        for (int i = 0; i < meshes.size(); i++)
            meshManager.removeMesh(meshes.get(i));

        meshes.clear();
    }

    // Accessible \\

    @Override
    public boolean isShown() {
        return !meshes.isEmpty();
    }

    // The middle of what is shown on an axis, in blocks from its low corner
    @Override
    public float getCenter(int axis) {
        return switch (axis) {
            case EngineSetting.AXIS_X -> maxX * 0.5f;
            case EngineSetting.AXIS_Y -> maxY * 0.5f;
            default -> maxZ * 0.5f;
        };
    }

    // Half the diagonal of what is shown, in blocks, never smaller than a block
    @Override
    public float getBoundingRadius() {
        return Math.max(
                LayoutPlannerSetting.MIN_RADIUS_BLOCKS,
                0.5f * (float) Math.sqrt((double) maxX * maxX + (double) maxY * maxY + (double) maxZ * maxZ));
    }

    public ObjectArrayList<MeshInstance> getMeshes() {
        return meshes;
    }

    // Whether the last plan stood up a settlement, and what it holds
    public boolean isPlanned() {
        return planned;
    }

    public int getLotCount() {
        return lotCount;
    }

    public int getRoadCount() {
        return roadCount;
    }

    // Moves whenever the preview is planned again
    public int getRevision() {
        return revision;
    }
}
