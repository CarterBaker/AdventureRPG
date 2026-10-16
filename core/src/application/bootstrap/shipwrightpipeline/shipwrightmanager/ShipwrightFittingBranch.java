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
     * lowers the sill the sea comes aboard over. Doors and furniture are cargo
     * drawn from furnishing tables: each is turned the way it faces, sized to
     * the octants its largest item takes up and centred in them, standing on
     * its deck's floor. Once every part is built, every item each
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

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.footprint = new int[EngineSetting.AXIS_COUNT];
        this.claimedCells = new LongOpenHashSet();
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

    // Every door and window cut through what it faces, before the hull is merged
    void cutOpenings(ShipBlueprintStruct blueprint, ShipLayoutStruct layout) {

        IntArrayList doors = blueprint.getDoors();
        IntArrayList windows = blueprint.getWindows();

        for (int i = 0; i < doors.size(); i++) {

            int cell = doors.getInt(i);

            resolveFootprint(EngineSetting.SHIP_DOOR_FURNISHING, ShipCellUtility.unpackSpin(cell));
            cutOpening(layout, cell, 1, footprint[EngineSetting.AXIS_Y], footprint[EngineSetting.AXIS_X],
                    footprint[EngineSetting.AXIS_Z]);
        }

        for (int i = 0; i < windows.size(); i++)
            cutOpening(layout, windows.getInt(i), EngineSetting.SHIP_WINDOW_SILL, EngineSetting.SHIP_WINDOW_HEIGHT,
                    1, 1);
    }

    // An opening a footprint across, rising from a height over its deck's floor, cut through the first wall or
    // planking along the way it faces in every column of its footprint
    private void cutOpening(ShipLayoutStruct layout, int cell, int sill, int height, int sizeX, int sizeZ) {

        ShipOctantGridStruct grid = layout.getGrid();
        int floor = ShipCellUtility.unpackLevel(cell) * EngineSetting.SHIP_DECK_HEIGHT;
        int spin = ShipCellUtility.unpackSpin(cell);
        int startX = layout.getOriginX() + ShipCellUtility.unpackX(cell);
        int startZ = ShipCellUtility.unpackZ(cell);
        boolean cut = false;

        for (int y = floor + sill; y < floor + sill + height; y++)
            for (int z = startZ; z < startZ + sizeZ; z++)
                for (int x = startX; x < startX + sizeX; x++) {

                    if (shipwrightHullBranch.isFloor(grid, x, y, z)) {
                        layout.reportBuildProblem(EngineSetting.SHIPWRIGHT_PROBLEM_OPENING_HEIGHT);
                        return;
                    }

                    if (cutThrough(layout, x, y, z, ShipCellUtility.stepX(spin), ShipCellUtility.stepZ(spin)))
                        cut = true;
                }

        if (!cut)
            layout.reportBuildProblem(EngineSetting.SHIPWRIGHT_PROBLEM_OPENING_WALL);
    }

    // Walks out from an octant along a step, clearing the first run of wall or planking it meets; true when it cut
    private boolean cutThrough(ShipLayoutStruct layout, int x, int y, int z, int stepX, int stepZ) {

        ShipOctantGridStruct grid = layout.getGrid();
        boolean cut = false;

        for (int reach = 0; reach <= EngineSetting.SHIP_OPENING_REACH; reach++) {

            int atX = x + stepX * reach;
            int atZ = z + stepZ * reach;
            ShipPiece piece = grid.get(atX, y, atZ);

            if (piece == null || !piece.isCuttable()) {

                if (cut || piece != null)
                    return cut;

                continue;
            }

            if (piece != ShipPiece.WALLS)
                layout.lowerOpeningSill(y * EngineSetting.SUB_BLOCK_SIZE);

            grid.clear(atX, y, atZ);
            cut = true;
        }

        return cut;
    }

    // Fittings \\

    void addFittings(ShipBlueprintStruct blueprint, ShipLayoutStruct layout) {

        if (blueprint.hasHelm())
            addHelm(layout, blueprint.getHelm());

        if (blueprint.hasCapstan())
            addCapstan(layout, blueprint.getCapstan());

        addRudder(layout);
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

    // Every door and piece of furniture as a furnished place, then every place checked to fit
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

            addPlace(blueprint, layout, piece.getFurnishingName(), piece.getCell());
        }

        verifyCargo(layout);
    }

    // A furnished place standing on its deck's floor, centred in the octants its largest item takes up
    private void addPlace(ShipBlueprintStruct blueprint, ShipLayoutStruct layout, String furnishingName, int cell) {

        int spin = ShipCellUtility.unpackSpin(cell);
        int orientation = EngineSetting.DEFAULT_BLOCK_ORIENTATION + spin;
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
                spin,
                1f,
                EngineSetting.LOOT_TABLE_NAME_NONE,
                furnishingManager,
                lootTableManager);

        layout.addCargo(slot, minX, minY, minZ, maxX, maxY, maxZ);
    }

    // The corner that centres a turned shape between two bounds on one axis
    private int resolveCorner(ItemShapeStruct shape, int orientation, int axis, int min, int max) {

        int low = itemRotationBufferSystem.getShapeMin(shape, orientation, axis);
        int high = itemRotationBufferSystem.getShapeMax(shape, orientation, axis);

        return min + (max - min - (high - low + 1)) / 2 - low;
    }

    // The octants a furnishing's largest item takes up on each axis once turned, written to the footprint
    void resolveFootprint(String furnishingName, int spin) {

        int orientation = EngineSetting.DEFAULT_BLOCK_ORIENTATION + spin;
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

    boolean isKnownFurnishing(String furnishingName) {
        return furnishingManager.hasFurnishing(furnishingName);
    }

    // The octants along x, up y and across z the footprint last resolved spans
    int getFootprint(int axis) {
        return footprint[axis];
    }
}
