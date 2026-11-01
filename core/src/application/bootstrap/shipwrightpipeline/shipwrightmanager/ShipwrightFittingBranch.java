package application.bootstrap.shipwrightpipeline.shipwrightmanager;

import application.bootstrap.furnishingpipeline.furnishing.FurnishingHandle;
import application.bootstrap.furnishingpipeline.furnishing.FurnishingSlotStruct;
import application.bootstrap.furnishingpipeline.furnishingmanager.FurnishingManager;
import application.bootstrap.furnishingpipeline.util.FurnishingArpgUtility;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelGridStruct;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.itemdefinition.ItemShapeStruct;
import application.bootstrap.itempipeline.itemrotationmanager.ItemRotationBufferSystem;
import application.bootstrap.lootpipeline.loottablemanager.LootTableManager;
import application.bootstrap.shipwrightpipeline.shipwright.ShipBlueprintStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipFurnitureStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipLayoutStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipOctantGridStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipPiece;
import application.bootstrap.shipwrightpipeline.util.ShipCellUtility;
import application.bootstrap.shipwrightpipeline.util.ShipRopeUtility;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartRole;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartStruct;
import application.bootstrap.vehiclepipeline.vehiclemanager.VehicleManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate3Long;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

class ShipwrightFittingBranch extends BranchPackage {

    /*
     * Fits a blueprint out. The rudder hangs off the stern post from the keel
     * up the stern, hinged at the post. The helm is a wheel turning across the
     * length on a two-posted stand at its place, and the capstan a drum, cap
     * and bars at its place, both standing on their deck's floor. A door or a
     * window is cut through the first wall, bulwark or planking it faces,
     * searching a little outward from where it was set down, a door full
     * height and a window at chest height; one cut through the hull's side
     * lowers the sill the sea comes aboard over. Every cannon fires through a
     * port of its own cut through the planking right before its muzzle, lidded,
     * never over a bulwark and never through a port another cannon shares.
     * Every hatch lid and port lid is a framed panel battened on its inner
     * face: a hatch lid lies flush in its hatchway, slats with gaps between
     * them, and swings up about its longer side; a port lid closes the port
     * from outside, one board set in from its frame, and swings out and up
     * about its top edge; both open and shut with Activate. Every
     * boarding ladder is rope and rungs hung clear of the side from the keel
     * to past the deck's edge, a step bridging it to the deck. Doors
     * and furniture are cargo, real items drawn from furnishing tables to be
     * taken up, set down and used like any other: each is turned the way it
     * faces, sized to the octants its largest item takes up and centred in
     * them, standing on open floor. Once every part is built, every item each
     * place could be furnished with must fit clear of every solid sub-voxel
     * and of every other place, exactly as a vehicle stows its cargo, so a
     * ship that compiles always furnishes.
     */

    // Internal
    private ShipwrightHullBranch shipwrightHullBranch;
    private FurnishingManager furnishingManager;
    private LootTableManager lootTableManager;
    private ItemRotationBufferSystem itemRotationBufferSystem;
    private VehicleManager vehicleManager;

