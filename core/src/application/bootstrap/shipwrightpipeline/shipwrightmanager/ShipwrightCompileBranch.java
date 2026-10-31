package application.bootstrap.shipwrightpipeline.shipwrightmanager;

import application.bootstrap.furnishingpipeline.furnishing.FurnishingSlotStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipBlueprintStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipLayoutStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipPiece;
import application.bootstrap.vehiclepipeline.vehicle.VehicleDesignStruct;
import application.bootstrap.vehiclepipeline.vehicle.VehicleHandlingStruct;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartStruct;
import application.bootstrap.vehiclepipeline.vehiclemanager.VehicleManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class ShipwrightCompileBranch extends BranchPackage {

    /*
     * Compiles a blueprint into what it is built of, the one way a ship drawn
     * at the drafting table becomes a ship. Every deck must be drawn, and
     * every piece of a deck above the bottom — a forecastle and a poop are two
     * pieces of one deck — must rest on the deck below. The octant grid is laid out round
     * the sheet with room aft for the rudder and the porch and headroom for
     * deckhouses and bulwarks; the hull, its decks, porch, bulwarks, waist, walls,
     * stairs, ladders, hatches, doors, windows and gun ports are laid into it
     * through ShipwrightHullBranch and ShipwrightFittingBranch, and every piece
     * merged into its part. The bowsprit, helm, capstan, rudder, anchors,
     * lids, ladders, glass and the whole rig are then built as parts of their
     * own, and the cargo is placed and checked against
     * everything solid. Last the ship is weighed, every box by what it is
     * made of, and floated: stone ballast is laid along its bottom until it
     * sinks to its share of the height the sea may rise to before it comes
     * aboard, no deeper than its share of its beam, or none when it is heavy
     * enough already, and the draft it floats
     * at follows from what it then weighs, so where the water sits is worked
     * out, never drawn. A hull too short, too round or too blunt at its
     * waterline to sail, or one that would sink, is a problem in its shape. A
     * compiled layout becomes a VehicleDesignStruct, named for the vehicle it
     * is registered as, with every handling value at its default but the
     * drop its anchors fall to, and the drag along its length left to the
     * shape of its bow.
     */

    // Internal
    private ShipwrightHullBranch shipwrightHullBranch;
    private ShipwrightRigBranch shipwrightRigBranch;
    private ShipwrightFittingBranch shipwrightFittingBranch;
    private VehicleManager vehicleManager;

    // Base \\

    @Override
    protected void get() {
        this.shipwrightHullBranch = get(ShipwrightHullBranch.class);
        this.shipwrightRigBranch = get(ShipwrightRigBranch.class);
        this.shipwrightFittingBranch = get(ShipwrightFittingBranch.class);
        this.vehicleManager = get(VehicleManager.class);
    }

    // Compile \\

    void compile(ShipBlueprintStruct blueprint, ShipLayoutStruct layout) {

        layout.clear(EngineSetting.SHIP_AFT_MARGIN);

        if (!blueprint.hasHull()) {
            layout.reportShapeProblem(EngineSetting.SHIPWRIGHT_PROBLEM_NO_HULL);
            return;
        }

        verifyDecks(blueprint, layout);
        shipwrightHullBranch.laySections(blueprint, layout);
        layout.getGrid().reset(
                EngineSetting.SHIP_AFT_MARGIN + EngineSetting.SHIP_SHEET_LENGTH,
                layout.getSectionLayers() + EngineSetting.SHIP_HEADROOM,
                EngineSetting.SHIP_SHEET_BREADTH);

        shipwrightHullBranch.layShell(layout);
        shipwrightHullBranch.layPorch(blueprint, layout);
        shipwrightHullBranch.layBulwarks(layout);
        shipwrightHullBranch.layWaists(blueprint, layout);
        shipwrightHullBranch.layWalls(blueprint, layout);
        shipwrightHullBranch.layStairs(blueprint, layout);
        shipwrightHullBranch.layLadders(blueprint, layout);
        shipwrightHullBranch.layHatches(blueprint, layout);
        shipwrightFittingBranch.cutOpenings(blueprint, layout);

        mergePieces(layout);

        shipwrightRigBranch.addBowsprit(blueprint, layout);
        shipwrightFittingBranch.addFittings(blueprint, layout);
        shipwrightRigBranch.addRig(blueprint, layout);
        shipwrightFittingBranch.addCargo(blueprint, layout);

        floatHull(blueprint, layout);
        verifyWaterline(layout);
    }

    // Decks \\

    // Every deck drawn, and every piece of one above the bottom resting on the deck below it
    private void verifyDecks(ShipBlueprintStruct blueprint, ShipLayoutStruct layout) {

        for (int deck = 0; deck < blueprint.getDeckCount(); deck++) {

            String deckName = EngineSetting.SHIPWRIGHT_TEXT_DECK + (deck + 1);

            if (!blueprint.isDeckDrawn(deck))
                layout.reportShapeProblem(deckName + EngineSetting.SHIPWRIGHT_PROBLEM_DECK_EMPTY);
            else if (deck > 0 && !restsOn(blueprint, deck))
                layout.reportShapeProblem(deckName + EngineSetting.SHIPWRIGHT_PROBLEM_DECK_FLOATING);
        }
    }

    // True when every run of a deck along the sheet shares an octant with the deck below it
    private boolean restsOn(ShipBlueprintStruct blueprint, int deck) {

        boolean inPiece = false;
        boolean resting = false;

        for (int x = 0; x <= EngineSetting.SHIP_SHEET_LENGTH; x++) {

            boolean here = blueprint.getOutline(deck, x) > 0;

            if (!here && inPiece && !resting)
                return false;

            if (!here || !inPiece)
                resting = false;

            inPiece = here;
            resting |= here && blueprint.getOutline(deck - 1, x) > 0;
        }

        return true;
    }

    // Pieces \\

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

    // Float \\

    // The ship weighed, trimmed with ballast down to its share of the sill, and floated at whatever it then weighs
    private void floatHull(ShipBlueprintStruct blueprint, ShipLayoutStruct layout) {

        ObjectArrayList<VehiclePartStruct> parts = layout.getParts();
        float sill = layout.getSillHeight();
        float[] weight = weighParts(parts);
        float structure = weight[0];
        float target = Math.min(sill * EngineSetting.SHIP_DRAFT_SHARE,
                resolveBeam(layout) * EngineSetting.SHIP_DRAFT_BEAM_SHARE);
        float ballast = Math.max(0f,
                vehicleManager.resolveDisplacement(blueprint.getDisplayName(), parts, target) - structure);
        float mass = structure + ballast;
        float draft = ballast > 0f ? target : vehicleManager.solveDraft(blueprint.getDisplayName(), parts, mass);

        float moment = weight[1] + ballast * resolveBallastHeight(blueprint, ballast);

        layout.setWeight(structure, ballast, moment / Math.max(mass, EngineSetting.DIVISION_EPSILON));

        if (draft == EngineSetting.VEHICLE_DRAFT_SINKS || draft >= sill) {
            layout.reportShapeProblem(EngineSetting.SHIPWRIGHT_PROBLEM_SINKS);
            return;
        }

        if (draft >= layout.getOpeningSill())
            layout.reportShapeProblem(EngineSetting.SHIPWRIGHT_PROBLEM_OPENING_SUBMERGED);

        layout.setDraft(draft);
    }

    // The tonnes every part weighs by what it is made of, then their moment about the keel in tonne-blocks
    private float[] weighParts(ObjectArrayList<VehiclePartStruct> parts) {

        float scale = 1f / EngineSetting.SUB_VOXEL_RESOLUTION;
        float mass = 0f;
        float moment = 0f;

        for (int i = 0; i < parts.size(); i++) {

            VehiclePartStruct part = parts.get(i);
            float density = resolveDensity(part.getTextureName());

            for (int box = 0; box < part.getBoxCount(); box++) {

                float sizeX = (part.getBoxBound(box, EngineSetting.BOX_MAX_X)
                        - part.getBoxBound(box, EngineSetting.BOX_MIN_X)) * scale;
                float sizeY = (part.getBoxBound(box, EngineSetting.BOX_MAX_Y)
                        - part.getBoxBound(box, EngineSetting.BOX_MIN_Y)) * scale;
                float sizeZ = (part.getBoxBound(box, EngineSetting.BOX_MAX_Z)
                        - part.getBoxBound(box, EngineSetting.BOX_MIN_Z)) * scale;
                float boxMass = sizeX * sizeY * sizeZ * density;
                float middle = (part.getBoxBound(box, EngineSetting.BOX_MIN_Y)
                        + part.getBoxBound(box, EngineSetting.BOX_MAX_Y)) * 0.5f * scale;

                mass += boxMass;
                moment += boxMass * middle;
            }
        }

        return new float[] { mass, moment };
    }

    private float resolveDensity(String textureName) {

        for (int i = 0; i < EngineSetting.SHIP_MATERIAL_TEXTURES.length; i++)
            if (EngineSetting.SHIP_MATERIAL_TEXTURES[i].equals(textureName))
                return EngineSetting.SHIP_MATERIAL_DENSITIES[i];

        return EngineSetting.SHIP_MATERIAL_DENSITY_DEFAULT;
    }

    // The hull's greatest breadth anywhere, in blocks
    private float resolveBeam(ShipLayoutStruct layout) {

        int widest = 0;

        for (int x = 0; x < EngineSetting.SHIP_SHEET_LENGTH; x++)
            widest = Math.max(widest, shipwrightHullBranch.findWidestSection(layout, x));

        return widest * 2 * EngineSetting.SUB_BLOCK_SIZE;
    }

    // The height of the middle of the ballast laid evenly over the bottom deck, in model blocks
    private float resolveBallastHeight(ShipBlueprintStruct blueprint, float ballast) {

        float octantArea = EngineSetting.SUB_BLOCK_SIZE * EngineSetting.SUB_BLOCK_SIZE;
        float area = 0f;

        for (int x = 0; x < EngineSetting.SHIP_SHEET_LENGTH; x++)
            area += blueprint.getOutline(0, x) * 2 * octantArea;

        float depth = ballast / EngineSetting.SHIP_BALLAST_DENSITY / Math.max(area, octantArea);

        return EngineSetting.SUB_BLOCK_SIZE + depth * 0.5f;
    }

    // Waterline \\

    // The hull's length, beam and bow at the layer its waterline crosses, each held to what a hull may sail with
    private void verifyWaterline(ShipLayoutStruct layout) {

        if (!layout.isAfloat())
            return;

        int layer = Math.max(0, Math.min(layout.getSectionLayers() - 1,
                (int) Math.ceil(layout.getDraft() / EngineSetting.SUB_BLOCK_SIZE) - 1));
        int stern = EngineSetting.INDEX_NOT_FOUND;
        int stem = EngineSetting.INDEX_NOT_FOUND;
        int fullest = EngineSetting.INDEX_NOT_FOUND;
        int widest = 0;

        for (int x = 0; x < EngineSetting.SHIP_SHEET_LENGTH; x++) {

            int halfBeam = layout.getWaterlineSection(x, layer);

            if (halfBeam == 0)
                continue;

            if (stern == EngineSetting.INDEX_NOT_FOUND)
                stern = x;

            stem = x;

            if (halfBeam >= widest) {
                widest = halfBeam;
                fullest = x;
            }
        }

        if (stern == EngineSetting.INDEX_NOT_FOUND)
            return;

        float length = (stem - stern + 1) * EngineSetting.SUB_BLOCK_SIZE;
        float beam = widest * 2 * EngineSetting.SUB_BLOCK_SIZE;
        float entry = (float) Math.toDegrees(Math.atan2(widest, stem - fullest + 1));

        layout.setWaterline(length, beam, entry);

        if (length < EngineSetting.SHIP_MIN_WATERLINE_LENGTH)
            layout.reportShapeProblem(EngineSetting.SHIPWRIGHT_PROBLEM_TOO_SHORT);
        else if (length < beam * EngineSetting.SHIP_MIN_LENGTH_BEAM_RATIO)
            layout.reportShapeProblem(EngineSetting.SHIPWRIGHT_PROBLEM_TOO_ROUND);
        else if (entry > EngineSetting.SHIP_MAX_ENTRY_DEGREES)
            layout.reportShapeProblem(EngineSetting.SHIPWRIGHT_PROBLEM_TOO_BLUNT);
    }

    // Design \\

    // A compiled layout as the design its vehicle is assembled from; it must have compiled without a problem
    VehicleDesignStruct toDesign(ShipBlueprintStruct blueprint, ShipLayoutStruct layout, String vehicleName) {

        ObjectArrayList<VehiclePartStruct> parts = new ObjectArrayList<>(layout.getParts());
        ObjectArrayList<FurnishingSlotStruct> cargoSlots = new ObjectArrayList<>(layout.getCargoSlots());

        return new VehicleDesignStruct(
                vehicleName,
                blueprint.getDisplayName(),
                blueprint.getCategory(),
                parts,
                layout.getDraft(),
                layout.getCenterOfMassHeight(),
                createHandling(layout),
                cargoSlots);
    }

    private VehicleHandlingStruct createHandling(ShipLayoutStruct layout) {
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
                layout.getAnchorDrop());
    }
}
