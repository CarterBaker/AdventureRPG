package application.bootstrap.shipwrightpipeline.shipwrightmanager;

import application.bootstrap.furnishingpipeline.furnishing.FurnishingSlotStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipBlueprintStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipLayoutStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipPiece;
import application.bootstrap.vehiclepipeline.vehicle.VehicleCategory;
import application.bootstrap.vehiclepipeline.vehicle.VehicleDesignStruct;
import application.bootstrap.vehiclepipeline.vehicle.VehicleHandlingStruct;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartStruct;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class ShipwrightCompileBranch extends BranchPackage {

    /*
     * Compiles a blueprint into what it is built of, the one way a ship drawn
     * at the drafting table becomes a ship. The octant grid is laid out round
     * the hull with room aft for the rudder and the porch and headroom for
     * deckhouses and rails; the hull, its decks, porch, walls, rails, stairs
     * and doorways are laid into it through ShipwrightHullBranch and
     * ShipwrightFittingBranch, and every piece merged into its part. The
     * bowsprit, helm, capstan, rudder and the whole rig are then built as
     * parts of their own, and last the cargo is placed and checked against
     * everything solid. The design draft is the top of the waterline layer,
     * and the centre of mass sits a share of the way up to it. A compiled
     * layout becomes a VehicleDesignStruct, named for the vehicle it is
     * registered as, with every handling value at its default and the drag
     * along its length left to the shape of its bow.
     */

    // Internal
    private ShipwrightHullBranch shipwrightHullBranch;
    private ShipwrightRigBranch shipwrightRigBranch;
    private ShipwrightFittingBranch shipwrightFittingBranch;

    // Base \\

    @Override
    protected void get() {
        this.shipwrightHullBranch = get(ShipwrightHullBranch.class);
        this.shipwrightRigBranch = get(ShipwrightRigBranch.class);
        this.shipwrightFittingBranch = get(ShipwrightFittingBranch.class);
    }

    // Compile \\

    void compile(ShipBlueprintStruct blueprint, ShipLayoutStruct layout) {

        layout.clear(EngineSetting.SHIP_AFT_MARGIN);

        if (!blueprint.hasHull()) {
            layout.reportProblem(EngineSetting.SHIPWRIGHT_PROBLEM_NO_HULL);
            return;
        }

        layout.getGrid().reset(
                EngineSetting.SHIP_AFT_MARGIN + blueprint.getLength(),
                blueprint.getHeight() + EngineSetting.SHIP_HEADROOM,
                blueprint.getBreadth());

        shipwrightHullBranch.layShell(blueprint, layout);
        shipwrightHullBranch.layDecks(blueprint, layout);
        shipwrightHullBranch.layPorch(blueprint, layout);
        shipwrightHullBranch.collectLevels(layout);
        shipwrightHullBranch.layWalls(blueprint, layout);
        shipwrightHullBranch.layRails(blueprint, layout);
        shipwrightHullBranch.layStairs(blueprint, layout);
        shipwrightFittingBranch.cutDoorways(blueprint, layout);

        mergePieces(layout);

        shipwrightRigBranch.addBowsprit(blueprint, layout);
        shipwrightFittingBranch.addFittings(blueprint, layout);
        shipwrightRigBranch.addRig(blueprint, layout);

        resolveHull(blueprint, layout);
        shipwrightFittingBranch.addCargo(blueprint, layout);
    }

    // Every piece laid in the grid merged into boxes, one part per piece that has any
    private void mergePieces(ShipLayoutStruct layout) {

        for (int i = 0; i < ShipPiece.VALUES.length; i++) {

            ShipPiece piece = ShipPiece.VALUES[i];
            VehiclePartStruct part = new VehiclePartStruct(
                    piece.getPartName(),
                    piece.getRole(),
                    piece.getTextureName(),
                    EngineSetting.SHIP_LINK_NONE,
                    new Vector3(),
                    EngineSetting.AXIS_Y,
                    0f);

            if (layout.getGrid().mergeInto(piece, part))
                layout.addPart(part);
        }
    }

    // The draft at the top of the waterline layer, the centre of mass a share of the way up to it
    private void resolveHull(ShipBlueprintStruct blueprint, ShipLayoutStruct layout) {

        if (!isAfloat(blueprint)) {
            layout.reportProblem(EngineSetting.SHIPWRIGHT_PROBLEM_WATERLINE);
            return;
        }

        float draft = (blueprint.getWaterline() + 1) * EngineSetting.SUB_BLOCK_SIZE;

        layout.setHull(draft, draft * EngineSetting.SHIP_CENTER_OF_MASS_SHARE);
    }

    // True when some of the hull lies at or below the waterline
    private boolean isAfloat(ShipBlueprintStruct blueprint) {

        for (int y = 0; y <= blueprint.getWaterline(); y++)
            for (int z = 0; z < blueprint.getBreadth(); z++)
                for (int x = 0; x < blueprint.getLength(); x++)
                    if (blueprint.isVolume(x, y, z))
                        return true;

        return false;
    }

    // Design \\

    // A compiled layout as the design its vehicle is assembled from; it must have compiled without a problem
    VehicleDesignStruct toDesign(ShipLayoutStruct layout, String vehicleName, String displayName) {

        ObjectArrayList<VehiclePartStruct> parts = new ObjectArrayList<>(layout.getParts());
        ObjectArrayList<FurnishingSlotStruct> cargoSlots = new ObjectArrayList<>(layout.getCargoSlots());

        return new VehicleDesignStruct(
                vehicleName,
                displayName,
                VehicleCategory.SHIP,
                parts,
                layout.getDraft(),
                layout.getCenterOfMassHeight(),
                createHandling(),
                cargoSlots);
    }

    private VehicleHandlingStruct createHandling() {
        return new VehicleHandlingStruct(
                EngineSetting.DEFAULT_VEHICLE_SWAY_DRAG,
                EngineSetting.DEFAULT_VEHICLE_HEAVE_DRAG,
                EngineSetting.DEFAULT_VEHICLE_FLOOD_RATE,
                EngineSetting.DEFAULT_VEHICLE_PUMP_RATE,
                EngineSetting.DEFAULT_VEHICLE_SAIL_FORCE,
                (float) Math.toRadians(EngineSetting.DEFAULT_VEHICLE_BRACE_LIMIT_DEGREES),
                (float) Math.toRadians(EngineSetting.DEFAULT_VEHICLE_BRACE_RATE_DEGREES),
                1f / EngineSetting.DEFAULT_VEHICLE_HOIST_SECONDS,
                (float) Math.toRadians(EngineSetting.DEFAULT_VEHICLE_RUDDER_LIMIT_DEGREES),
                (float) Math.toRadians(EngineSetting.DEFAULT_VEHICLE_RUDDER_RATE_DEGREES),
                EngineSetting.DEFAULT_VEHICLE_RUDDER_FORCE,
                EngineSetting.DEFAULT_VEHICLE_ANCHOR_HOLD,
                EngineSetting.DEFAULT_VEHICLE_ANCHOR_DROP);
    }
}