    // Scratch — main thread only
    private int[] footprint;
    private LongOpenHashSet claimedCells;
    private int[] panelMin;
    private int[] panelMax;
    private int[] boxMin;
    private int[] boxMax;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.footprint = new int[EngineSetting.AXIS_COUNT];
        this.claimedCells = new LongOpenHashSet();
        this.panelMin = new int[EngineSetting.AXIS_COUNT];
        this.panelMax = new int[EngineSetting.AXIS_COUNT];
        this.boxMin = new int[EngineSetting.AXIS_COUNT];
        this.boxMax = new int[EngineSetting.AXIS_COUNT];
    }

    @Override
    protected void get() {
        this.shipwrightHullBranch = get(ShipwrightHullBranch.class);
        this.furnishingManager = get(FurnishingManager.class);
        this.lootTableManager = get(LootTableManager.class);
        this.itemRotationBufferSystem = get(ItemRotationBufferSystem.class);
        this.vehicleManager = get(VehicleManager.class);
    }

    // Openings \\

    // Every door and window cut through the wall or side it is set on, and every cannon's port, before the hull is
    // merged
    void cutOpenings(ShipBlueprintStruct blueprint, ShipLayoutStruct layout) {

        IntArrayList doors = blueprint.getDoors();
        IntArrayList windows = blueprint.getWindows();

        for (int i = 0; i < doors.size(); i++) {

            int cell = doors.getInt(i);

            resolveFootprint(EngineSetting.SHIP_DOOR_FURNISHING, ShipCellUtility.unpackSpin(cell));
            cutOpening(layout, cell, 1, footprint[EngineSetting.AXIS_Y], footprint[EngineSetting.AXIS_X],
                    footprint[EngineSetting.AXIS_Z], false);
        }

        for (int i = 0; i < windows.size(); i++)
            cutOpening(layout, windows.getInt(i), EngineSetting.SHIP_WINDOW_SILL, EngineSetting.SHIP_WINDOW_HEIGHT,
                    1, 1, true);

        cutPorts(blueprint, layout);
    }

    // An opening a footprint across, rising from a height over its deck's floor, cut through the wall or planking
    // it is set on and only through that, in every column of its footprint the wall runs through; a window is
    // glazed across the octants it cut
    private void cutOpening(
            ShipLayoutStruct layout,
            int cell,
            int sill,
            int height,
            int sizeX,
            int sizeZ,
            boolean glazed) {

        ShipOctantGridStruct grid = layout.getGrid();
        int floor = ShipCellUtility.unpackLevel(cell) * EngineSetting.SHIP_DECK_HEIGHT;
        int spin = ShipCellUtility.unpackSpin(cell);
        int startX = layout.getOriginX() + ShipCellUtility.unpackX(cell);
        int startZ = ShipCellUtility.unpackZ(cell);
        ShipPiece wall = grid.get(startX, floor + sill, startZ);

        if (wall == null || !wall.isCuttable()) {
            layout.reportBuildProblem(EngineSetting.SHIPWRIGHT_PROBLEM_OPENING_WALL);
            return;
        }

        for (int y = floor + sill; y < floor + sill + height; y++)
            for (int z = startZ; z < startZ + sizeZ; z++)
                for (int x = startX; x < startX + sizeX; x++) {

                    if (shipwrightHullBranch.isFloor(grid, x, y, z)) {
                        layout.reportBuildProblem(EngineSetting.SHIPWRIGHT_PROBLEM_OPENING_HEIGHT);
                        return;
                    }

                    cutThrough(layout, x, y, z, ShipCellUtility.stepX(spin), ShipCellUtility.stepZ(spin), wall);
                }

        if (glazed)
            layout.addPane(startX, floor + sill, startZ, startX + sizeX, floor + sill + height, startZ + sizeZ, spin);
    }

    // Walks across a wall from an octant along a step, clearing it for as long as it stays the piece the opening
    // was set on, so an opening never reaches past the wall it is cut through
    private void cutThrough(ShipLayoutStruct layout, int x, int y, int z, int stepX, int stepZ, ShipPiece wall) {

        ShipOctantGridStruct grid = layout.getGrid();

        for (int reach = 0; reach <= EngineSetting.SHIP_OPENING_REACH; reach++) {

            int atX = x + stepX * reach;
            int atZ = z + stepZ * reach;

            if (grid.get(atX, y, atZ) != wall)
                return;

            if (wall != ShipPiece.WALLS)
                layout.lowerOpeningSill(y * EngineSetting.SUB_BLOCK_SIZE);

            grid.clear(atX, y, atZ);
        }
    }

    // Every cannon cuts a lidded port of its own through the planking right before its muzzle
    private void cutPorts(ShipBlueprintStruct blueprint, ShipLayoutStruct layout) {

        for (int i = 0; i < blueprint.getFurniture().size(); i++) {

            ShipFurnitureStruct piece = blueprint.getFurniture().get(i);

            if (!piece.getFurnishingName().equals(EngineSetting.SHIP_CANNON_FURNISHING)
                    || !furnishingManager.hasFurnishing(piece.getFurnishingName()))
                continue;

            cutPort(layout, piece.getCell());
        }
    }

    private void cutPort(ShipLayoutStruct layout, int cell) {

        ShipOctantGridStruct grid = layout.getGrid();
        int spin = ShipCellUtility.unpackSpin(cell);
        int floor = ShipCellUtility.unpackLevel(cell) * EngineSetting.SHIP_DECK_HEIGHT;
        int startX = layout.getOriginX() + ShipCellUtility.unpackX(cell);
        int startZ = ShipCellUtility.unpackZ(cell);

        resolveFootprint(EngineSetting.SHIP_CANNON_FURNISHING, spin);

        int sizeX = footprint[EngineSetting.AXIS_X];
        int sizeZ = footprint[EngineSetting.AXIS_Z];
        int span = ShipCellUtility.stepX(spin) != 0 ? sizeZ : sizeX;
        int rowMinX = resolveMuzzleX(startX, sizeX, spin, 0);
        int rowMinZ = resolveMuzzleZ(startZ, sizeZ, spin, 0);
        int rowMaxX = resolveMuzzleX(startX, sizeX, spin, span - 1) + 1;
        int rowMaxZ = resolveMuzzleZ(startZ, sizeZ, spin, span - 1) + 1;

        if (touchesPort(layout, rowMinX, floor + 1, rowMinZ, rowMaxX, rowMaxZ)) {
            layout.reportBuildProblem(EngineSetting.SHIPWRIGHT_PROBLEM_CANNON_SHARED);
            return;
        }

        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        boolean planking = false;

        for (int along = 0; along < span; along++) {

            int x = resolveMuzzleX(startX, sizeX, spin, along);
            int z = resolveMuzzleZ(startZ, sizeZ, spin, along);

            for (int y = floor + 1; y <= floor + EngineSetting.SHIP_GUN_PORT_HEIGHT; y++) {

                ShipPiece side = grid.get(x, y, z);

                if (side != ShipPiece.HULL && side != ShipPiece.BULWARK)
                    continue;

                if (side == ShipPiece.HULL) {
                    planking = true;
                    layout.lowerOpeningSill(y * EngineSetting.SUB_BLOCK_SIZE);
                }

                grid.clear(x, y, z);
                minX = Math.min(minX, x);
                minZ = Math.min(minZ, z);
                maxX = Math.max(maxX, x + 1);
                maxZ = Math.max(maxZ, z + 1);
            }
        }

        if (!planking) {
            layout.reportBuildProblem(EngineSetting.SHIPWRIGHT_PROBLEM_CANNON_PORT);
            return;
        }

        layout.addPort(minX, floor + 1, minZ, maxX, floor + 1 + EngineSetting.SHIP_GUN_PORT_HEIGHT, maxZ, spin);
    }

    // True when the row before a muzzle, at a port's sill, overlaps or touches a port already cut, so two cannons would
    // fire through one opening
    private boolean touchesPort(ShipLayoutStruct layout, int minX, int minY, int minZ, int maxX, int maxZ) {

        IntArrayList boxes = layout.getPortBoxes();

        for (int i = 0; i < ShipLayoutStruct.countFittings(boxes); i++)
            if (ShipLayoutStruct.getFittingBound(boxes, i, EngineSetting.BOX_MIN_Y) == minY
                    && minX <= ShipLayoutStruct.getFittingBound(boxes, i, EngineSetting.BOX_MAX_X)
                    && maxX >= ShipLayoutStruct.getFittingBound(boxes, i, EngineSetting.BOX_MIN_X)
                    && minZ <= ShipLayoutStruct.getFittingBound(boxes, i, EngineSetting.BOX_MAX_Z)
                    && maxZ >= ShipLayoutStruct.getFittingBound(boxes, i, EngineSetting.BOX_MIN_Z))
                return true;

        return false;
    }

    // One octant of the row right before a turned footprint's muzzle edge
    private int resolveMuzzleX(int startX, int sizeX, int spin, int along) {

        int stepX = ShipCellUtility.stepX(spin);

        if (stepX > 0)
            return startX + sizeX;

        return stepX < 0 ? startX - 1 : startX + along;
    }

    private int resolveMuzzleZ(int startZ, int sizeZ, int spin, int along) {

        int stepZ = ShipCellUtility.stepZ(spin);

        if (stepZ > 0)
            return startZ + sizeZ;

        return stepZ < 0 ? startZ - 1 : startZ + along;
    }

    // The quarter turn a door or window set on a wall faces: across the wall's run, toward the side the turn given
    // already favours, or outboard of the centre line when it runs along the length; a door's item turns from it
    // through resolveItemSpin()
    int resolveOpeningSpin(ShipLayoutStruct layout, int deck, int x, int z, int sill, int spin) {

        ShipOctantGridStruct grid = layout.getGrid();
        int gridX = layout.getOriginX() + x;
        int y = deck * EngineSetting.SHIP_DECK_HEIGHT + sill;
        ShipPiece wall = grid.get(gridX, y, z);
        boolean runsAlong = grid.get(gridX - 1, y, z) == wall || grid.get(gridX + 1, y, z) == wall;
        boolean runsAcross = grid.get(gridX, y, z - 1) == wall || grid.get(gridX, y, z + 1) == wall;

        if (runsAcross && !runsAlong)
            return ShipCellUtility.stepX(spin) != 0 ? spin : 0;

        if (ShipCellUtility.stepZ(spin) != 0)
            return spin;

        return z >= EngineSetting.SHIP_SHEET_HALF_BREADTH ? 1 : EngineSetting.ENCODED_FACE_SPIN_COUNT - 1;
    }

    // True when a door or window could be set at an octant: on a wall, bulwark or planking at its height
    boolean isOpeningWall(ShipLayoutStruct layout, int deck, int x, int z, int sill) {

        ShipPiece piece = layout.getGrid().get(
                layout.getOriginX() + x, deck * EngineSetting.SHIP_DECK_HEIGHT + sill, z);

        return piece != null && piece.isCuttable();
    }

    // Fittings \\

    void addFittings(ShipBlueprintStruct blueprint, ShipLayoutStruct layout) {

        if (blueprint.hasHelm())
            addHelm(layout, blueprint.getHelm());

        if (blueprint.hasCapstan())
            addCapstan(layout, blueprint.getCapstan());

        addRudder(layout);
        addAnchors(layout);
        addHatchLids(layout);
        addPortLids(layout);
        addLadders(layout);
        addBoardingLadders(layout);
        addPanes(layout);
    }

    // Lids \\

    // A lid lying flush in every hatchway, hinged along its longer side, swinging up and over
    private void addHatchLids(ShipLayoutStruct layout) {

        IntArrayList boxes = layout.getHatchBoxes();
        int octant = EngineSetting.SHIP_OCTANT_SUB_VOXELS;
        float angle = (float) Math.toRadians(EngineSetting.SHIP_HATCH_OPEN_DEGREES);

        for (int i = 0; i < ShipLayoutStruct.countFittings(boxes); i++) {

            int hinge = resolveHingeSpin(boxes, i);
            boolean acrossX = ShipCellUtility.stepX(hinge) != 0;
            int top = ShipLayoutStruct.getFittingBound(boxes, i, EngineSetting.BOX_MAX_Y) * octant;

            readFittingBox(boxes, i);
            panelMin[EngineSetting.AXIS_Y] = top - EngineSetting.SHIP_LID_THICKNESS_SUB_VOXELS;
            panelMax[EngineSetting.AXIS_Y] = top;

            VehiclePartStruct lid = createLid(EngineSetting.SHIP_PART_HATCH + (i + 1), EngineSetting.SHIP_TEXTURE_DECK,
                    hinge, top, acrossX ? EngineSetting.AXIS_Z : EngineSetting.AXIS_X,
                    (hinge == 1 || hinge == 2 ? 1f : -1f) * angle);

            addPanel(lid, EngineSetting.AXIS_Y, true, acrossX ? EngineSetting.AXIS_X : EngineSetting.AXIS_Z, true);
            layout.addPart(lid);
        }
    }

    // The side of a hatch's box its lid is hinged along: one of its two longer sides, the one its quarter turn
    // favours, or the side it faces when the box is square
    int resolveHingeSpin(IntArrayList boxes, int fittingIndex) {

        int spin = ShipLayoutStruct.getFittingBound(boxes, fittingIndex, EngineSetting.SHIP_FITTING_SPIN);
        int sizeX = ShipLayoutStruct.getFittingBound(boxes, fittingIndex, EngineSetting.BOX_MAX_X)
                - ShipLayoutStruct.getFittingBound(boxes, fittingIndex, EngineSetting.BOX_MIN_X);
        int sizeZ = ShipLayoutStruct.getFittingBound(boxes, fittingIndex, EngineSetting.BOX_MAX_Z)
                - ShipLayoutStruct.getFittingBound(boxes, fittingIndex, EngineSetting.BOX_MIN_Z);

        if (sizeX > sizeZ && ShipCellUtility.stepX(spin) != 0)
            return spin == 0 ? 1 : EngineSetting.ENCODED_FACE_SPIN_COUNT - 1;

        if (sizeZ > sizeX && ShipCellUtility.stepZ(spin) != 0)
            return spin == 1 ? 0 : 2;

        return spin;
    }

    // A lid over every gun port cut through the planking, closing it from outside, hinged along its top outer edge,
    // swinging out and up
    private void addPortLids(ShipLayoutStruct layout) {

        IntArrayList boxes = layout.getPortBoxes();
        int octant = EngineSetting.SHIP_OCTANT_SUB_VOXELS;
        int thickness = EngineSetting.SHIP_LID_THICKNESS_SUB_VOXELS;
        float angle = (float) Math.toRadians(EngineSetting.SHIP_PORT_OPEN_DEGREES);

        for (int i = 0; i < ShipLayoutStruct.countFittings(boxes); i++) {

            int spin = ShipLayoutStruct.getFittingBound(boxes, i, EngineSetting.SHIP_FITTING_SPIN);
            boolean alongX = ShipCellUtility.stepX(spin) != 0;
            boolean outward = ShipCellUtility.stepX(spin) > 0 || ShipCellUtility.stepZ(spin) > 0;
            int normal = alongX ? EngineSetting.AXIS_X : EngineSetting.AXIS_Z;
            int top = ShipLayoutStruct.getFittingBound(boxes, i, EngineSetting.BOX_MAX_Y) * octant;

            readFittingBox(boxes, i);

            if (outward)
                panelMin[normal] = panelMax[normal] - thickness;
            else
                panelMax[normal] = panelMin[normal] + thickness;

            VehiclePartStruct lid = createLid(EngineSetting.SHIP_PART_PORT_LID + (i + 1),
                    EngineSetting.SHIP_TEXTURE_WALL, spin, top, alongX ? EngineSetting.AXIS_Z : EngineSetting.AXIS_X,
                    (spin == 0 || spin == EngineSetting.ENCODED_FACE_SPIN_COUNT - 1 ? 1f : -1f) * angle);

            addPanel(lid, normal, outward, EngineSetting.AXIS_Y, false);
            layout.addPart(lid);
        }
    }

    // A fitting's octant box in sub-voxels, written to the panel scratch
    private void readFittingBox(IntArrayList boxes, int fittingIndex) {

        int octant = EngineSetting.SHIP_OCTANT_SUB_VOXELS;

        for (int axis = 0; axis < EngineSetting.AXIS_COUNT; axis++) {
            panelMin[axis] = ShipLayoutStruct.getFittingBound(boxes, fittingIndex, EngineSetting.BOX_MIN_X + axis)
                    * octant;
            panelMax[axis] = ShipLayoutStruct.getFittingBound(boxes, fittingIndex, EngineSetting.BOX_MAX_X + axis)
                    * octant;
        }
    }

    // A door part hinged at a height along the edge of the panel scratch a quarter turn faces
    private VehiclePartStruct createLid(
            String partName,
            String textureName,
            int hinge,
            int hingeY,
            int axis,
            float openAngle) {

        float scale = 1f / EngineSetting.SUB_VOXEL_RESOLUTION;
        int stepX = ShipCellUtility.stepX(hinge);
        int stepZ = ShipCellUtility.stepZ(hinge);
        float hingeX = stepX > 0 ? panelMax[EngineSetting.AXIS_X]
                : stepX < 0 ? panelMin[EngineSetting.AXIS_X]
                        : (panelMin[EngineSetting.AXIS_X] + panelMax[EngineSetting.AXIS_X]) * 0.5f;
        float hingeZ = stepZ > 0 ? panelMax[EngineSetting.AXIS_Z]
                : stepZ < 0 ? panelMin[EngineSetting.AXIS_Z]
                        : (panelMin[EngineSetting.AXIS_Z] + panelMax[EngineSetting.AXIS_Z]) * 0.5f;

        return new VehiclePartStruct(
                partName, VehiclePartRole.DOOR, textureName, EngineSetting.SHIP_LINK_NONE,
                new Vector3(hingeX * scale, hingeY * scale, hingeZ * scale), axis, openAngle);
    }

    // The panel scratch built as a lid a thickness along its normal: a frame round its rim, set in from the outer face
    // between it either slats running along an axis and parted by gaps or one board, and two battens across them on
    // the inner face
    private void addPanel(VehiclePartStruct part, int normal, boolean outerPositive, int runAxis, boolean slatted) {

        int across = EngineSetting.AXIS_COUNT * (EngineSetting.AXIS_COUNT - 1) / 2 - normal - runAxis;
        int frame = EngineSetting.SHIP_LID_FRAME_SUB_VOXELS;
        int recess = EngineSetting.SHIP_LID_RECESS_SUB_VOXELS;
        int batten = EngineSetting.SHIP_LID_BATTEN_SUB_VOXELS;
        int pitch = slatted
                ? EngineSetting.SHIP_LID_SLAT_SUB_VOXELS + EngineSetting.SHIP_LID_GAP_SUB_VOXELS
                : panelMax[across] - panelMin[across];
        int runLength = panelMax[runAxis] - panelMin[runAxis];

        addPanelBox(part, across, panelMin[across], panelMin[across] + frame, normal, 0, 0);
        addPanelBox(part, across, panelMax[across] - frame, panelMax[across], normal, 0, 0);
        addPanelBox(part, runAxis, panelMin[runAxis], panelMin[runAxis] + frame, across, frame, frame);
        addPanelBox(part, runAxis, panelMax[runAxis] - frame, panelMax[runAxis], across, frame, frame);

        for (int slat = panelMin[across] + frame; slat < panelMax[across] - frame; slat += pitch) {

            resetPanelBox();
            boxMin[across] = slat;
            boxMax[across] = slatted
                    ? Math.min(slat + EngineSetting.SHIP_LID_SLAT_SUB_VOXELS, panelMax[across] - frame)
                    : panelMax[across] - frame;
            boxMin[runAxis] += frame;
            boxMax[runAxis] -= frame;

            if (outerPositive)
                boxMax[normal] -= recess;
            else
                boxMin[normal] += recess;

            part.addBox(boxMin[0], boxMin[1], boxMin[2], boxMax[0], boxMax[1], boxMax[2]);
        }

        for (int quarter = 1; quarter < EngineSetting.SHIP_LID_BATTEN_COUNT * 2; quarter += 2) {

            int middle = panelMin[runAxis] + runLength * quarter / (EngineSetting.SHIP_LID_BATTEN_COUNT * 2);

            resetPanelBox();
            boxMin[runAxis] = middle - batten / 2;
            boxMax[runAxis] = boxMin[runAxis] + batten;
            boxMin[across] += frame;
            boxMax[across] -= frame;

            if (outerPositive) {
                boxMax[normal] = panelMin[normal];
                boxMin[normal] = panelMin[normal] - batten;
            } else {
                boxMin[normal] = panelMax[normal];
                boxMax[normal] = panelMax[normal] + batten;
            }

            part.addBox(boxMin[0], boxMin[1], boxMin[2], boxMax[0], boxMax[1], boxMax[2]);
        }
    }

    // One strip of the panel scratch between two bounds on an axis, the whole thickness of the panel, trimmed at both
    // ends of another axis
    private void addPanelBox(VehiclePartStruct part, int axis, int min, int max, int trimAxis, int trimMin,
            int trimMax) {

        resetPanelBox();
        boxMin[axis] = min;
        boxMax[axis] = max;
        boxMin[trimAxis] += trimMin;
        boxMax[trimAxis] -= trimMax;

        part.addBox(boxMin[0], boxMin[1], boxMin[2], boxMax[0], boxMax[1], boxMax[2]);
    }

    private void resetPanelBox() {
        System.arraycopy(panelMin, 0, boxMin, 0, EngineSetting.AXIS_COUNT);
        System.arraycopy(panelMax, 0, boxMax, 0, EngineSetting.AXIS_COUNT);
    }

    // Ladders \\

    // Every ladder a pair of rails at its sides and a sheet of rungs across the edge it faces, all one climbable part
    private void addLadders(ShipLayoutStruct layout) {

        IntArrayList boxes = layout.getLadderBoxes();

        if (ShipLayoutStruct.countFittings(boxes) == 0)
            return;

        VehiclePartStruct ladders = new VehiclePartStruct(
                EngineSetting.SHIP_PART_LADDERS, VehiclePartRole.LADDER, EngineSetting.SHIP_TEXTURE_RUNGS,
                EngineSetting.SHIP_LINK_NONE, new Vector3(), EngineSetting.AXIS_Y, 0f);
        VehiclePartStruct rails = new VehiclePartStruct(
                EngineSetting.SHIP_PART_LADDER_RAILS, VehiclePartRole.LADDER, EngineSetting.SHIP_TEXTURE_SPAR,
                EngineSetting.SHIP_LINK_NONE, new Vector3(), EngineSetting.AXIS_Y, 0f);

        for (int i = 0; i < ShipLayoutStruct.countFittings(boxes); i++)
            addLadder(boxes, i, ladders, rails);

        layout.addPart(rails);
        layout.addPart(ladders);
    }

    private void addLadder(IntArrayList boxes, int ladderIndex, VehiclePartStruct ladders, VehiclePartStruct rails) {

        int octant = EngineSetting.SHIP_OCTANT_SUB_VOXELS;
        int rail = EngineSetting.SHIP_LADDER_RAIL_SUB_VOXELS;
        int inset = EngineSetting.SHIP_LADDER_INSET_SUB_VOXELS;
        int spin = ShipLayoutStruct.getFittingBound(boxes, ladderIndex, EngineSetting.SHIP_FITTING_SPIN);
        int minX = ShipLayoutStruct.getFittingBound(boxes, ladderIndex, EngineSetting.BOX_MIN_X) * octant;
        int minY = ShipLayoutStruct.getFittingBound(boxes, ladderIndex, EngineSetting.BOX_MIN_Y) * octant;
        int minZ = ShipLayoutStruct.getFittingBound(boxes, ladderIndex, EngineSetting.BOX_MIN_Z) * octant;
        int maxX = ShipLayoutStruct.getFittingBound(boxes, ladderIndex, EngineSetting.BOX_MAX_X) * octant;
        int maxY = ShipLayoutStruct.getFittingBound(boxes, ladderIndex, EngineSetting.BOX_MAX_Y) * octant;
        int maxZ = ShipLayoutStruct.getFittingBound(boxes, ladderIndex, EngineSetting.BOX_MAX_Z) * octant;
        int stepX = ShipCellUtility.stepX(spin);
        int stepZ = ShipCellUtility.stepZ(spin);

        if (stepX != 0) {

            int face = stepX > 0 ? maxX - inset : minX + inset;
            int back = stepX > 0 ? face - rail : face;

            rails.addBox(back, minY, minZ, back + rail, maxY, minZ + rail);
            rails.addBox(back, minY, maxZ - rail, back + rail, maxY, maxZ);
            ladders.addQuad(
                    stepX > 0 ? EngineSetting.SUB_VOXEL_FACE_POSITIVE_X : EngineSetting.SUB_VOXEL_FACE_NEGATIVE_X,
                    face, minY, maxZ, face, minY, minZ, face, maxY, minZ, face, maxY, maxZ);
            return;
        }

        int face = stepZ > 0 ? maxZ - inset : minZ + inset;
        int back = stepZ > 0 ? face - rail : face;

        rails.addBox(minX, minY, back, minX + rail, maxY, back + rail);
        rails.addBox(maxX - rail, minY, back, maxX, maxY, back + rail);
        ladders.addQuad(
                stepZ > 0 ? EngineSetting.SUB_VOXEL_FACE_POSITIVE_Z : EngineSetting.SUB_VOXEL_FACE_NEGATIVE_Z,
                minX, minY, face, maxX, minY, face, maxX, maxY, face, minX, maxY, face);
    }

    // Boarding Ladders \\

    // Every boarding ladder rungs between two ropes hung clear of the side from the keel up past the deck's edge, and a
    // step bridging its head to the deck, its rungs and ropes climbable
    private void addBoardingLadders(ShipLayoutStruct layout) {

        IntArrayList boxes = layout.getBoardingLadderBoxes();

        if (ShipLayoutStruct.countFittings(boxes) == 0)
            return;

        VehiclePartStruct rungs = new VehiclePartStruct(
                EngineSetting.SHIP_PART_BOARDING_LADDERS, VehiclePartRole.LADDER, EngineSetting.SHIP_TEXTURE_RUNGS,
                EngineSetting.SHIP_LINK_NONE, new Vector3(), EngineSetting.AXIS_Y, 0f);
        VehiclePartStruct ropes = new VehiclePartStruct(
                EngineSetting.SHIP_PART_BOARDING_ROPES, VehiclePartRole.LADDER, EngineSetting.SHIP_TEXTURE_ROPE,
                EngineSetting.SHIP_LINK_NONE, new Vector3(), EngineSetting.AXIS_Y, 0f);
        VehiclePartStruct steps = new VehiclePartStruct(
                EngineSetting.SHIP_PART_BOARDING_STEPS, VehiclePartRole.STRUCTURE, EngineSetting.SHIP_TEXTURE_DECK,
                EngineSetting.SHIP_LINK_NONE, new Vector3(), EngineSetting.AXIS_Y, 0f);

        for (int i = 0; i < ShipLayoutStruct.countFittings(boxes); i++)
            addBoardingLadder(boxes, i, rungs, ropes, steps);

        layout.addPart(steps);
        layout.addPart(ropes);
        layout.addPart(rungs);
    }

    private void addBoardingLadder(
            IntArrayList boxes,
            int ladderIndex,
            VehiclePartStruct rungs,
            VehiclePartStruct ropes,
            VehiclePartStruct steps) {

        int octant = EngineSetting.SHIP_OCTANT_SUB_VOXELS;
        int step = EngineSetting.SHIP_BOARDING_STEP_SUB_VOXELS;
        int inset = EngineSetting.SHIP_LADDER_INSET_SUB_VOXELS;
        boolean starboard = ShipLayoutStruct.getFittingBound(boxes, ladderIndex, EngineSetting.SHIP_FITTING_SPIN) == 1;
        int minX = ShipLayoutStruct.getFittingBound(boxes, ladderIndex, EngineSetting.BOX_MIN_X) * octant;
        int minY = ShipLayoutStruct.getFittingBound(boxes, ladderIndex, EngineSetting.BOX_MIN_Y) * octant;
        int minZ = ShipLayoutStruct.getFittingBound(boxes, ladderIndex, EngineSetting.BOX_MIN_Z) * octant;
        int maxX = ShipLayoutStruct.getFittingBound(boxes, ladderIndex, EngineSetting.BOX_MAX_X) * octant;
        int maxY = ShipLayoutStruct.getFittingBound(boxes, ladderIndex, EngineSetting.BOX_MAX_Y) * octant;
        int maxZ = ShipLayoutStruct.getFittingBound(boxes, ladderIndex, EngineSetting.BOX_MAX_Z) * octant;
        int deckTop = maxY - EngineSetting.SHIP_LADDER_HANDHOLD * octant;
        int edge = starboard ? minZ + octant : maxZ - octant;
        int reach = starboard
                ? Math.max(maxZ + EngineSetting.SHIP_BOARDING_OUTBOARD_SUB_VOXELS, edge + step)
                : Math.min(minZ - EngineSetting.SHIP_BOARDING_OUTBOARD_SUB_VOXELS, edge - step);
        int face = Math.max(0, Math.min(EngineSetting.SHIP_SHEET_BREADTH * octant, reach));

        if (face != edge)
            steps.addBox(minX, deckTop - EngineSetting.SHIP_LID_THICKNESS_SUB_VOXELS, Math.min(edge, face),
                    maxX, deckTop, Math.max(edge, face));

        rungs.addQuad(
                starboard ? EngineSetting.SUB_VOXEL_FACE_POSITIVE_Z : EngineSetting.SUB_VOXEL_FACE_NEGATIVE_Z,
                minX + inset, minY, face, maxX - inset, minY, face, maxX - inset, maxY, face, minX + inset, maxY,
                face);
        ShipRopeUtility.addRope(ropes, minX + inset, minY, face, minX + inset, maxY, face);
        ShipRopeUtility.addRope(ropes, maxX - inset, minY, face, maxX - inset, maxY, face);
    }

    // Glass \\

    // A pane of glass a sub-voxel thick across the middle of every window, solid to whoever leans on it
    private void addPanes(ShipLayoutStruct layout) {

        IntArrayList boxes = layout.getPaneBoxes();

        if (ShipLayoutStruct.countFittings(boxes) == 0)
            return;

        int octant = EngineSetting.SHIP_OCTANT_SUB_VOXELS;
        int half = octant / 2;
        VehiclePartStruct glass = new VehiclePartStruct(
                EngineSetting.SHIP_PART_GLASS, VehiclePartRole.STRUCTURE, EngineSetting.SHIP_TEXTURE_GLASS,
                EngineSetting.SHIP_LINK_NONE, new Vector3(), EngineSetting.AXIS_Y, 0f);

        for (int i = 0; i < ShipLayoutStruct.countFittings(boxes); i++) {

            boolean acrossX = ShipCellUtility.stepX(
                    ShipLayoutStruct.getFittingBound(boxes, i, EngineSetting.SHIP_FITTING_SPIN)) != 0;
            int minX = ShipLayoutStruct.getFittingBound(boxes, i, EngineSetting.BOX_MIN_X) * octant;
            int minY = ShipLayoutStruct.getFittingBound(boxes, i, EngineSetting.BOX_MIN_Y) * octant;
            int minZ = ShipLayoutStruct.getFittingBound(boxes, i, EngineSetting.BOX_MIN_Z) * octant;
            int maxX = ShipLayoutStruct.getFittingBound(boxes, i, EngineSetting.BOX_MAX_X) * octant;
            int maxY = ShipLayoutStruct.getFittingBound(boxes, i, EngineSetting.BOX_MAX_Y) * octant;
            int maxZ = ShipLayoutStruct.getFittingBound(boxes, i, EngineSetting.BOX_MAX_Z) * octant;

            if (acrossX)
                glass.addBox(minX + half, minY, minZ, minX + half + 1, maxY, maxZ);
            else
                glass.addBox(minX, minY, minZ + half, maxX, maxY, minZ + half + 1);
        }

        layout.addPart(glass);
    }

    // Anchors \\

    // As many anchors as the hull's length carries, each hung at a cathead outboard of the side near the bow, then
    // the quarters, its cable let out below it only while it is down
    private void addAnchors(ShipLayoutStruct layout) {

        int center = EngineSetting.SHIP_SHEET_HALF_BREADTH;
        int stern = findHullEnd(layout, center, false);
        int stem = findHullEnd(layout, center, true);

        if (stern == EngineSetting.INDEX_NOT_FOUND)
            return;

        int length = stem - stern + 1;
        int count = Math.max(1, Math.min(EngineSetting.SHIP_MAX_ANCHORS,
                Math.round(length * EngineSetting.SUB_BLOCK_SIZE / EngineSetting.SHIP_ANCHOR_LENGTH_PER_ANCHOR)));
        int setback = Math.max(1, Math.round(length * EngineSetting.SHIP_ANCHOR_SETBACK_SHARE));

        layout.setAnchorDrop(layout.getSectionLayers() * EngineSetting.SUB_BLOCK_SIZE
                + EngineSetting.SHIP_ANCHOR_BELOW_KEEL);

        for (int anchor = 0; anchor < count; anchor++)
            addAnchor(layout, anchor, anchor < 2 ? stem - setback : stern + setback, anchor % 2 == 0);
    }

    // One anchor hung outboard of the side at an octant along the sheet, and the cable it pays out as it drops; an
    // anchor never reaches past the edge of the sheet or below the keel, so a hull drawn full breadth or a single
    // deck high still carries one within its model grid
    private void addAnchor(ShipLayoutStruct layout, int anchorIndex, int x, boolean starboard) {

        int octant = EngineSetting.SHIP_OCTANT_SUB_VOXELS;
        int center = EngineSetting.SHIP_SHEET_HALF_BREADTH;
        int halfBeam = shipwrightHullBranch.findWidestSection(layout, x);
        int sideZ = starboard ? ShipCellUtility.toStarboard(halfBeam - 1) : ShipCellUtility.toPort(halfBeam - 1);
        int top = shipwrightHullBranch.findColumnTop(layout, x, sideZ);

        if (halfBeam == 0 || top == EngineSetting.SHIP_LEVEL_NONE)
            top = shipwrightHullBranch.findColumnTop(layout, x, center);

        int arm = EngineSetting.SHIP_ANCHOR_ARM_SUB_VOXELS;
        int fluke = EngineSetting.SHIP_ANCHOR_FLUKE_SUB_VOXELS;
        int cathead = Math.max((top + 1) * octant, fluke);
        int middleX = (layout.getOriginX() + x) * octant + octant / 2;
        int outboard = EngineSetting.SHIP_ANCHOR_OUTBOARD_SUB_VOXELS;
        int middleZ = Math.max(fluke, Math.min(EngineSetting.SHIP_SHEET_BREADTH * octant - fluke, starboard
                ? (center + halfBeam) * octant + outboard
                : (center - halfBeam) * octant - outboard));
        int crown = Math.max(0, cathead - EngineSetting.SHIP_ANCHOR_SHANK_SUB_VOXELS);

        VehiclePartStruct anchor = new VehiclePartStruct(
                EngineSetting.SHIP_PART_ANCHOR + (anchorIndex + 1), VehiclePartRole.ANCHOR,
                EngineSetting.SHIP_TEXTURE_IRON, EngineSetting.SHIP_LINK_NONE, new Vector3(), EngineSetting.AXIS_Y, 0f);
        anchor.addBox(middleX - 1, crown, middleZ - 1, middleX + 1, cathead, middleZ + 1);
        anchor.addBox(middleX - arm, crown, middleZ - 1, middleX + arm, crown + 2, middleZ + 1);
        anchor.addBox(middleX - arm, crown, middleZ - 1, middleX - arm + 2, crown + fluke, middleZ + 1);
        anchor.addBox(middleX + arm - 2, crown, middleZ - 1, middleX + arm, crown + fluke, middleZ + 1);
        anchor.addBox(middleX - 1, cathead - 2, middleZ - fluke, middleX + 1, cathead, middleZ + fluke);

        int drop = Math.round(layout.getAnchorDrop() * EngineSetting.SUB_VOXEL_RESOLUTION);
        VehiclePartStruct cable = new VehiclePartStruct(
                EngineSetting.SHIP_PART_CABLE + (anchorIndex + 1), VehiclePartRole.CABLE,
                EngineSetting.SHIP_TEXTURE_ROPE, EngineSetting.SHIP_LINK_NONE, new Vector3(), EngineSetting.AXIS_Y, 0f);
        ShipRopeUtility.addRope(cable, middleX, cathead + 2, middleZ, middleX, cathead - drop, middleZ);

        layout.addPart(anchor);
        layout.addPart(cable);
    }

    // The aftmost or foremost octant along the sheet the hull reaches on a line along its length
    private int findHullEnd(ShipLayoutStruct layout, int z, boolean forward) {

        for (int i = 0; i < EngineSetting.SHIP_SHEET_LENGTH; i++) {

            int x = forward ? EngineSetting.SHIP_SHEET_LENGTH - 1 - i : i;

            if (shipwrightHullBranch.findColumnTop(layout, x, z) != EngineSetting.SHIP_LEVEL_NONE)
                return x;
        }

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // A wheel turning across the length on a two-posted stand, its stand a fitting of it
    private void addHelm(ShipLayoutStruct layout, int cell) {

        if (!isOnFloor(layout, cell, 1, EngineSetting.SHIP_FITTING_OCTANTS)) {
            layout.reportBuildProblem(EngineSetting.SHIPWRIGHT_PROBLEM_HELM_FLOOR);
            return;
        }

        int octant = EngineSetting.SHIP_OCTANT_SUB_VOXELS;
        int radius = EngineSetting.SHIP_WHEEL_RADIUS;
        int reach = EngineSetting.SHIP_WHEEL_HANDLE_REACH;
        int floor = (ShipCellUtility.unpackLevel(cell) * EngineSetting.SHIP_DECK_HEIGHT + 1) * octant;
        int centerX = (layout.getOriginX() + ShipCellUtility.unpackX(cell)) * octant + octant / 2;
        int centerY = floor + EngineSetting.SHIP_WHEEL_HEIGHT;
        int centerZ = (ShipCellUtility.unpackZ(cell) + 1) * octant;
        int wheelX = centerX + 1;
        int wheelEndX = wheelX + EngineSetting.SHIP_WHEEL_THICKNESS;
        float scale = 1f / EngineSetting.SUB_VOXEL_RESOLUTION;

        VehiclePartStruct stand = new VehiclePartStruct(
                EngineSetting.SHIP_PART_WHEEL_STAND, VehiclePartRole.STRUCTURE, EngineSetting.SHIP_TEXTURE_DECK,
                EngineSetting.SHIP_PART_WHEEL, new Vector3(), EngineSetting.AXIS_Y, 0f);
        stand.addBox(centerX - 1, floor, centerZ - radius - 2, centerX + 1, centerY + 1, centerZ - radius);
        stand.addBox(centerX - 1, floor, centerZ + radius, centerX + 1, centerY + 1, centerZ + radius + 2);
        stand.addBox(centerX - 1, centerY - 1, centerZ - radius, centerX + 1, centerY + 1, centerZ + radius);

        VehiclePartStruct wheel = new VehiclePartStruct(
                EngineSetting.SHIP_PART_WHEEL, VehiclePartRole.HELM, EngineSetting.SHIP_TEXTURE_SPAR,
                EngineSetting.SHIP_LINK_NONE,
                new Vector3((wheelX + wheelEndX) * 0.5f * scale, centerY * scale, centerZ * scale),
                EngineSetting.AXIS_X, 0f);
        wheel.addBox(wheelX, centerY + radius - 1, centerZ - radius + 3, wheelEndX, centerY + radius,
                centerZ + radius - 3);
        wheel.addBox(wheelX, centerY - radius, centerZ - radius + 3, wheelEndX, centerY - radius + 1,
                centerZ + radius - 3);
        wheel.addBox(wheelX, centerY - radius + 3, centerZ - radius, wheelEndX, centerY + radius - 3,
                centerZ - radius + 1);
        wheel.addBox(wheelX, centerY - radius + 3, centerZ + radius - 1, wheelEndX, centerY + radius - 3,
                centerZ + radius);
        wheel.addBox(wheelX, centerY + radius - 3, centerZ - radius + 1, wheelEndX, centerY + radius - 1,
                centerZ - radius + 3);
        wheel.addBox(wheelX, centerY + radius - 3, centerZ + radius - 3, wheelEndX, centerY + radius - 1,
                centerZ + radius - 1);
        wheel.addBox(wheelX, centerY - radius + 1, centerZ - radius + 1, wheelEndX, centerY - radius + 3,
                centerZ - radius + 3);
        wheel.addBox(wheelX, centerY - radius + 1, centerZ + radius - 3, wheelEndX, centerY - radius + 3,
                centerZ + radius - 1);
        wheel.addBox(wheelX, centerY - reach, centerZ - 1, wheelEndX, centerY + reach, centerZ + 1);
        wheel.addBox(wheelX, centerY - 1, centerZ - reach, wheelEndX, centerY + 1, centerZ + reach);

        layout.addPart(stand);
        layout.addPart(wheel);
    }

    // A drum under a broad cap, two bars crossing over it
    private void addCapstan(ShipLayoutStruct layout, int cell) {

        int size = EngineSetting.SHIP_FITTING_OCTANTS;

        if (!isOnFloor(layout, cell, size, size)) {
            layout.reportBuildProblem(EngineSetting.SHIPWRIGHT_PROBLEM_CAPSTAN_FLOOR);
            return;
        }

        int octant = EngineSetting.SHIP_OCTANT_SUB_VOXELS;
        int minX = (layout.getOriginX() + ShipCellUtility.unpackX(cell)) * octant;
        int minZ = ShipCellUtility.unpackZ(cell) * octant;
        int floor = (ShipCellUtility.unpackLevel(cell) * EngineSetting.SHIP_DECK_HEIGHT + 1) * octant;
        int span = size * octant;
        int middleX = minX + span / 2;
        int middleZ = minZ + span / 2;
        int drum = EngineSetting.SHIP_CAPSTAN_DRUM_HEIGHT;
        int bar = EngineSetting.SHIP_CAPSTAN_BAR_REACH;

        VehiclePartStruct capstan = new VehiclePartStruct(
                EngineSetting.SHIP_PART_CAPSTAN, VehiclePartRole.CAPSTAN, EngineSetting.SHIP_TEXTURE_SPAR,
                EngineSetting.SHIP_LINK_NONE, new Vector3(), EngineSetting.AXIS_Y, 0f);
        capstan.addBox(minX + 3, floor, minZ + 3, minX + span - 3, floor + drum, minZ + span - 3);
        capstan.addBox(minX + 1, floor + drum, minZ + 1, minX + span - 1, floor + drum + 2, minZ + span - 1);
        capstan.addBox(middleX - bar, floor + drum + 2, middleZ - 1, middleX + bar, floor + drum + 3, middleZ + 1);
        capstan.addBox(middleX - 1, floor + drum + 2, middleZ - bar, middleX + 1, floor + drum + 3, middleZ + bar);

        layout.addPart(capstan);
    }

    // A blade off the stern post from the keel up the stern, no higher than a deck over the keel, hinged at the post
    private void addRudder(ShipLayoutStruct layout) {

        int center = EngineSetting.SHIP_SHEET_HALF_BREADTH;
        int post = findSternPost(layout, center);

        if (post == EngineSetting.INDEX_NOT_FOUND)
            return;

        int octant = EngineSetting.SHIP_OCTANT_SUB_VOXELS;
        int keel = shipwrightHullBranch.findKeel(layout, post, center);
        int top = Math.min(keel + EngineSetting.SHIP_DECK_HEIGHT,
                shipwrightHullBranch.findColumnTop(layout, post, center) + 1 + EngineSetting.SHIP_BULWARK_HEIGHT);
        int hingeX = (layout.getOriginX() + post) * octant;
        int middleZ = center * octant;
        float scale = 1f / EngineSetting.SUB_VOXEL_RESOLUTION;

        VehiclePartStruct rudder = new VehiclePartStruct(
                EngineSetting.SHIP_PART_RUDDER, VehiclePartRole.RUDDER, EngineSetting.SHIP_TEXTURE_HULL,
                EngineSetting.SHIP_LINK_NONE,
                new Vector3(hingeX * scale, keel * octant * scale, middleZ * scale),
                EngineSetting.AXIS_Y, 0f);
        rudder.addBox(
                hingeX - EngineSetting.SHIP_RUDDER_DEPTH * octant,
                keel * octant,
                middleZ - EngineSetting.SHIP_RUDDER_HALF_THICKNESS,
                hingeX,
                top * octant,
                middleZ + EngineSetting.SHIP_RUDDER_HALF_THICKNESS);

        layout.addPart(rudder);
    }

    // The aftmost octant along the sheet the hull reaches on the centre line within a deck of its bottom
    private int findSternPost(ShipLayoutStruct layout, int center) {

        for (int x = 0; x < EngineSetting.SHIP_SHEET_LENGTH; x++)
            for (int y = 0; y < Math.min(layout.getSectionLayers(), EngineSetting.SHIP_DECK_HEIGHT); y++)
                if (layout.isVolume(x, y, center))
                    return x;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // True when every octant under a fitting's footprint is floor
    private boolean isOnFloor(ShipLayoutStruct layout, int cell, int sizeX, int sizeZ) {

        int level = ShipCellUtility.unpackLevel(cell) * EngineSetting.SHIP_DECK_HEIGHT;
        int startX = layout.getOriginX() + ShipCellUtility.unpackX(cell);
        int startZ = ShipCellUtility.unpackZ(cell);

        for (int z = startZ; z < startZ + sizeZ; z++)
            for (int x = startX; x < startX + sizeX; x++)
                if (!shipwrightHullBranch.isFloor(layout.getGrid(), x, level, z)
                        || !layout.getGrid().isEmpty(x, level + 1, z))
                    return false;

        return true;
    }

    // Cargo \\

    // Every door and piece of furniture as a furnished place, every piece on open floor, then every place checked to
    // fit
    void addCargo(ShipBlueprintStruct blueprint, ShipLayoutStruct layout) {

        IntArrayList doors = blueprint.getDoors();

        for (int i = 0; i < doors.size(); i++)
            addPlace(blueprint, layout, EngineSetting.SHIP_DOOR_FURNISHING, doors.getInt(i));

        for (int i = 0; i < blueprint.getFurniture().size(); i++) {

            ShipFurnitureStruct piece = blueprint.getFurniture().get(i);

            if (!furnishingManager.hasFurnishing(piece.getFurnishingName())) {
                layout.reportBuildProblem(
                        EngineSetting.SHIPWRIGHT_PROBLEM_FURNISHING_UNKNOWN + piece.getFurnishingName());
                continue;
            }

            resolveFootprint(piece.getFurnishingName(), ShipCellUtility.unpackSpin(piece.getCell()));

            if (!isOnFloor(layout, piece.getCell(), footprint[EngineSetting.AXIS_X],
                    footprint[EngineSetting.AXIS_Z])) {
                layout.reportBuildProblem(EngineSetting.SHIPWRIGHT_PROBLEM_FURNITURE_FLOOR);
                continue;
            }

            addPlace(blueprint, layout, piece.getFurnishingName(), piece.getCell());
        }

        verifyCargo(layout);
    }

    // A furnished place standing on its deck's floor, centred in the octants its largest item takes up
    private void addPlace(ShipBlueprintStruct blueprint, ShipLayoutStruct layout, String furnishingName, int cell) {

        int spin = ShipCellUtility.unpackSpin(cell);
        int itemSpin = resolveItemSpin(furnishingName, spin);
        int orientation = EngineSetting.DEFAULT_BLOCK_ORIENTATION + itemSpin;
        ItemShapeStruct shape = furnishingManager.getFurnishingHandleFromFurnishingName(furnishingName)
                .getItems()[0].getShape();
        int octant = EngineSetting.SHIP_OCTANT_SUB_VOXELS;

        resolveFootprint(furnishingName, spin);

        int minX = (layout.getOriginX() + ShipCellUtility.unpackX(cell)) * octant;
        int minY = (ShipCellUtility.unpackLevel(cell) * EngineSetting.SHIP_DECK_HEIGHT + 1) * octant;
        int minZ = ShipCellUtility.unpackZ(cell) * octant;
        int maxX = minX + footprint[EngineSetting.AXIS_X] * octant;
        int maxY = minY + footprint[EngineSetting.AXIS_Y] * octant;
        int maxZ = minZ + footprint[EngineSetting.AXIS_Z] * octant;

        FurnishingSlotStruct slot = FurnishingArpgUtility.createSlot(
                blueprint.getDisplayName(),
                furnishingName,
                resolveCorner(shape, orientation, EngineSetting.AXIS_X, minX, maxX),
                minY - itemRotationBufferSystem.getShapeMin(shape, orientation, EngineSetting.AXIS_Y),
                resolveCorner(shape, orientation, EngineSetting.AXIS_Z, minZ, maxZ),
                itemSpin,
                1f,
                EngineSetting.LOOT_TABLE_NAME_NONE,
                furnishingManager,
                lootTableManager);

        layout.addCargo(slot, minX, minY, minZ, maxX, maxY, maxZ);
    }

    // The quarter turn a furnishing's items take to face the way a cell faces: items turn the other way round from
    // the sheet's quarter turns, and a door, whose face looks along its model's breadth, a quarter turn further, so
    // a door set across a wall lies in it
    int resolveItemSpin(String furnishingName, int spin) {

        int turn = furnishingName.equals(EngineSetting.SHIP_DOOR_FURNISHING) ? EngineSetting.SHIP_DOOR_FACING_TURN : 0;

        return ShipCellUtility.toItemSpin(spin + turn);
    }

    // The corner that centres a turned shape between two bounds on one axis
    private int resolveCorner(ItemShapeStruct shape, int orientation, int axis, int min, int max) {

        int low = itemRotationBufferSystem.getShapeMin(shape, orientation, axis);
        int high = itemRotationBufferSystem.getShapeMax(shape, orientation, axis);

        return min + (max - min - (high - low + 1)) / 2 - low;
    }

    // The octants a furnishing's largest item takes up on each axis once turned, written to the footprint
    void resolveFootprint(String furnishingName, int spin) {

        int orientation = EngineSetting.DEFAULT_BLOCK_ORIENTATION + resolveItemSpin(furnishingName, spin);
        int octant = EngineSetting.SHIP_OCTANT_SUB_VOXELS;
        ItemDefinitionHandle[] items = furnishingManager.getFurnishingHandleFromFurnishingName(furnishingName)
                .getItems();

        for (int axis = 0; axis < EngineSetting.AXIS_COUNT; axis++) {

            int span = 1;

            for (int i = 0; i < items.length; i++) {

                ItemShapeStruct shape = items[i].getShape();

                span = Math.max(span, itemRotationBufferSystem.getShapeMax(shape, orientation, axis)
                        - itemRotationBufferSystem.getShapeMin(shape, orientation, axis) + 1);
            }

            footprint[axis] = (span + octant - 1) / octant;
        }
    }

    // Every item every place could be furnished with, clear of every solid sub-voxel and of every other place
    private void verifyCargo(ShipLayoutStruct layout) {

        if (layout.getCargoCount() == 0)
            return;

        SubVoxelGridStruct solidGrid = vehicleManager.buildSolidGrid(layout.getParts());

        claimedCells.clear();

        for (int slotIndex = 0; slotIndex < layout.getCargoCount(); slotIndex++) {

            FurnishingSlotStruct slot = layout.getCargoSlots().get(slotIndex);
            FurnishingHandle furnishing = furnishingManager.getFurnishingHandleFromFurnishingID(
                    slot.getFurnishingID());

            for (int i = 0; i < furnishing.getItems().length; i++)
                if (!fits(solidGrid, slot, furnishing.getItems()[i].getShape())) {
                    layout.reportBuildProblem(EngineSetting.SHIPWRIGHT_PROBLEM_CARGO_FIT + slot.getFurnishingName());
                    claimedCells.clear();
                    return;
                }

            for (int i = 0; i < furnishing.getItems().length; i++)
                claim(slot, furnishing.getItems()[i].getShape());
        }

        claimedCells.clear();
    }

    private boolean fits(SubVoxelGridStruct solidGrid, FurnishingSlotStruct slot, ItemShapeStruct shape) {

        for (int i = 0; i < shape.getCellCount(); i++) {

            int x = resolveCell(slot, shape, i, EngineSetting.AXIS_X);
            int y = resolveCell(slot, shape, i, EngineSetting.AXIS_Y);
            int z = resolveCell(slot, shape, i, EngineSetting.AXIS_Z);

            if (x < 0 || y < 0 || z < 0 || solidGrid.isFilled(x, y, z)
                    || claimedCells.contains(Coordinate3Long.pack(x, y, z)))
                return false;
        }

        return true;
    }

    private void claim(FurnishingSlotStruct slot, ItemShapeStruct shape) {

        for (int i = 0; i < shape.getCellCount(); i++)
            claimedCells.add(Coordinate3Long.pack(
                    resolveCell(slot, shape, i, EngineSetting.AXIS_X),
                    resolveCell(slot, shape, i, EngineSetting.AXIS_Y),
                    resolveCell(slot, shape, i, EngineSetting.AXIS_Z)));
    }

    // One axis of the model sub-voxel a shape's cell fills at a place, turned as the place is turned
    private int resolveCell(FurnishingSlotStruct slot, ItemShapeStruct shape, int cellIndex, int axis) {

        int corner = axis == EngineSetting.AXIS_X ? slot.getCornerX()
                : axis == EngineSetting.AXIS_Y ? slot.getCornerY() : slot.getCornerZ();

        return corner + itemRotationBufferSystem.rotateCell(
                slot.getOrientation(),
                axis,
                shape.getOffsetX() + shape.getCellX(cellIndex),
                shape.getOffsetY() + shape.getCellY(cellIndex),
                shape.getOffsetZ() + shape.getCellZ(cellIndex));
    }

    // Accessible \\

    // The octants along x, up y and across z the footprint last resolved spans
    int getFootprint(int axis) {
        return footprint[axis];
    }
}
